package com.reportplatform.proto.integration.v1;

import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;
import io.grpc.stub.StreamObserver;

/**
 * gRPC service stub for SmartPersistenceService.
 * This is a manual stub since protobuf generation is not configured.
 */
public class SmartPersistenceServiceGrpc {
    
    public static final String SERVICE_NAME = 
            "com.reportplatform.proto.integration.v1.SmartPersistenceService";
    
    private SmartPersistenceServiceGrpc() {}
    
    public static class SmartPersistenceServiceImplBase implements BindableService {
        
        public void getPromotionCandidates(
                GetCandidatesRequest request,
                StreamObserver<GetCandidatesResponse> responseObserver) {
            responseObserver.onCompleted();
        }
        
        public void approvePromotion(
                ApprovePromotionRequest request,
                StreamObserver<ApprovePromotionResponse> responseObserver) {
            responseObserver.onCompleted();
        }
        
        public void getRoutingInfo(
                GetRoutingInfoRequest request,
                StreamObserver<GetRoutingInfoResponse> responseObserver) {
            responseObserver.onCompleted();
        }
        
        public void migrateData(
                MigrateDataRequest request,
                StreamObserver<MigrateDataResponse> responseObserver) {
            responseObserver.onCompleted();
        }
        
        @Override
        public ServerServiceDefinition bindService() {
            return ServerServiceDefinition.builder(SERVICE_NAME).build();
        }
    }
}
