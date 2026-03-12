package com.reportplatform.admin.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reportplatform.admin.model.dto.FailedJobDTO;
import com.reportplatform.admin.model.dto.PaginatedResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Service
public class FailedJobService {

    private static final Logger logger = LoggerFactory.getLogger(FailedJobService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${dapr.host:localhost}")
    private String daprHost;

    @Value("${dapr.http-port:3500}")
    private int daprPort;

    public FailedJobService(ObjectMapper objectMapper) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
    }

    public PaginatedResponse<FailedJobDTO> getFailedJobs(int page, int pageSize, String orgId) {
        // Call MS-ORCH via Dapr to get failed jobs
        // For now, return empty response - actual implementation would query MS-ORCH
        PaginatedResponse<FailedJobDTO> response = new PaginatedResponse<>();
        response.setPage(page);
        response.setPageSize(pageSize);
        response.setTotalItems(0);
        response.setTotalPages(0);
        response.setData(java.util.Collections.emptyList());
        return response;
    }

    public void reprocessFailedJob(UUID jobId) {
        // Trigger MS-ORCH to reprocess the failed job via Dapr
        String daprAppId = "ms-orch";
        String url = String.format("http://%s:%d/api/v1/failed-jobs/%s/reprocess", daprHost, daprPort, jobId);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            logger.info("Triggered reprocessing for failed job: {}", jobId);
        } catch (Exception e) {
            logger.error("Failed to trigger reprocessing for job: {}", jobId, e);
            throw new RuntimeException("Failed to reprocess job: " + jobId, e);
        }
    }

    public FailedJobDTO getFailedJob(UUID jobId) {
        // Call MS-ORCH to get job details
        FailedJobDTO dto = new FailedJobDTO();
        dto.setId(jobId);
        return dto;
    }
}
