package com.reportplatform.qry.controller;

import com.reportplatform.qry.model.SnowProjectBudgetEntity;
import com.reportplatform.qry.model.SnowProjectEntity;
import com.reportplatform.qry.model.SnowProjectTaskEntity;
import com.reportplatform.qry.model.dto.SnowProjectBudgetDto;
import com.reportplatform.qry.model.dto.SnowProjectDto;
import com.reportplatform.qry.model.dto.SnowProjectTaskDto;
import com.reportplatform.qry.repository.SnowProjectBudgetRepository;
import com.reportplatform.qry.repository.SnowProjectRepository;
import com.reportplatform.qry.repository.SnowProjectTaskRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Query endpoints for ServiceNow project data stored in snow_projects.
 * Data is written by engine-integrations (ProjectFetchService) via ms-sink-tbl.
 */
@RestController
@RequestMapping("/api/v1/data/snow/projects")
public class SnowProjectController {

    private final SnowProjectRepository projectRepository;
    private final SnowProjectTaskRepository taskRepository;
    private final SnowProjectBudgetRepository budgetRepository;

    public SnowProjectController(SnowProjectRepository projectRepository,
                                  SnowProjectTaskRepository taskRepository,
                                  SnowProjectBudgetRepository budgetRepository) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.budgetRepository = budgetRepository;
    }

    /**
     * List projects with optional filters.
     * GET /api/v1/data/snow/projects?ragStatus=RED&status=active&managerEmail=...&connectionId=...&page=0&size=20
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public Page<SnowProjectDto> listProjects(
            @RequestHeader("X-Org-Id") String orgId,
            @RequestParam(required = false) String ragStatus,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String managerEmail,
            @RequestParam(required = false) UUID connectionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return projectRepository
                .findFiltered(UUID.fromString(orgId), ragStatus, status, managerEmail, connectionId, pageable)
                .map(p -> toDto(p, null, null));
    }

    /**
     * Project detail – returns full project with tasks and budget lines.
     * GET /api/v1/data/snow/projects/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<SnowProjectDto> getProject(
            @RequestHeader("X-Org-Id") String orgId,
            @PathVariable UUID id) {

        return projectRepository.findByIdAndOrgId(id, UUID.fromString(orgId))
                .map(project -> {
                    List<SnowProjectTaskEntity> tasks =
                            taskRepository.findByProjectIdOrderByDueDateAsc(project.getId());
                    List<SnowProjectBudgetEntity> budgets =
                            budgetRepository.findByProjectIdOrderByFiscalYearAsc(project.getId());
                    return ResponseEntity.ok(toDto(project, tasks, budgets));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ---- Mapping ----

    private SnowProjectDto toDto(SnowProjectEntity p,
                                  List<SnowProjectTaskEntity> tasks,
                                  List<SnowProjectBudgetEntity> budgets) {
        return new SnowProjectDto(
                p.getId(),
                p.getResolverConnectionId(),
                p.getSysId(),
                p.getNumber(),
                p.getShortDescription(),
                p.getStatus(),
                p.getPhase(),
                p.getManagerName(),
                p.getManagerEmail(),
                p.getDepartment(),
                p.getPlannedStartDate(),
                p.getPlannedEndDate(),
                p.getActualStartDate(),
                p.getProjectedEndDate(),
                p.getPercentComplete(),
                p.getTotalBudget(),
                p.getActualCost(),
                p.getProjectedCost(),
                p.getBudgetUtilizationPct(),
                p.getScheduleVarianceDays(),
                p.getMilestoneCompletionRate(),
                p.getCostForecastAccuracy(),
                p.getRagStatus(),
                p.getSyncedAt(),
                tasks == null ? null : tasks.stream().map(this::toTaskDto).toList(),
                budgets == null ? null : budgets.stream().map(this::toBudgetDto).toList()
        );
    }

    private SnowProjectTaskDto toTaskDto(SnowProjectTaskEntity t) {
        return new SnowProjectTaskDto(
                t.getId(), t.getSysId(), t.getNumber(), t.getShortDescription(),
                t.getParentSysId(), t.getState(), t.isMilestone(),
                t.getAssignedToName(), t.getDueDate(), t.getCompletedAt());
    }

    private SnowProjectBudgetDto toBudgetDto(SnowProjectBudgetEntity b) {
        return new SnowProjectBudgetDto(
                b.getId(), b.getSysId(), b.getCategory(), b.getFiscalYear(),
                b.getPlannedAmount(), b.getActualAmount());
    }
}
