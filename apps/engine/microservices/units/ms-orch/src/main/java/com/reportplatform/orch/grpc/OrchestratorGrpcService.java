package com.reportplatform.orch.grpc;

import com.reportplatform.orch.model.FailedJobEntity;
import com.reportplatform.orch.model.WorkflowHistoryEntity;
import com.reportplatform.orch.service.WorkflowService;
import com.reportplatform.proto.orchestrator.v1.CancelWorkflowRequest;
import com.reportplatform.proto.orchestrator.v1.CancelWorkflowResponse;
import com.reportplatform.proto.orchestrator.v1.GetWorkflowStatusRequest;
import com.reportplatform.proto.orchestrator.v1.ListFailedJobsRequest;
import com.reportplatform.proto.orchestrator.v1.ListFailedJobsResponse;
import com.reportplatform.proto.orchestrator.v1.OrchestratorServiceGrpc;
import com.reportplatform.proto.orchestrator.v1.ReprocessFailedJobRequest;
import com.reportplatform.proto.orchestrator.v1.RetryWorkflowRequest;
import com.reportplatform.proto.orchestrator.v1.StartFileWorkflowRequest;
import com.reportplatform.proto.orchestrator.v1.StartFileWorkflowResponse;
import com.reportplatform.proto.orchestrator.v1.WorkflowStatusResponse;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * gRPC service implementation for the Orchestrator.
 * <p>
 * Exposes all workflow management RPCs: start, status, retry, cancel,
 * list failed jobs, and reprocess failed jobs.
 * </p>
 */
@GrpcService
public class OrchestratorGrpcService extends OrchestratorServiceGrpc.OrchestratorServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(OrchestratorGrpcService.class);

    private final WorkflowService workflowService;

    public OrchestratorGrpcService(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @Override
    public void startFileWorkflow(StartFileWorkflowRequest request,
                                  StreamObserver<StartFileWorkflowResponse> responseObserver) {
        try {
            log.info("gRPC StartFileWorkflow: fileId={}, fileType={}, orgId={}",
                    request.getFileId(), request.getFileType(), request.getOrgId());

            String workflowId = workflowService.startWorkflow(
                    request.getFileId(), request.getFileType(), request.getOrgId());

            StartFileWorkflowResponse response = StartFileWorkflowResponse.newBuilder()
                    .setWorkflowId(workflowId)
                    .setStatus("STARTED")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("StartFileWorkflow failed: {}", e.getMessage(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to start workflow: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void getWorkflowStatus(GetWorkflowStatusRequest request,
                                  StreamObserver<WorkflowStatusResponse> responseObserver) {
        try {
            WorkflowHistoryEntity history = workflowService.getWorkflowStatus(request.getWorkflowId());
            if (history == null) {
                responseObserver.onError(Status.NOT_FOUND
                        .withDescription("Workflow not found: " + request.getWorkflowId())
                        .asRuntimeException());
                return;
            }

            WorkflowStatusResponse.Builder builder = WorkflowStatusResponse.newBuilder()
                    .setWorkflowId(history.getWorkflowId())
                    .setFileId(history.getFileId())
                    .setStatus(history.getStatus())
                    .setStartedAt(history.getStartedAt().toString());

            if (history.getCompletedAt() != null) {
                builder.setCompletedAt(history.getCompletedAt().toString());
            }
            if (history.getStepsJson() != null) {
                builder.setStepsJson(history.getStepsJson());
            }

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("GetWorkflowStatus failed: {}", e.getMessage(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to get workflow status: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void retryWorkflow(RetryWorkflowRequest request,
                              StreamObserver<StartFileWorkflowResponse> responseObserver) {
        try {
            log.info("gRPC RetryWorkflow: workflowId={}", request.getWorkflowId());

            String newWorkflowId = workflowService.retryWorkflow(request.getWorkflowId());

            StartFileWorkflowResponse response = StartFileWorkflowResponse.newBuilder()
                    .setWorkflowId(newWorkflowId)
                    .setStatus("RETRYING")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        } catch (IllegalStateException e) {
            responseObserver.onError(Status.FAILED_PRECONDITION
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        } catch (Exception e) {
            log.error("RetryWorkflow failed: {}", e.getMessage(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to retry workflow: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void cancelWorkflow(CancelWorkflowRequest request,
                               StreamObserver<CancelWorkflowResponse> responseObserver) {
        try {
            log.info("gRPC CancelWorkflow: workflowId={}", request.getWorkflowId());

            boolean cancelled = workflowService.cancelWorkflow(request.getWorkflowId());

            CancelWorkflowResponse response = CancelWorkflowResponse.newBuilder()
                    .setSuccess(cancelled)
                    .setMessage(cancelled ? "Workflow cancelled" : "Unable to cancel workflow")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("CancelWorkflow failed: {}", e.getMessage(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to cancel workflow: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void listFailedJobs(ListFailedJobsRequest request,
                               StreamObserver<ListFailedJobsResponse> responseObserver) {
        try {
            List<FailedJobEntity> failedJobs = workflowService.listFailedJobs(request.getOrgId());

            ListFailedJobsResponse.Builder responseBuilder = ListFailedJobsResponse.newBuilder();
            for (FailedJobEntity job : failedJobs) {
                ListFailedJobsResponse.FailedJob protoJob = ListFailedJobsResponse.FailedJob.newBuilder()
                        .setJobId(job.getId().toString())
                        .setFileId(job.getFileId())
                        .setWorkflowId(job.getWorkflowId())
                        .setErrorType(job.getErrorType())
                        .setErrorDetail(job.getErrorDetail() != null ? job.getErrorDetail() : "")
                        .setFailedAt(job.getFailedAt().toString())
                        .setRetryCount(job.getRetryCount())
                        .build();
                responseBuilder.addJobs(protoJob);
            }
            responseBuilder.setTotalCount(failedJobs.size());

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("ListFailedJobs failed: {}", e.getMessage(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to list failed jobs: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void reprocessFailedJob(ReprocessFailedJobRequest request,
                                   StreamObserver<StartFileWorkflowResponse> responseObserver) {
        try {
            log.info("gRPC ReprocessFailedJob: jobId={}", request.getJobId());

            String workflowId = workflowService.reprocessFailedJob(request.getJobId());

            StartFileWorkflowResponse response = StartFileWorkflowResponse.newBuilder()
                    .setWorkflowId(workflowId)
                    .setStatus("REPROCESSING")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        } catch (Exception e) {
            log.error("ReprocessFailedJob failed: {}", e.getMessage(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to reprocess failed job: " + e.getMessage())
                    .asRuntimeException());
        }
    }
}
