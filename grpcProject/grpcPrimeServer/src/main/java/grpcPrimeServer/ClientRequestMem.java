package grpcPrimeServer;

import io.grpc.netty.shaded.io.netty.util.internal.logging.InternalLogger;
import io.grpc.netty.shaded.io.netty.util.internal.logging.InternalLoggerFactory;
import io.grpc.stub.StreamObserver;
import primeServerService.PrimeResponse;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClientRequestMem {

    public static ConcurrentHashMap<String, StreamObserver<PrimeResponse>> isPrimeRequests = new ConcurrentHashMap<>();
    static InternalLogger logger = InternalLoggerFactory.getInstance(ClientRequestMem.class);
    
    
    public static String insert(StreamObserver<PrimeResponse> response) {
        String id = UUID.randomUUID().toString();
        isPrimeRequests.put(id, response);
        return id;
    }

    @Nullable
    public static StreamObserver<PrimeResponse> getResponseObserver(String id) {
        return isPrimeRequests.get(id);
    }

    public static void sendResponseToClient(String requestId, boolean isPrime) {
        StreamObserver<PrimeResponse> clientResponse = ClientRequestMem.getResponseObserver(requestId);
        if (clientResponse == null) {
            throw new RuntimeException("Client response is null");
        }
        logger.info("Sending Response to client");
        clientResponse.onNext(PrimeResponse.newBuilder().setIsPrime(isPrime).build());
        clientResponse.onCompleted();
    }

}
