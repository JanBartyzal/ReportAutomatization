package com.reportplatform.proto.orchestrator.v1;

import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;
import io.grpc.stub.StreamObserver;

/**
 * gRPC service stub for OrchestratorService.
 * This is a manual stub since protobuf generation is not configured.
 */
public class OrchestratorServiceGrpc {
    
    public static final String SERVICE_NAME = 
            "com.reportplatform.proto.orchestrator.v1.OrchestratorService";
    
    private OrchestratorServiceGrpc() {}
    
    public static class OrchestratorServiceImplBase implements BindableService {
        
        public void startFileWorkflow(
                StartFileWorkflowRequest request,
                StreamObserver<StartFileWorkflowResponse> responseObserver) {
            responseObserver.onCompleted();
        }
        
        public void getWorkflowStatus(
                GetWorkflowStatusRequest request,
                StreamObserver<WorkflowStatusResponse> responseObserver) {
            responseObserver.onCompleted();
        }
        
        public void retryWorkflow(
                RetryWorkflowRequest request,
                StreamObserver<StartFileWorkflowResponse> responseObserver) {
            responseObserver.onCompleted();
        }
        
        public void cancelWorkflow(
                CancelWorkflowRequest request,
                StreamObserver<CancelWorkflowResponse> responseObserver) {
            responseObserver.onCompleted();
        }
        
        public void listFailedJobs(
                ListFailedJobsRequest request,
                StreamObserver<ListFailedJobsResponse> responseObserver) {
            responseObserver.onCompleted();
        }
        
        public void reprocessFailedJob(
                ReprocessFailedJobRequest request,
                StreamObserver<StartFileWorkflowResponse> responseObserver) {
            responseObserver.onCompleted();
        }
        
        @Override
        public ServerServiceDefinition bindService() {
            return ServerServiceDefinition.builder(SERVICE_NAME).build();
        }
    }
}
