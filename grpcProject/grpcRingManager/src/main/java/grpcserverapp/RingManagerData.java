package grpcserverapp;

import shared.General.ServerInfo;

import javax.annotation.Nullable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class RingManagerData {

    // Stores registered PrimeServers
    private static final CopyOnWriteArrayList<ServerInfo> registeredServers = new CopyOnWriteArrayList<>();
    private static final AtomicInteger roundRobinIndex = new AtomicInteger(0);

    public static boolean insert(ServerInfo serverInfo) {
        if(!contains(serverInfo)) {
            return registeredServers.add(serverInfo);
        }
        return false;
    }

    public static ServerInfo get(Integer index) {
        return registeredServers.get(index);
    }

    public static boolean isEmpty() {
        return registeredServers.isEmpty();
    }

    public static boolean contains(ServerInfo serverInfo) {
        String ip = serverInfo.getIp();
        int port = serverInfo.getPort();
        return registeredServers.stream().anyMatch(server ->
                server.getIp().equals(ip) && server.getPort() == port
        );
    }

    @Nullable
    public static ServerInfo findNext(ServerInfo currentServerInfo) {
        int currentIndex = -1;
        String currentIp = currentServerInfo.getIp();
        int currentPort = currentServerInfo.getPort();
        for (int i = 0; i < registeredServers.size(); i++) {
            ServerInfo server = registeredServers.get(i);
            if (server.getIp().equals(currentIp) && server.getPort() == currentPort) {
                currentIndex = i;
                break;
            }
        }

        if(currentIndex == -1) {
            return null;
        }
        else {
            // % used in case the current server is the last in the ring
            int nextIndex = (currentIndex + 1) % registeredServers.size();
            return registeredServers.get(nextIndex);
        }
    }

    public static ServerInfo getPrimeServer() {
        int index = roundRobinIndex.getAndUpdate(i -> (i + 1) % registeredServers.size());
        return registeredServers.get(index);
    }
}
