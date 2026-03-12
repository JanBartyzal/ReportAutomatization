package com.reportplatform.qry.grpc;

import com.reportplatform.proto.generator.v1.*;
import com.reportplatform.qry.service.ReportDataAggregationService;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * gRPC service implementation for ReportDataService.
 * Provides aggregated report data for PPTX generation.
 * Handles incoming gRPC requests from MS-ORCH and MS-DASH via Dapr sidecar.
 */
@GrpcService
public class ReportDataGrpcService extends ReportDataServiceGrpc.ReportDataServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(ReportDataGrpcService.class);

    private final ReportDataAggregationService aggregationService;

    public ReportDataGrpcService(ReportDataAggregationService aggregationService) {
        this.aggregationService = aggregationService;
    }

    @Override
    public void getReportData(GetReportDataRequest request, StreamObserver<GetReportDataResponse> responseObserver) {
        logger.info("Received GetReportData request: reportId={}, templateId={}",
                request.getReportId(), request.getTemplateId());

        try {
            String reportId = request.getReportId();
            String orgId = request.getContext().getOrgId();
            String templateId = request.getTemplateId();

            ReportDataAggregationService.AggregatedReportData data = aggregationService.getAggregatedData(reportId,
                    orgId, templateId);

            // Build proto response
            GetReportDataResponse.Builder responseBuilder = GetReportDataResponse.newBuilder()
                    .setReportId(reportId)
                    .addAllTextPlaceholders(data.textPlaceholders())
                    .addAllAvailableFields(data.availableFields())
                    .setCachedAt(data.cachedAt());

            // Add table data
            for (ReportDataAggregationService.TableData table : data.tables()) {
                GeneratorTableData.Builder tableBuilder = GeneratorTableData.newBuilder()
                        .setPlaceholderKey(table.placeholderKey())
                        .addAllHeaders(table.headers())
                        .setAggregation(TableAggregationType.valueOf(table.aggregationType().name()));

                for (List<String> row : table.rows()) {
                    tableBuilder.addRows(GeneratorTableRow.newBuilder()
                            .addAllCells(row)
                            .build());
                }
                responseBuilder.addTables(tableBuilder.build());
            }

            // Add chart data
            for (ReportDataAggregationService.ChartData chart : data.charts()) {
                GeneratorChartData.Builder chartBuilder = GeneratorChartData.newBuilder()
                        .setPlaceholderKey(chart.placeholderKey())
                        .setChartType(chart.chartType())
                        .addAllLabels(chart.labels())
                        .setAggregation(ChartAggregationType.valueOf(chart.aggregationType().name()));

                for (ReportDataAggregationService.ChartSeries series : chart.series()) {
                    chartBuilder.addSeries(ChartSeries.newBuilder()
                            .setName(series.name())
                            .addAllValues(series.values())
                            .build());
                }
                responseBuilder.addCharts(chartBuilder.build());
            }

            GetReportDataResponse response = responseBuilder.build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();

            logger.info("GetReportData completed: reportId={}, textFields={}, tables={}, charts={}",
                    reportId, data.textPlaceholders().size(), data.tables().size(), data.charts().size());

        } catch (Exception e) {
            logger.error("GetReportData failed: {}", e.getMessage(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void getBatchReportData(GetBatchReportDataRequest request,
            StreamObserver<GetBatchReportDataResponse> responseObserver) {
        logger.info("Received GetBatchReportData request: reportIds={}", request.getReportIdsCount());

        try {
            List<GetReportDataResponse> results = new ArrayList<>();
            int successful = 0;
            int failed = 0;

            for (String reportId : request.getReportIdsList()) {
                try {
                    GetReportDataRequest singleRequest = GetReportDataRequest.newBuilder()
                            .setReportId(reportId)
                            .setContext(request.getContext())
                            .setTemplateId(request.getTemplateId())
                            .build();

                    // Reuse single request handling
                    ReportDataAggregationService.AggregatedReportData data = aggregationService.getAggregatedData(
                            reportId,
                            request.getContext().getOrgId(),
                            request.getTemplateId());

                    GetReportDataResponse.Builder responseBuilder = GetReportDataResponse.newBuilder()
                            .setReportId(reportId)
                            .addAllTextPlaceholders(data.textPlaceholders())
                            .addAllAvailableFields(data.availableFields())
                            .setCachedAt(data.cachedAt());

                    for (ReportDataAggregationService.TableData table : data.tables()) {
                        GeneratorTableData.Builder tableBuilder = GeneratorTableData.newBuilder()
                                .setPlaceholderKey(table.placeholderKey())
                                .addAllHeaders(table.headers())
                                .setAggregation(TableAggregationType.valueOf(table.aggregationType().name()));

                        for (List<String> row : table.rows()) {
                            tableBuilder.addRows(GeneratorTableRow.newBuilder()
                                    .addAllCells(row)
                                    .build());
                        }
                        responseBuilder.addTables(tableBuilder.build());
                    }

                    for (ReportDataAggregationService.ChartData chart : data.charts()) {
                        GeneratorChartData.Builder chartBuilder = GeneratorChartData.newBuilder()
                                .setPlaceholderKey(chart.placeholderKey())
                                .setChartType(chart.chartType())
                                .addAllLabels(chart.labels())
                                .setAggregation(ChartAggregationType.valueOf(chart.aggregationType().name()));

                        for (ReportDataAggregationService.ChartSeries series : chart.series()) {
                            chartBuilder.addSeries(ChartSeries.newBuilder()
                                    .setName(series.name())
                                    .addAllValues(series.values())
                                    .build());
                        }
                        responseBuilder.addCharts(chartBuilder.build());
                    }

                    results.add(responseBuilder.build());
                    successful++;

                } catch (Exception e) {
                    logger.warn("Failed to get data for report {}: {}", reportId, e.getMessage());
                    failed++;
                }
            }

            GetBatchReportDataResponse response = GetBatchReportDataResponse.newBuilder()
                    .addAllResults(results)
                    .setSuccessful(successful)
                    .setFailed(failed)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            logger.info("GetBatchReportData completed: successful={}, failed={}", successful, failed);

        } catch (Exception e) {
            logger.error("GetBatchReportData failed: {}", e.getMessage(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }
}
