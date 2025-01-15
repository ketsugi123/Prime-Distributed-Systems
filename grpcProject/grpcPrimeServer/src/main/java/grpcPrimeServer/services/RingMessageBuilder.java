package grpcPrimeServer.services;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.netty.shaded.io.netty.util.internal.logging.InternalLogger;
import io.grpc.netty.shaded.io.netty.util.internal.logging.InternalLoggerFactory;
import io.grpc.stub.StreamObserver;
import primeServerCommunicationService.PrimeMessageState;
import primeServerCommunicationService.PrimeServerCommunicationServiceGrpc;
import primeServerCommunicationService.RingMessageRequest;
import primeServerCommunicationService.RingMessageResponse;
import shared.General;

import javax.annotation.Nullable;

public class RingMessageBuilder {
    public static StreamObserver<RingMessageRequest> buildRingMessage(
            General.ServerInfo nextServerInfo,
            StreamObserver<RingMessageResponse> ringMessageResponseStreamObserver
    ) {
        ManagedChannel primeRingMessageChannel = ManagedChannelBuilder.forAddress(nextServerInfo.getIp(), nextServerInfo.getPort())
                .usePlaintext().build();
        PrimeServerCommunicationServiceGrpc.PrimeServerCommunicationServiceStub
                primeRingMessageStub = PrimeServerCommunicationServiceGrpc.newStub(primeRingMessageChannel);
        
        InternalLogger logger = InternalLoggerFactory.getInstance(RingMessageBuilder.class);
        
        
        return primeRingMessageStub.ringMessage(new StreamObserver<RingMessageResponse>() {
            @Override
            public void onNext(RingMessageResponse value) {
                logger.info("Received Ring Message from Next Server");
                boolean isFirstRingMessage = ringMessageResponseStreamObserver == null;
                if (!isFirstRingMessage) {
                    ringMessageResponseStreamObserver.onNext(RingMessageResponse.getDefaultInstance());
                    ringMessageResponseStreamObserver.onCompleted();
                }
                primeRingMessageChannel.shutdown();
            }

            @Override
            public void onError(Throwable t) {
                logger.error("Error in receiving ringMessage from next server " + t.getMessage());
                primeRingMessageChannel.shutdown();
            }

            @Override
            public void onCompleted() {
                logger.info("Ring Message processing completed.");
                primeRingMessageChannel.shutdown();
            }
        });
    }

    public static void ringMessageRequest(
            long requestNumber,
            String requestId,
            PrimeMessageState state,
            General.ServerInfo originServer,
            General.ServerInfo nextServerInfo,
            @Nullable StreamObserver<RingMessageResponse> ringMessageResponseStreamObserver
            ) {
        RingMessageRequest ringRequest = RingMessageRequest.newBuilder()
                .setNumber(requestNumber)
                .setState(state)
                .setRequestId(requestId)
                .setOrigin(originServer)
                .build();

        StreamObserver<RingMessageRequest> requestStreamObserver = RingMessageBuilder.buildRingMessage(nextServerInfo, ringMessageResponseStreamObserver);
        requestStreamObserver.onNext(ringRequest);
        requestStreamObserver.onCompleted();
    }
}
