package app;

import server.GameServer;

public class ServerMain {

    public static void main(String[] args) {

        int port = args.length == 0 ? GameServer.TCP_PORT : Integer.parseInt(args[0]);
        GameServer server = new GameServer(port);

        Runtime.getRuntime().addShutdownHook(new Thread(server::close, "server-shutdown"));

        try {
            server.start();
        } catch (Exception e) {
            System.out.println("Server stopped: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
