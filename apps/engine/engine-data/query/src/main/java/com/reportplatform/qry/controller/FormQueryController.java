package com.reportplatform.qry.controller;

import com.reportplatform.qry.service.FormQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for querying form data.
 * Provides form response data for dashboards alongside file data.
 */
@RestController
@RequestMapping("/api/query/forms")
public class FormQueryController {

    private static final Logger log = LoggerFactory.getLogger(FormQueryController.class);

    private final FormQueryService formQueryService;

    public FormQueryController(FormQueryService formQueryService) {
        this.formQueryService = formQueryService;
    }

    /**
     * Returns aggregated form response data for a specific form.
     * Used by dashboards to show form submission statistics.
     *
     * @param formId the form identifier
     * @param orgId  organization ID from header (for RLS)
     * @param userId user ID from header
     * @return aggregated form data including submission counts, status breakdown
     */
    @GetMapping("/{form_id}/data")
    public ResponseEntity<Map<String, Object>> getFormData(
            @PathVariable("form_id") UUID formId,
            @RequestHeader(value = "X-Org-Id") String orgId,
            @RequestHeader(value = "X-User-Id") String userId) {

        log.info("GET form data: formId={}, orgId={}", formId, orgId);

        Map<String, Object> formData = formQueryService.getFormData(formId, orgId);
        return ResponseEntity.ok(formData);
    }

    /**
     * Returns all responses for a specific form and organization.
     * Used to display form submission details in the UI.
     *
     * @param formId   the form identifier
     * @param orgId    organization ID from header (for RLS)
     * @param userId   user ID from header
     * @param periodId optional period filter
     * @param status   optional status filter (DRAFT, SUBMITTED, etc.)
     * @param page     page number for pagination
     * @param size     page size
     * @return list of form responses
     */
    @GetMapping("/{form_id}/responses")
    public ResponseEntity<Map<String, Object>> getFormResponses(
            @PathVariable("form_id") UUID formId,
            @RequestHeader(value = "X-Org-Id") String orgId,
            @RequestHeader(value = "X-User-Id") String userId,
            @RequestParam(value = "period_id", required = false) String periodId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {

        log.info("GET form responses: formId={}, orgId={}, periodId={}, status={}",
                formId, orgId, periodId, status);

        Map<String, Object> responses = formQueryService.getFormResponses(
                formId, orgId, periodId, status, page, size);
        return ResponseEntity.ok(responses);
    }

    /**
     * Returns a specific org's response for a form.
     * Used to view a single organization's form submission.
     *
     * @param formId the form identifier
     * @param orgId  the organization identifier
     * @param userId user ID from header
     * @return form response data for the specific organization
     */
    @GetMapping("/{form_id}/responses/{resp_org_id}")
    public ResponseEntity<Map<String, Object>> getOrgFormResponse(
            @PathVariable("form_id") UUID formId,
            @PathVariable("resp_org_id") String orgId,
            @RequestHeader(value = "X-User-Id") String userId) {

        log.info("GET org form response: formId={}, orgId={}", formId, orgId);

        return formQueryService.getOrgFormResponse(formId, orgId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Returns aggregated form data for all forms in a period.
     * Used by matrix dashboards to show period completion status.
     *
     * @param periodId the period identifier
     * @param orgId    organization ID from header (for RLS)
     * @param userId   user ID from header
     * @return list of form completion status
     */
    @GetMapping("/period/{period_id}/completions")
    public ResponseEntity<List<Map<String, Object>>> getFormCompletions(
            @PathVariable("period_id") String periodId,
            @RequestHeader(value = "X-Org-Id") String orgId,
            @RequestHeader(value = "X-User-Id") String userId) {

        log.info("GET form completions: periodId={}, orgId={}", periodId, orgId);

        List<Map<String, Object>> completions = formQueryService.getFormCompletions(periodId, orgId);
        return ResponseEntity.ok(completions);
    }
}
