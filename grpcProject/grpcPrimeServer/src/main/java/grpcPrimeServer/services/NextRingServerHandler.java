package grpcPrimeServer.services;

import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.netty.util.internal.logging.InternalLogger;
import io.grpc.netty.shaded.io.netty.util.internal.logging.InternalLoggerFactory;
import io.grpc.stub.StreamObserver;
import primeServerCommunicationService.PrimeMessageState;
import primeServerCommunicationService.RingMessageResponse;
import primeServerService.PrimeRequest;
import ringManagerPrimeService.*;
import shared.General;
import shared.General.ServerInfo;

public class NextRingServerHandler {

    public static NextRingServerHandler createHandler(ServerInfo serverInfo, ManagedChannel channel) {
        return new NextRingServerHandler(channel, serverInfo);
    }

    private NextRingServerHandler(ManagedChannel channel, ServerInfo serverInfo) {
        this.channel = channel;
        this.serverDetails = serverInfo;
    }

    private final ManagedChannel channel;
    private final ServerInfo serverDetails;
    private final InternalLogger logger = InternalLoggerFactory.getInstance(NextRingServerHandler.class);

    public StreamObserver<RegisterServerRequest> registerServer(StreamObserver<PrimeRequest> requestObserver) {
        RegisterServerRequest request = RegisterServerRequest
                .newBuilder()
                .setPrimeServer(serverDetails)
                .build();
        // Register the server with RingManager using the asynchronous gRPC stub

        return RingManagerPrimeServiceGrpc.newStub(channel)
                .registerServer(new StreamObserver<RegisterServerResponse>() {
                    @Override
                    public void onNext(RegisterServerResponse registerServerResponse) {
                        ServerInfo nextServerInfo = registerServerResponse.getNextServer();
                        forwardRingMessage();
                        logger.info("PrimeServer received server info: " + nextServerInfo);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        logger.error("Error registering server: {}", throwable.getMessage());
                        System.exit(1);
                    }

                    @Override
                    public void onCompleted() {
                        logger.info("Server registration completed awaiting requests");
                    }
                });

    }

    private void forwardRingMessage(
            long number,
            ServerInfo originServer,
            PrimeMessageState state,
            String requestId,
            ServerInfo nextServer,
            StreamObserver<RingMessageResponse> ringMessageResponseStreamObserver
    ) {

        logger.info("Forwarding ring request");
        logger.info("Received next ring response");
        RingMessageBuilder.ringMessageRequest(number, requestId, state, originServer, nextServer, ringMessageResponseStreamObserver);
    }

}
