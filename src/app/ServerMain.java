package app;

import server.GameServer;
import server.UdpHeartbeatServer;

public class ServerMain {

    public static void main(String[] args) {

        int tcpPort = args.length < 1 ? GameServer.TCP_PORT : Integer.parseInt(args[0]);
        int udpPort = args.length < 2 ? UdpHeartbeatServer.UDP_PORT : Integer.parseInt(args[1]);
        GameServer server = new GameServer(tcpPort);

        try {
            UdpHeartbeatServer heartbeat = new UdpHeartbeatServer(udpPort);
            Thread heartbeatThread = new Thread(heartbeat, "udp-heartbeat-server");
            heartbeatThread.setDaemon(true);
            heartbeatThread.start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                server.close();
                heartbeat.close();
            }, "server-shutdown"));
            server.start();
        } catch (Exception e) {
            System.out.println("Server stopped: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
