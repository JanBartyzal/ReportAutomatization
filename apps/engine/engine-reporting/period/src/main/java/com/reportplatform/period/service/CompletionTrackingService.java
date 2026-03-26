package com.reportplatform.period.service;

import com.reportplatform.period.dto.PeriodStatusResponse;
import com.reportplatform.period.model.PeriodEntity;
import com.reportplatform.period.repository.PeriodOrgAssignmentRepository;
import com.reportplatform.period.repository.PeriodRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CompletionTrackingService {

    private static final Logger log = LoggerFactory.getLogger(CompletionTrackingService.class);

    private final PeriodRepository periodRepository;
    private final PeriodOrgAssignmentRepository assignmentRepository;
    private Object daprClient;

    public CompletionTrackingService(PeriodRepository periodRepository,
            PeriodOrgAssignmentRepository assignmentRepository) {
        this.periodRepository = periodRepository;
        this.assignmentRepository = assignmentRepository;
    }

    @PostConstruct
    void init() {
        try {
            this.daprClient = new io.dapr.client.DaprClientBuilder().build();
            log.info("Dapr client initialized for CompletionTrackingService");
        } catch (Exception e) {
            log.warn("Dapr client unavailable, completion tracking will use local data only: {}", e.getMessage());
            this.daprClient = null;
        }
    }

    @PreDestroy
    void destroy() throws Exception {
        if (daprClient instanceof AutoCloseable ac) {
            ac.close();
        }
    }

    public PeriodStatusResponse getCompletionStatus(UUID periodId) {
        PeriodEntity period = periodRepository.findById(periodId)
                .orElseThrow(() -> new com.reportplatform.period.exception.PeriodNotFoundException(periodId));

        var assignments = assignmentRepository.findByPeriodId(periodId);
        int totalOrgs = assignments.size();

        // Query MS-LIFECYCLE for matrix data via Dapr service invocation
        List<Map<String, Object>> matrixData = fetchMatrixFromLifecycle(periodId);

        int totalReports = 0;
        int approvedReports = 0;
        List<PeriodStatusResponse.OrgStatusEntry> orgStatuses = new java.util.ArrayList<>();

        for (var assignment : assignments) {
            String orgId = assignment.getOrgId();
            int orgReports = 0;
            String orgStatus = "PENDING";

            for (var entry : matrixData) {
                if (orgId.equals(entry.get("orgId"))) {
                    Object countObj = entry.get("count");
                    int count = countObj instanceof Number n ? n.intValue() : 0;
                    orgReports += count;
                    String entryStatus = String.valueOf(entry.get("status"));
                    if ("APPROVED".equals(entryStatus)) {
                        approvedReports += count;
                        orgStatus = "APPROVED";
                    } else if (!"APPROVED".equals(orgStatus)) {
                        orgStatus = entryStatus;
                    }
                }
            }
            totalReports += orgReports;
            orgStatuses.add(new PeriodStatusResponse.OrgStatusEntry(orgId, orgStatus, orgReports));
        }

        int completionPct = totalOrgs > 0 ? (approvedReports * 100 / totalOrgs) : 0;

        return new PeriodStatusResponse(
                periodId, period.getName(), totalOrgs,
                totalReports, approvedReports, completionPct, orgStatuses);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fetchMatrixFromLifecycle(UUID periodId) {
        if (daprClient == null) {
            log.debug("Dapr client not available, returning empty matrix for period {}", periodId);
            return List.of();
        }
        try {
            var client = (io.dapr.client.DaprClient) daprClient;
            var result = client.invokeMethod(
                    "ms-lifecycle",
                    "api/reports/matrix?periodId=" + periodId,
                    null,
                    io.dapr.client.domain.HttpExtension.GET,
                    List.class).block();
            return result != null ? (List<Map<String, Object>>) result : List.of();
        } catch (Exception e) {
            log.warn("Failed to fetch matrix from ms-lifecycle for period {}: {}",
                    periodId, e.getMessage());
            return List.of();
        }
    }
}
