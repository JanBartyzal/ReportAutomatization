package com.reportplatform.template.tmpl.grpc;

import com.reportplatform.proto.template.v1.*;
import com.reportplatform.template.tmpl.service.MappingRuleEngine.MappingActionData;
import com.reportplatform.template.tmpl.service.TemplateMappingService;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * gRPC service implementation for TemplateMappingService.
 * Handles incoming gRPC requests from MS-ORCH via Dapr sidecar.
 */
@GrpcService
public class TemplateMappingGrpcService extends TemplateMappingServiceGrpc.TemplateMappingServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(TemplateMappingGrpcService.class);

    private final TemplateMappingService templateMappingService;

    public TemplateMappingGrpcService(TemplateMappingService templateMappingService) {
        this.templateMappingService = templateMappingService;
    }

    @Override
    public void applyMapping(ApplyMappingRequest request, StreamObserver<ApplyMappingResponse> responseObserver) {
        logger.info("Received ApplyMapping request: templateId={}, headers={}",
                request.getTemplateId(), request.getSourceHeadersCount());

        try {
            UUID templateId = UUID.fromString(request.getTemplateId());
            String orgId = request.getContext().getOrgId();
            List<String> sourceHeaders = new ArrayList<>(request.getSourceHeadersList());

            TemplateMappingService.ApplyMappingResult result =
                    templateMappingService.applyMapping(templateId, orgId, sourceHeaders, null);

            // Build proto response
            List<MappingAction> appliedMappings = new ArrayList<>();
            for (MappingActionData action : result.actions()) {
                appliedMappings.add(MappingAction.newBuilder()
                        .setSourceColumn(action.sourceColumn())
                        .setTargetColumn(action.targetColumn())
                        .setRule(action.ruleType())
                        .setConfidence(action.confidence())
                        .build());
            }

            // Pass through the rows unchanged (header renaming only)
            ApplyMappingResponse response = ApplyMappingResponse.newBuilder()
                    .addAllMappedHeaders(result.mappedHeaders())
                    .addAllMappedRows(request.getRowsList())
                    .addAllAppliedMappings(appliedMappings)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            logger.info("ApplyMapping completed: {} headers mapped", result.mappedHeaders().size());

        } catch (Exception e) {
            logger.error("ApplyMapping failed: {}", e.getMessage(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void suggestMapping(SuggestMappingRequest request,
                               StreamObserver<SuggestMappingResponse> responseObserver) {
        logger.info("Received SuggestMapping request: orgId={}, headers={}",
                request.getOrgId(), request.getSourceHeadersCount());

        try {
            String orgId = request.getOrgId();
            List<String> sourceHeaders = new ArrayList<>(request.getSourceHeadersList());

            List<MappingActionData> suggestions =
                    templateMappingService.suggestMapping(orgId, sourceHeaders);

            List<MappingAction> suggestionProtos = new ArrayList<>();
            for (MappingActionData action : suggestions) {
                suggestionProtos.add(MappingAction.newBuilder()
                        .setSourceColumn(action.sourceColumn())
                        .setTargetColumn(action.targetColumn())
                        .setRule(action.ruleType())
                        .setConfidence(action.confidence())
                        .build());
            }

            SuggestMappingResponse response = SuggestMappingResponse.newBuilder()
                    .addAllSuggestions(suggestionProtos)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            logger.info("SuggestMapping completed: {} suggestions", suggestions.size());

        } catch (Exception e) {
            logger.error("SuggestMapping failed: {}", e.getMessage(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void mapExcelToForm(MapExcelToFormRequest request,
                               StreamObserver<MapExcelToFormResponse> responseObserver) {
        logger.info("Received MapExcelToForm request: formId={}, headers={}",
                request.getFormId(), request.getExcelHeadersCount());

        try {
            String orgId = request.getContext().getOrgId();
            List<String> excelHeaders = new ArrayList<>(request.getExcelHeadersList());

            List<MappingActionData> suggestions =
                    templateMappingService.mapExcelToForm(orgId, excelHeaders);

            // Build form field mappings from suggestions
            List<FormFieldMapping> mappings = new ArrayList<>();
            List<String> unmappedExcelCols = new ArrayList<>();

            for (int i = 0; i < excelHeaders.size(); i++) {
                String header = excelHeaders.get(i);
                MappingActionData match = suggestions.stream()
                        .filter(s -> s.sourceColumn().equalsIgnoreCase(header))
                        .findFirst()
                        .orElse(null);

                if (match != null && !"UNMAPPED".equals(match.ruleType())) {
                    mappings.add(FormFieldMapping.newBuilder()
                            .setExcelColumn(header)
                            .setFormFieldId(match.targetColumn())
                            .setFormFieldName(match.targetColumn())
                            .setConfidence(match.confidence())
                            .build());
                } else {
                    unmappedExcelCols.add(header);
                }
            }

            MapExcelToFormResponse response = MapExcelToFormResponse.newBuilder()
                    .addAllMappings(mappings)
                    .addAllUnmappedExcelColumns(unmappedExcelCols)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            logger.info("MapExcelToForm completed: {} mapped, {} unmapped",
                    mappings.size(), unmappedExcelCols.size());

        } catch (Exception e) {
            logger.error("MapExcelToForm failed: {}", e.getMessage(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }
}
