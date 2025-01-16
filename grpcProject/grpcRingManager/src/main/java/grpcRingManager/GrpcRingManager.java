package grpcRingManager;

// generic ServerApp for hosting grpcService

import io.grpc.ServerBuilder;
import io.grpc.netty.shaded.io.netty.util.internal.logging.InternalLogger;
import io.grpc.netty.shaded.io.netty.util.internal.logging.Log4J2LoggerFactory;


public class GrpcRingManager {

    private static final InternalLogger logger = Log4J2LoggerFactory.getInstance(GrpcRingManager.class);
    
    public static void main(String[] args) {
        int svcPort;

        if (args.length >= 1) {
            svcPort = Integer.parseInt(args[0]);
        } else {
            return;
        }

        try {
            io.grpc.Server svc = ServerBuilder
                    .forPort(svcPort)
                    .addService(new RingManagerPrimeService())
                    .addService(new RingManagerClientService())
                    .build();
            svc.start();
            logger.info("Server started, listening on " + svcPort);


            Runtime.getRuntime().addShutdownHook(new ShutdownHook(svc));
            svc.awaitTermination();
            svc.shutdown();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
