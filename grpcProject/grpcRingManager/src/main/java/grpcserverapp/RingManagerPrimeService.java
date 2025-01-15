package grpcserverapp;

import io.grpc.netty.shaded.io.netty.util.internal.logging.InternalLogger;
import io.grpc.netty.shaded.io.netty.util.internal.logging.Log4J2LoggerFactory;
import io.grpc.stub.StreamObserver;
import ringManagerPrimeService.*;
import shared.General.ServerInfo;
import shared.General.SimpleResponse;


public class RingManagerPrimeService extends RingManagerPrimeServiceGrpc.RingManagerPrimeServiceImplBase {
    
    private final InternalLogger logger = Log4J2LoggerFactory.getInstance(this.getClass());
    
    @Override
    public void registerServer(RegisterServerRequest request, StreamObserver<RegisterServerResponse> responseObserver) {
        ServerInfo primeServer = request.getPrimeServer();
        logger.info(primeServer.toString());
        String ip = primeServer.getIp();
        int port = primeServer.getPort();
        
        boolean insertedValue = RingManagerData.insert(primeServer);

        if(insertedValue) {
            logger.info("Registered PrimeServer: " + ip + ":" + port);

            SimpleResponse simpleResponse = SimpleResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Server registered successfully")
                    .build();

            RegisterServerResponse response = RegisterServerResponse.newBuilder()
                    .setResponse(simpleResponse)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } else {
            logger.info("Prime Server Already Registered: " + ip + ":" + port);

            SimpleResponse simpleResponse = SimpleResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Prime Server Already Registered")
                    .build();

            RegisterServerResponse response = RegisterServerResponse.newBuilder()
                    .setResponse(simpleResponse)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }


    }

    @Override
    public StreamObserver<NextRingServerRequest> nextRingServer(StreamObserver<NextRingServerResponse> responseObserver) {
        return new StreamObserver<NextRingServerRequest>() {

            @Override
            public void onNext(NextRingServerRequest request) {
                logger.info("Next Ring Server being executed");
                ServerInfo currentPrimeServer = request.getCurrentRingServer();

                ServerInfo nextServer = RingManagerData.findNext(currentPrimeServer);
                logger.info("Current: "+ currentPrimeServer + "Next Ring Server: " + nextServer);
                if (nextServer != null) {
                    NextRingServerResponse response = NextRingServerResponse.newBuilder()
                            .setNextRingServer(nextServer)
                            .build();
                    responseObserver.onNext(response);
                    responseObserver.onCompleted();
                } else {
                    responseObserver.onError(new Throwable("Current server not found in registered servers list."));
                }

            }

            @Override
            public void onError(Throwable t) {
                logger.error(t.getMessage(), t);
            }

            @Override
            public void onCompleted() {

            }
        };

    }

}
