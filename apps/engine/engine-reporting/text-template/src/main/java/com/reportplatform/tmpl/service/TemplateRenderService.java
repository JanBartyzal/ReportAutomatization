package com.reportplatform.tmpl.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reportplatform.tmpl.dto.RenderRequest;
import com.reportplatform.tmpl.dto.RenderResponse;
import com.reportplatform.tmpl.entity.TextTemplateEntity;
import com.reportplatform.tmpl.model.BindingEntry;
import com.reportplatform.tmpl.model.BindingType;
import com.reportplatform.tmpl.repository.TextTemplateRepository;
import io.dapr.client.DaprClient;
import io.dapr.client.domain.HttpExtension;
import io.dapr.utils.TypeRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Renders a {@link TextTemplateEntity} into a downloadable file.
 * <p>
 * Render pipeline:
 * <ol>
 *   <li>Load template + parse dataBindings JSON.</li>
 *   <li>For each binding, resolve {@code {{input.X}}} references in params using render-time input.</li>
 *   <li>Execute Named Query via engine-data:query (Dapr gRPC).</li>
 *   <li>Build content model (resolved text blocks + table data + chart specs).</li>
 *   <li>Call appropriate generator: processor-generators:pptx or :xls (Dapr gRPC).</li>
 *   <li>Return Blob Storage download URL.</li>
 * </ol>
 * <p>
 * This service is DATA-SOURCE AGNOSTIC – any Named Query can be a binding target,
 * regardless of underlying table (platform files, ITSM, projects, forms, etc.).
 */
@Service
public class TemplateRenderService {

    private static final Logger log = LoggerFactory.getLogger(TemplateRenderService.class);
    private static final Pattern INPUT_REF_PATTERN = Pattern.compile("\\{\\{input\\.([^}]+)}}");

    private final TextTemplateRepository templateRepo;
    private final DaprClient daprClient;
    private final ObjectMapper objectMapper;

    @Value("${dapr.remote.engine-data-app-id:engine-data}")
    private String engineDataAppId;

    @Value("${dapr.remote.processor-generators-app-id:processor-generators}")
    private String processorGeneratorsAppId;

    public TemplateRenderService(TextTemplateRepository templateRepo,
                                  DaprClient daprClient,
                                  ObjectMapper objectMapper) {
        this.templateRepo = templateRepo;
        this.daprClient = daprClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Render a text template to the requested output format.
     *
     * @param orgId      caller's organisation UUID
     * @param templateId target template
     * @param req        render parameters and desired output format
     * @return download URL for the generated file
     */
    @Transactional(readOnly = true)
    public RenderResponse render(UUID orgId, UUID templateId, RenderRequest req) {
        // 1. Load template
        TextTemplateEntity template = templateRepo.findByIdAndOrgAccess(templateId, orgId)
                .orElseThrow(() -> new NoSuchElementException("Text template not found: " + templateId));

        if (!template.isActive()) {
            throw new IllegalStateException("Template is not active: " + templateId);
        }

        // Validate requested output format is supported by template
        String requestedFormat = req.outputFormat().toUpperCase();
        if (!template.getOutputFormats().toUpperCase().contains(requestedFormat)) {
            throw new IllegalArgumentException(
                    "Output format '" + requestedFormat + "' is not supported by this template. " +
                    "Supported: " + template.getOutputFormats());
        }

        // 2. Parse bindings
        List<BindingEntry> bindings = parseBindings(template.getDataBindings());

        // 3. Resolve each binding → fetch data
        Map<String, Object> resolvedData = new LinkedHashMap<>();
        for (BindingEntry binding : bindings) {
            Object resolved = resolveBinding(binding, req.params(), orgId);
            resolvedData.put(binding.placeholder(), resolved);
        }

        // 4. Build render request payload
        Map<String, Object> generatorRequest = Map.of(
                "templateId", templateId.toString(),
                "templateName", template.getName(),
                "templateContent", template.getContent(),
                "outputFormat", requestedFormat,
                "resolvedBindings", resolvedData,
                "inputParams", req.params()
        );

        // 5. Call appropriate generator
        String generatorPath = "EXCEL".equals(requestedFormat)
                ? "/api/v1/generate/excel-from-template"
                : "/api/v1/generate/pptx-from-template";

        @SuppressWarnings("unchecked")
        Map<String, Object> generatorResponse = daprClient
                .invokeMethod(processorGeneratorsAppId, generatorPath,
                        generatorRequest, HttpExtension.POST,
                        new TypeRef<Map<String, Object>>() {})
                .block();

        String downloadUrl = generatorResponse != null
                ? (String) generatorResponse.getOrDefault("downloadUrl", "")
                : "";
        String blobName = generatorResponse != null
                ? (String) generatorResponse.getOrDefault("blobName", "")
                : "";

        log.info("Template '{}' rendered as {} → {}", template.getName(), requestedFormat, blobName);

        return new RenderResponse(templateId, template.getName(),
                requestedFormat, downloadUrl, blobName, Instant.now());
    }

    // ---- Private helpers ----

    /**
     * Executes a single binding: resolves input references, calls Named Query,
     * and returns structured result (rows list, scalar, or chart spec).
     */
    private Object resolveBinding(BindingEntry binding, Map<String, String> inputParams, UUID orgId) {
        // Resolve {{input.X}} in binding params
        Map<String, String> resolvedParams = resolveInputRefs(binding.params(), inputParams);

        try {
            // Call engine-data:query Named Query execute endpoint
            Map<String, Object> executeRequest = Map.of(
                    "params", resolvedParams,
                    "limit", 5000
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> result = daprClient.invokeMethod(
                            engineDataAppId,
                            "/api/v1/data/named-queries/" + binding.queryId() + "/execute",
                            executeRequest,
                            HttpExtension.POST,
                            new TypeRef<Map<String, Object>>() {})
                    .block();

            if (result == null) return null;

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rows = (List<Map<String, Object>>) result.get("rows");

            return switch (binding.type()) {
                case SCALAR -> extractScalar(rows);
                case CHART -> Map.of(
                        "chartType", binding.chartType() != null ? binding.chartType() : "BAR",
                        "label", binding.label() != null ? binding.label() : binding.placeholder(),
                        "rows", rows != null ? rows : List.of()
                );
                case TABLE -> Map.of(
                        "label", binding.label() != null ? binding.label() : binding.placeholder(),
                        "rows", rows != null ? rows : List.of()
                );
            };
        } catch (Exception e) {
            log.warn("Failed to resolve binding '{}': {}", binding.placeholder(), e.getMessage());
            return null;
        }
    }

    /** Extracts the first column of the first row as a scalar value. */
    private Object extractScalar(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) return null;
        Map<String, Object> firstRow = rows.get(0);
        if (firstRow.isEmpty()) return null;
        return firstRow.values().iterator().next();
    }

    /**
     * Replaces {@code {{input.paramName}}} references in a params map
     * with actual values from the render-time input.
     */
    private Map<String, String> resolveInputRefs(Map<String, String> bindingParams,
                                                  Map<String, String> inputParams) {
        if (bindingParams == null || bindingParams.isEmpty()) return Map.of();
        Map<String, String> resolved = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : bindingParams.entrySet()) {
            String value = entry.getValue();
            if (value != null) {
                Matcher m = INPUT_REF_PATTERN.matcher(value);
                StringBuffer sb = new StringBuffer();
                while (m.find()) {
                    String inputKey = m.group(1);
                    String replacement = inputParams.getOrDefault(inputKey, "");
                    m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
                }
                m.appendTail(sb);
                value = sb.toString();
            }
            resolved.put(entry.getKey(), value);
        }
        return resolved;
    }

    private List<BindingEntry> parseBindings(String dataBindingsJson) {
        try {
            Map<String, Object> root = objectMapper.readValue(dataBindingsJson,
                    new TypeReference<Map<String, Object>>() {});
            Object bindingsRaw = root.get("bindings");
            if (!(bindingsRaw instanceof List<?> list)) return List.of();
            return objectMapper.convertValue(list,
                    new TypeReference<List<BindingEntry>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse dataBindings JSON: {}", e.getMessage());
            return List.of();
        }
    }
}
