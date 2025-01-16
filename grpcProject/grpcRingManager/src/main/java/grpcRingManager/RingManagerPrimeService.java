package grpcRingManager;

import io.grpc.netty.shaded.io.netty.util.internal.logging.InternalLogger;
import io.grpc.netty.shaded.io.netty.util.internal.logging.Log4J2LoggerFactory;
import io.grpc.stub.StreamObserver;
import ringManagerPrimeService.*;
import shared.General.ServerInfo;


public class RingManagerPrimeService extends RingManagerPrimeServiceGrpc.RingManagerPrimeServiceImplBase {
    
    private final InternalLogger logger = Log4J2LoggerFactory.getInstance(this.getClass());
    
    @Override
    public void registerServer(RegisterServerRequest request, StreamObserver<RegisterServerResponse> responseObserver) {
        ServerInfo primeServer = request.getPrimeServer();
        logger.info(primeServer.toString());
        String ip = primeServer.getIp();
        int port = primeServer.getPort();
        
        boolean insertedValue = RingManagerData.insert(primeServer);

        RegisterServerResponse response = RegisterServerResponse.newBuilder()
                .setNextServer(RingManagerData.findNext(primeServer))
                .build();

        if(insertedValue) {
            logger.info("Registered PrimeServer: " + ip + ":" + port);
        } else {
            logger.info("Prime Server Already Registered: " + ip + ":" + port);
        }
        responseObserver.onNext(response);
    }
}
