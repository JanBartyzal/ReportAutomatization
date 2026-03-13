package com.reportplatform.period.service;

import com.reportplatform.period.dto.PeriodStatusResponse;
import com.reportplatform.period.model.PeriodEntity;
import com.reportplatform.period.repository.PeriodOrgAssignmentRepository;
import com.reportplatform.period.repository.PeriodRepository;
import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.domain.HttpExtension;
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
    private DaprClient daprClient;

    public CompletionTrackingService(PeriodRepository periodRepository,
            PeriodOrgAssignmentRepository assignmentRepository) {
        this.periodRepository = periodRepository;
        this.assignmentRepository = assignmentRepository;
    }

    @PostConstruct
    void init() {
        this.daprClient = new DaprClientBuilder().build();
    }

    @PreDestroy
    void destroy() throws Exception {
        if (daprClient != null) {
            daprClient.close();
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
            String orgStatus = "EMPTY";

            for (var entry : matrixData) {
                if (orgId.equals(entry.get("orgId"))) {
                    orgReports += ((Number) entry.get("count")).intValue();
                    if ("APPROVED".equals(entry.get("status"))) {
                        approvedReports += ((Number) entry.get("count")).intValue();
                        orgStatus = "APPROVED";
                    } else if (!"APPROVED".equals(orgStatus)) {
                        orgStatus = (String) entry.get("status");
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
        try {
            var result = daprClient.invokeMethod(
                    "ms-lifecycle",
                    "api/reports/matrix?periodId=" + periodId,
                    null,
                    HttpExtension.GET,
                    List.class).block();
            return result != null ? (List<Map<String, Object>>) result : List.of();
        } catch (Exception e) {
            log.warn("Failed to fetch matrix from ms-lifecycle for period {}: {}",
                    periodId, e.getMessage());
            return List.of();
        }
    }
}
