package com.reportplatform.template.tmpl.controller;

import com.reportplatform.template.tmpl.service.PromotionService;
import com.reportplatform.template.tmpl.service.PromotionService.RoutingInfo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * Internal REST endpoint for routing info queries from MS-ORCH via Dapr.
 * This is NOT exposed through the API Gateway - only accessible via Dapr service invocation.
 */
@RestController
public class RoutingInfoController {

    private final PromotionService promotionService;

    public RoutingInfoController(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    /**
     * Called by the orchestrator via Dapr to check if a mapping template
     * has a promoted dedicated table.
     */
    @PostMapping("/getRoutingInfo")
    public ResponseEntity<RoutingInfo> getRoutingInfo(@RequestBody Map<String, String> request) {
        String mappingTemplateId = request.get("mappingTemplateId");
        if (mappingTemplateId == null || mappingTemplateId.isBlank()) {
            return ResponseEntity.ok(new RoutingInfo(false, null, false, null));
        }

        RoutingInfo info = promotionService.getRoutingInfo(UUID.fromString(mappingTemplateId));
        return ResponseEntity.ok(info);
    }
}
