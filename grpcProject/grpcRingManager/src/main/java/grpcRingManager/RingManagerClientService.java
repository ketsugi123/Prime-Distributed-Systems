package grpcRingManager;

import io.grpc.netty.shaded.io.netty.util.internal.logging.InternalLogger;
import io.grpc.netty.shaded.io.netty.util.internal.logging.Log4J2LoggerFactory;
import io.grpc.stub.StreamObserver;
import ringManagerClientService.GetPrimeServerRequest;
import ringManagerClientService.GetPrimeServerResponse;
import ringManagerClientService.RingManagerClientServiceGrpc;
import shared.General.ServerInfo;


public class RingManagerClientService extends RingManagerClientServiceGrpc.RingManagerClientServiceImplBase {
    
    
    private final InternalLogger logger = Log4J2LoggerFactory.getInstance(this.getClass());
    
    @Override
    public void getPrimeServer(GetPrimeServerRequest request, StreamObserver<GetPrimeServerResponse> responseObserver) {

        if (RingManagerData.isEmpty()) {
            logger.info("No prime servers");
            responseObserver.onError(new Throwable("No PrimeServers are currently registered."));
            return;
        }

        ServerInfo selectedServer = RingManagerData.getPrimeServer();
        GetPrimeServerResponse response = GetPrimeServerResponse.newBuilder()
                .setPrimeServer(selectedServer)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
