package grpcPrimeServer.services;

import com.github.dockerjava.api.DockerClient;
import grpcPrimeServer.ClientRequestMem;
import grpcPrimeServer.model.ServerDetails;
import grpcPrimeServer.utils.DockerUtils;
import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.netty.util.internal.logging.InternalLogger;
import io.grpc.netty.shaded.io.netty.util.internal.logging.Log4J2LoggerFactory;
import io.grpc.stub.StreamObserver;
import primeServerService.PrimeRequest;
import primeServerService.PrimeResponse;
import primeServerService.PrimeServerServiceGrpc;
import redis.clients.jedis.Jedis;
import ringManagerPrimeService.RegisterServerRequest;
import ringManagerPrimeService.RingManagerPrimeServiceGrpc;
import shared.General;

import static grpcPrimeServer.ClientRequestMem.sendResponseToClient;


public class PrimeServerClientImpl extends PrimeServerServiceGrpc.PrimeServerServiceImplBase {

    private final Jedis redisClient;
    private final DockerClient dockerClient;
    private final RingManagerPrimeServiceGrpc.RingManagerPrimeServiceStub ringManagerStub;
    private final ServerDetails serverDetails;
    private final ServerDetails redisServerDetails;
    private final StreamObserver<RegisterServerRequest> serverRing;
    private static final InternalLogger logger = Log4J2LoggerFactory.getInstance(PrimeServerClientImpl.class);

    public PrimeServerClientImpl(
            Jedis redisClient,
            DockerClient dockerClient,
            ManagedChannel ringManagerChannel,
            ServerDetails serverDetails,
            ServerDetails redisServerDetails,
            StreamObserver<RegisterServerRequest> serverRing
    ) {
        this.redisClient = redisClient;
        this.dockerClient = dockerClient;
        this.ringManagerStub = RingManagerPrimeServiceGrpc.newStub(ringManagerChannel);
        this.serverDetails = serverDetails;
        this.redisServerDetails = redisServerDetails;
        this.serverRing = serverRing;
    }

    @Override
    public StreamObserver<PrimeRequest> isPrime(final StreamObserver<PrimeResponse> responseObserver) {
        return new StreamObserver<>() {
            @Override
            public void onNext(PrimeRequest primeRequest) {
                logger.info("Handling Prime Request");
                long requestNumber = primeRequest.getNumber();

                String cachedResult = redisClient.get(Long.toString(requestNumber));
                if (cachedResult != null) {
                    boolean isPrime = Boolean.parseBoolean(cachedResult);
                    logger.info("Redis already has result: " + isPrime);
                    responseObserver.onNext(PrimeResponse.newBuilder().setIsPrime(isPrime).build());
                    responseObserver.onCompleted();
                } else {
                    String requestId = ClientRequestMem.insert(responseObserver);
                    forwardRingMessageToNextServer(requestNumber, requestId, responseObserver);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                // Handle errors if any
                logger.error("Error in isPrime request: " + throwable.getMessage());
                responseObserver.onError(throwable);
            }

            @Override
            public void onCompleted() {
                logger.info("Prime Request processing completed.");
            }
        };
    }


    private void forwardRingMessageToNextServer(Long requestNumber, String requestId, StreamObserver<PrimeResponse> responseObserver) {
        logger.info("Forwarding Ring Message to next Server");
        RegisterServerRequest registerServerRequest = RegisterServerRequest
                .newBuilder()
                .setPrimeServer(serverDetails.getThisServerInfo())
                .build();
        serverRing.onNext(registerServerRequest);
        logger.info("Received Next Ring Server");
        General.ServerInfo nextServerInfo = null;
/**
        // if next is the same server which means there exists only one
        if (noNextRingServer(nextServerInfo)) {
            logger.info("I am the only server in the ring, launching container...");
            boolean result = DockerUtils
                    .launchPrimeContainer(dockerClient, redisServerDetails, requestNumber);
            logger.info("LONELY SERVER RESULT: " + result);

            //Send response to client and store it in the redis database
            sendResponseToClient(requestId, result);
            redisClient.set(requestNumber + "", String.valueOf(result));
        } else {
            ServerInfo originServer = serverDetails.getThisServerInfo();
            PrimeMessageState initialState = PrimeMessageState.PROCESSING;
            RingMessageBuilder.ringMessageRequest(
                    requestNumber,
                    requestId,
                    initialState,
                    originServer,
                    nextServer.getNextRingServer(), null);
        }
 */
    }

}
