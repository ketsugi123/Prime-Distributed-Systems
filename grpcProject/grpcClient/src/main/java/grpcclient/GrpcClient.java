package grpcclient;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.netty.shaded.io.netty.util.internal.logging.InternalLogger;
import io.grpc.netty.shaded.io.netty.util.internal.logging.InternalLoggerFactory;
import io.grpc.stub.StreamObserver;
import ringManagerClientService.RingManagerClientServiceGrpc;
import ringManagerClientService.GetPrimeServerRequest;
import ringManagerClientService.GetPrimeServerResponse;
import primeServerService.PrimeServerServiceGrpc;
import primeServerService.PrimeRequest;
import primeServerService.PrimeResponse;
import shared.General.ServerInfo;

import java.util.Scanner;

public class GrpcClient {
    
    private static InternalLogger logger = InternalLoggerFactory.getInstance(GrpcClient.class);

    public static void main(String[] args) throws InterruptedException {
        String ringManagerAddress;
        int ringManagerPort;

        if (args.length >= 2) {
            ringManagerPort = Integer.parseInt(args[0]);
            ringManagerAddress = args[1];
        } else {
            logger.info("No Ring Manager port and address specified");
            System.out.println("Usage: <PRIME_SERVER_ADDRESS> <PRIME_SERVER_PORT> <REDIS_PORT> <RING_MANAGER_ADDRESS> <RING_MANAGER_PORT>\n" +
                    "\n");
            return; // Exit if no args are provided
        }

        // Ring Manager Channel
        ManagedChannel ringManagerChannel = ManagedChannelBuilder.forAddress(ringManagerAddress, ringManagerPort)
                .usePlaintext()
                .build();

        // Ring Manager client stub
        RingManagerClientServiceGrpc.RingManagerClientServiceStub ringManagerClient =
                RingManagerClientServiceGrpc.newStub(ringManagerChannel);

        logger.info("Requesting Prime Server from Ring Manager...");
        GetPrimeServerRequest request = GetPrimeServerRequest.newBuilder().build();
        
        ringManagerClient.getPrimeServer(request, new StreamObserver<GetPrimeServerResponse>() {
            @Override
            public void onNext(GetPrimeServerResponse response) {
                ServerInfo primeServerInfo = response.getPrimeServer();
                String primeServerAddress = primeServerInfo.getIp();
                int primeServerPort = primeServerInfo.getPort();
                
                logger.info("Prime Server Info: " + primeServerAddress + ":" + primeServerPort);
                
                ringManagerChannel.shutdownNow();
                
                // Prime Server Channel
                ManagedChannel primeServerChannel = ManagedChannelBuilder.forAddress(primeServerAddress, primeServerPort)
                        .usePlaintext()
                        .build();
                
                // Prime Server client stub
                PrimeServerServiceGrpc.PrimeServerServiceStub asyncClient = PrimeServerServiceGrpc.newStub(primeServerChannel);
                
                // Get input from the user
                Scanner scanner = new Scanner(System.in);
                System.out.print("Enter a number to check if it's prime: ");
                long number = scanner.nextLong();
                
                // Prepare the request
                PrimeRequest primeRequest = PrimeRequest.newBuilder().setNumber(number).build();
                
                // Create a StreamObserver for the response
                StreamObserver<PrimeResponse> responseObserver = new StreamObserver<PrimeResponse>() {
                    @Override
                    public void onNext(PrimeResponse response) {
                        boolean isPrime = response.getIsPrime();
                        logger.info("The number " + number + " is " + (isPrime ? "prime" : "not prime"));
                        System.exit(0);
                    }
                    
                    @Override
                    public void onError(Throwable t) {
                        logger.error("Error checking prime status: " + t.getMessage());
                        primeServerChannel.shutdown();
                        System.exit(1);
                    }
                    
                    @Override
                    public void onCompleted() {
                        logger.info("Completed checking prime status.");
                        primeServerChannel.shutdown();
                    }
                };
                
                StreamObserver<PrimeRequest> requestObserver = asyncClient.isPrime(responseObserver);
                
                requestObserver.onNext(primeRequest);
                requestObserver.onCompleted();
            }
            
            @Override
            public void onError(Throwable t) {
               logger.error("Error checking prime status: " + t.getMessage());
            
            }
            
            @Override
            public void onCompleted() {
                logger.info("Completed checking prime Request");
            }
        });
        
        while (true) {
        
        }
    }
}
