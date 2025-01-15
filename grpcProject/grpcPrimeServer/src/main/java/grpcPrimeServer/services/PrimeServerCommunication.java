package grpcPrimeServer.services;

import com.github.dockerjava.api.DockerClient;
import grpcPrimeServer.model.ServerDetails;
import grpcPrimeServer.utils.ResponseUtils;
import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.netty.util.internal.logging.InternalLogger;
import io.grpc.netty.shaded.io.netty.util.internal.logging.Log4J2LoggerFactory;
import io.grpc.stub.StreamObserver;
import primeServerCommunicationService.PrimeMessageState;
import primeServerCommunicationService.PrimeServerCommunicationServiceGrpc.PrimeServerCommunicationServiceImplBase;
import primeServerCommunicationService.RingMessageRequest;
import primeServerCommunicationService.RingMessageResponse;
import redis.clients.jedis.Jedis;
import ringManagerPrimeService.NextRingServerRequest;
import ringManagerPrimeService.NextRingServerResponse;
import ringManagerPrimeService.RingManagerPrimeServiceGrpc;
import shared.General.ServerInfo;

import static grpcPrimeServer.ClientRequestMem.sendResponseToClient;
import static grpcPrimeServer.utils.DockerUtils.launchPrimeContainer;
import static grpcPrimeServer.utils.ResponseUtils.stateToBoolean;
import static primeServerCommunicationService.PrimeMessageState.PROCESSING;

public class PrimeServerCommunication extends PrimeServerCommunicationServiceImplBase {

    private final Jedis redisClient;
    private final DockerClient dockerClient;
    private final RingManagerPrimeServiceGrpc.RingManagerPrimeServiceStub ringManagerStub;
    private final ServerDetails serverDetails;
    private final ServerDetails redisServerDetails;
    private static final InternalLogger logger = Log4J2LoggerFactory.getInstance(PrimeServerCommunication.class);
    
    
    public PrimeServerCommunication(
            Jedis redisClient,
            DockerClient dockerClient,
            ManagedChannel ringManagerChannel,
            ServerDetails serverDetails,
            ServerDetails redisServerDetails) {
        this.redisClient = redisClient;
        this.dockerClient = dockerClient;
        this.ringManagerStub = RingManagerPrimeServiceGrpc.newStub(ringManagerChannel);
        this.serverDetails = serverDetails;
        this.redisServerDetails = redisServerDetails;
    }


    @Override
    public StreamObserver<RingMessageRequest> ringMessage(StreamObserver<RingMessageResponse> ringMessageResponseStreamObserver) {
        return new StreamObserver<RingMessageRequest>() {
            @Override
            public void onNext(RingMessageRequest request) {
                logger.info("Received ring request");

                long number = request.getNumber();
                PrimeMessageState requestState = request.getState();
                String requestId = request.getRequestId();
                ServerInfo originServer = request.getOrigin();

                // If request has traversed the whole ring
                if (ringLoopCompleted(request)) {
                    ringMessageResponseStreamObserver.onNext(RingMessageResponse.getDefaultInstance());
                    ringMessageResponseStreamObserver.onCompleted();

                    // If no prime server had the response for the request,
                    // the prime calculation container is launched, else,
                    // the received value is sent to the client
                    boolean result;
                    if (requestState == PROCESSING) {
                        result = launchPrimeContainer(dockerClient, redisServerDetails, number);
                    } else {
                        result = stateToBoolean(requestState);
                    }

                    //Send response to client and store it in the redis database
                    sendResponseToClient(requestId, result);
                    redisClient.set(number + "", String.valueOf(result));
                } else {
                    PrimeMessageState cachedResult = getCachedPrimeState(number, request.getState());
                    if (cachedResult == PROCESSING && requestState != PROCESSING) {
                        redisClient.set(number + "", ResponseUtils.stateToString(requestState));
                        cachedResult = requestState;
                    }

                    // Forward the request to the next server in the ring
                    forwardRingMessage(
                            number,
                            originServer,
                            cachedResult,
                            requestId,
                            ringMessageResponseStreamObserver
                    );
                }
            }

            @Override
            public void onError(Throwable t) {
                logger.error(t.getMessage(), t);
            }

            @Override
            public void onCompleted() {
                logger.info("Ring Message Completed");
            }
        };

    }

    private PrimeMessageState getCachedPrimeState(long number, PrimeMessageState currentState) {
        String result = redisClient.get(number + "");
        return result == null ?
                PROCESSING : ResponseUtils.stringToState(result);
    }


    private void forwardRingMessage(
            long number,
            ServerInfo originServer,
            PrimeMessageState state,
            String requestId,
            StreamObserver<RingMessageResponse> ringMessageResponseStreamObserver
    ) {

        logger.info("Forwarding ring request");

        NextRingServerRequest request = NextRingServerRequest
                .newBuilder()
                .setCurrentRingServer(serverDetails.getThisServerInfo())
                .build();

        // Obtain the next server information from RingManager asynchronously
        StreamObserver<NextRingServerRequest> requestStream = ringManagerStub.nextRingServer(
                 new StreamObserver<NextRingServerResponse>() {
                    @Override
                    public void onNext(NextRingServerResponse nextRingServerResponse) {
                        logger.info("Received next ring response");
                        ServerInfo nextServer = nextRingServerResponse.getNextRingServer();
                        RingMessageBuilder.ringMessageRequest(number, requestId, state, originServer, nextServer, ringMessageResponseStreamObserver);

                    }

                    @Override
                    public void onError(Throwable t) {
                        logger.error("Received error on next ring server " + t.getMessage());
                    }

                    @Override
                    public void onCompleted() {
                        logger.info("Received next ring server request completed");
                    }
                });
        requestStream.onNext(request);
        requestStream.onCompleted();
    }

    private boolean ringLoopCompleted(RingMessageRequest request) {
        return request.getOrigin().equals(serverDetails.getThisServerInfo());
    }

}

