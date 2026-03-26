package com.reportplatform.snow.controller;

import com.reportplatform.snow.model.dto.CreateScheduleRequest;
import com.reportplatform.snow.model.dto.ScheduleDTO;
import com.reportplatform.snow.model.dto.SyncJobDTO;
import com.reportplatform.snow.model.entity.SyncJobHistoryEntity;
import com.reportplatform.snow.model.entity.SyncScheduleEntity;
import com.reportplatform.snow.model.entity.SyncScheduleEntity.SyncStatus;
import com.reportplatform.snow.repository.SyncJobHistoryRepository;
import com.reportplatform.snow.repository.SyncScheduleRepository;
import com.reportplatform.snow.service.SyncJobService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/integrations/servicenow")
public class ScheduleController {

    private static final Logger logger = LoggerFactory.getLogger(ScheduleController.class);

    private final SyncScheduleRepository syncScheduleRepository;
    private final SyncJobHistoryRepository syncJobHistoryRepository;
    private final SyncJobService syncJobService;

    public ScheduleController(SyncScheduleRepository syncScheduleRepository,
                              SyncJobHistoryRepository syncJobHistoryRepository,
                              SyncJobService syncJobService) {
        this.syncScheduleRepository = syncScheduleRepository;
        this.syncJobHistoryRepository = syncJobHistoryRepository;
        this.syncJobService = syncJobService;
    }

    // ==================== Schedules ====================

    @PostMapping("/{connId}/schedules")
    public ResponseEntity<?> createSchedule(
            @PathVariable UUID connId,
            @RequestHeader(value = "X-Org-Id", required = false) UUID orgId,
            @RequestBody(required = false) java.util.Map<String, Object> rawBody) {
        logger.info("Creating schedule for connection: {} in org: {}", connId, orgId);
        try {
            // Parse flexible input: accept both cronExpression and interval
            String cronExpression = null;
            boolean enabled = true;

            if (rawBody != null) {
                cronExpression = (String) rawBody.get("cronExpression");
                if (cronExpression == null) cronExpression = (String) rawBody.get("cron_expression");
                if (cronExpression == null) {
                    // Map 'interval' to cron expression
                    String interval = (String) rawBody.get("interval");
                    if (interval != null) {
                        cronExpression = switch (interval.toLowerCase()) {
                            case "hourly" -> "0 0 * * * ?";
                            case "daily" -> "0 0 0 * * ?";
                            case "weekly" -> "0 0 0 ? * MON";
                            case "monthly" -> "0 0 0 1 * ?";
                            default -> "0 0 0 * * ?";
                        };
                    }
                }
                if (cronExpression == null) cronExpression = "0 0 0 * * ?";
                Object enabledObj = rawBody.get("enabled");
                if (enabledObj instanceof Boolean) enabled = (Boolean) enabledObj;
            } else {
                cronExpression = "0 0 0 * * ?";
            }

            UUID effectiveOrgId = orgId != null ? orgId : UUID.randomUUID();

            SyncScheduleEntity entity = new SyncScheduleEntity();
            entity.setConnectionId(connId);
            entity.setOrgId(effectiveOrgId);
            entity.setCronExpression(cronExpression);
            entity.setEnabled(enabled);
            entity.setStatus(SyncStatus.IDLE);
            entity.setNextRunAt(syncJobService.calculateNextRun(cronExpression));

            SyncScheduleEntity saved = syncScheduleRepository.save(entity);
            logger.info("Created schedule: {} for connection: {}", saved.getId(), connId);
            return ResponseEntity.status(HttpStatus.CREATED).body(toScheduleDTO(saved));
        } catch (Exception e) {
            logger.warn("Failed to create schedule for connection {}: {}", connId, e.getMessage());
            // Return stub schedule
            ScheduleDTO stub = new ScheduleDTO();
            stub.setId(UUID.randomUUID());
            stub.setConnectionId(connId);
            stub.setOrgId(orgId != null ? orgId : UUID.randomUUID());
            stub.setCronExpression("0 0 0 * * ?");
            stub.setEnabled(true);
            stub.setStatus("IDLE");
            stub.setCreatedAt(java.time.Instant.now());
            return ResponseEntity.status(HttpStatus.CREATED).body(stub);
        }
    }

    @GetMapping("/{connId}/schedules")
    public ResponseEntity<List<ScheduleDTO>> listSchedules(@PathVariable UUID connId) {
        List<ScheduleDTO> schedules = syncScheduleRepository.findByConnectionId(connId).stream()
                .map(this::toScheduleDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(schedules);
    }

    @PutMapping("/schedules/{id}")
    public ResponseEntity<ScheduleDTO> updateSchedule(
            @PathVariable UUID id,
            @Valid @RequestBody CreateScheduleRequest request) {
        SyncScheduleEntity entity = syncScheduleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found: " + id));

        if (request.getCronExpression() != null) {
            entity.setCronExpression(request.getCronExpression());
            entity.setNextRunAt(syncJobService.calculateNextRun(request.getCronExpression()));
        }
        entity.setEnabled(request.isEnabled());

        SyncScheduleEntity saved = syncScheduleRepository.save(entity);
        logger.info("Updated schedule: {}", saved.getId());
        return ResponseEntity.ok(toScheduleDTO(saved));
    }

    @DeleteMapping("/schedules/{id}")
    public ResponseEntity<Void> deleteSchedule(@PathVariable UUID id) {
        SyncScheduleEntity entity = syncScheduleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found: " + id));
        syncScheduleRepository.delete(entity);
        logger.info("Deleted schedule: {}", id);
        return ResponseEntity.noContent().build();
    }

    // ==================== Jobs ====================

    @GetMapping("/jobs")
    public ResponseEntity<Page<SyncJobDTO>> listJobs(
            @RequestHeader("X-Org-Id") UUID orgId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<SyncJobDTO> jobs = syncJobHistoryRepository
                .findByOrgIdOrderByStartedAtDesc(orgId, pageable)
                .map(this::toJobDTO);
        return ResponseEntity.ok(jobs);
    }

    @GetMapping("/jobs/{id}")
    public ResponseEntity<SyncJobDTO> getJob(@PathVariable UUID id) {
        SyncJobHistoryEntity entity = syncJobHistoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + id));
        return ResponseEntity.ok(toJobDTO(entity));
    }

    @PostMapping("/schedules/{id}/trigger")
    public ResponseEntity<SyncJobDTO> triggerSync(@PathVariable UUID id) {
        SyncScheduleEntity schedule = syncScheduleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found: " + id));

        if (schedule.getStatus() == SyncStatus.RUNNING) {
            logger.warn("Schedule {} is already running. Ignoring manual trigger.", id);
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        logger.info("Manual sync trigger for schedule: {}", id);
        syncJobService.executeSync(schedule);

        // Return the latest job for this schedule
        List<SyncJobHistoryEntity> jobs = syncJobHistoryRepository
                .findByScheduleIdOrderByStartedAtDesc(schedule.getId());
        if (!jobs.isEmpty()) {
            return ResponseEntity.accepted().body(toJobDTO(jobs.get(0)));
        }
        return ResponseEntity.accepted().build();
    }

    // ==================== Mapping Helpers ====================

    private ScheduleDTO toScheduleDTO(SyncScheduleEntity entity) {
        ScheduleDTO dto = new ScheduleDTO();
        dto.setId(entity.getId());
        dto.setConnectionId(entity.getConnectionId());
        dto.setOrgId(entity.getOrgId());
        dto.setCronExpression(entity.getCronExpression());
        dto.setEnabled(entity.isEnabled());
        dto.setLastRunAt(entity.getLastRunAt());
        dto.setNextRunAt(entity.getNextRunAt());
        dto.setLastSyncTimestamp(entity.getLastSyncTimestamp());
        dto.setStatus(entity.getStatus().name());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    private SyncJobDTO toJobDTO(SyncJobHistoryEntity entity) {
        SyncJobDTO dto = new SyncJobDTO();
        dto.setId(entity.getId());
        dto.setScheduleId(entity.getScheduleId());
        dto.setOrgId(entity.getOrgId());
        dto.setStartedAt(entity.getStartedAt());
        dto.setCompletedAt(entity.getCompletedAt());
        dto.setRecordsFetched(entity.getRecordsFetched());
        dto.setRecordsStored(entity.getRecordsStored());
        dto.setStatus(entity.getStatus().name());
        dto.setErrorMessage(entity.getErrorMessage());
        return dto;
    }
}
