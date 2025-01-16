package grpcPrimeServer;

import com.github.dockerjava.api.DockerClient;
import grpcPrimeServer.model.ServerDetails;
import grpcPrimeServer.services.PrimeServerClientImpl;
import grpcPrimeServer.services.PrimeServerCommunication;
import grpcPrimeServer.utils.DockerUtils;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.netty.shaded.io.netty.util.internal.logging.InternalLogger;
import io.grpc.netty.shaded.io.netty.util.internal.logging.Log4J2LoggerFactory;
import io.grpc.stub.StreamObserver;
import redis.clients.jedis.Jedis;
import ringManagerPrimeService.RegisterServerRequest;
import ringManagerPrimeService.RegisterServerResponse;
import ringManagerPrimeService.RingManagerPrimeServiceGrpc;
import shared.General;

import java.io.IOException;

public class PrimeServer {
    private static final InternalLogger logger = Log4J2LoggerFactory.getInstance(PrimeServer.class.getName());

    public static void main(String[] args) throws IOException, InterruptedException {
        int svcPort;
        String ringManagerAddress;
        int ringManagerPort;
        String serverAddress;
        String redisAddress;
        int redisPort;

        if (args.length >= 5) {
            serverAddress = args[0];
            redisAddress = args[0];
            svcPort = Integer.parseInt(args[1]);
            redisPort = Integer.parseInt(args[2]);
            ringManagerAddress = args[3];
            ringManagerPort = Integer.parseInt(args[4]);
        } else {
            return;
        }

        // Set up Docker client
        DockerClient dockerclient = DockerUtils.getDockerClient();
        // ServerInfo object for this PrimeServer and redis container
        ServerDetails serverDetails = new ServerDetails(serverAddress, svcPort);
        ServerDetails redisServerDetails = new ServerDetails(redisAddress, redisPort);
        ManagedChannel ringManagerChannel = ManagedChannelBuilder
                .forAddress(ringManagerAddress, ringManagerPort)
                .usePlaintext()
                .build();
        DockerUtils.launchRedis(dockerclient, redisServerDetails);

        // Connect to Redis server
        Jedis redis = new Jedis(redisAddress, redisPort);
        if (redis.getConnection().isBroken()) {
            throw new RuntimeException("Redis connection failed");
        }

        // Register this PrimeServer with the RingManager
        StreamObserver<RegisterServerRequest> serverRing = getServerRing(ringManagerChannel, serverDetails);

        // Set up and start the gRPC server for the PrimeServer service
        Server server = ServerBuilder.forPort(svcPort)
                .addService(new PrimeServerClientImpl(
                        redis, dockerclient, ringManagerChannel,
                        serverDetails, redisServerDetails, serverRing)
                )
                .addService(new PrimeServerCommunication(redis, dockerclient, ringManagerChannel, serverDetails, redisServerDetails))
                .build();


        server.start();

        logger.info("PrimeServer started, listening on {}", svcPort);

        // Add shutdown hook to gracefully stop the server
        Runtime.getRuntime().addShutdownHook(new ShutdownHook(server));

        // Block the main thread until the server terminates
        server.awaitTermination();
        server.shutdown();
    }


    private static StreamObserver<RegisterServerRequest> getServerRing(ManagedChannel channel, ServerDetails serverDetails) {
        RegisterServerRequest request = RegisterServerRequest
                .newBuilder()
                .setPrimeServer(serverDetails.getThisServerInfo())
                .build();

        // Register the server with RingManager using the asynchronous gRPC stub

        return RingManagerPrimeServiceGrpc.newStub(channel)
                .registerServer(new StreamObserver<RegisterServerResponse>() {
                    @Override
                    public void onNext(RegisterServerResponse registerServerResponse) {
                        General.ServerInfo nextServerInfo = registerServerResponse.getNextServer();
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

}
