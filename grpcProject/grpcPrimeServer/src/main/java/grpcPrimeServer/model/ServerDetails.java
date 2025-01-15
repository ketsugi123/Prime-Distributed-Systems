package grpcPrimeServer.model;

import shared.General;

public class ServerDetails {

    private final String serverAddress;
    private final int serverPort;
    public ServerDetails(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }


    public String getServerAddress() {
        return serverAddress;
    }
    public Integer getServerPort() {
        return serverPort;
    }



    public General.ServerInfo getThisServerInfo() {
        return General.ServerInfo.newBuilder()
                .setIp(serverAddress)
                .setPort(serverPort).build();
    }
}
