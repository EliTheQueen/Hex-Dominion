package app;

import client.NetworkManager;
import client.NetworkEventListener;
import model.GameState;

import java.io.IOException;
import java.util.Scanner;

public class NetworkClientMain {

    public static void main(String[] args) {

        String host = args.length < 1 ? "localhost" : args[0];
        int tcpPort = args.length < 2 ? 8082 : Integer.parseInt(args[1]);
        int udpPort = args.length < 3 ? 8083 : Integer.parseInt(args[2]);
        NetworkManager networkManager = new NetworkManager(host, tcpPort, udpPort);
        networkManager.addListener(new NetworkEventListener() {
            @Override public void onHello(int clientId, boolean controller) {
                System.out.println("HELLO client=" + clientId + " role="
                        + (controller ? "controller" : "observer"));
            }
            @Override public void onStateUpdate(GameState state, long revision) {
                System.out.println("STATE_UPDATE revision=" + revision
                        + " turn=" + state.getCurrentTurn());
            }
            @Override public void onError(String code, String message) {
                System.out.println("ERROR " + code + ": " + message);
            }
            @Override public void onHeartbeat(boolean reachable) {
                System.out.println("UDP heartbeat: " + (reachable ? "PONG" : "timeout"));
            }
            @Override public void onDisconnected(String reason) {
                System.out.println(reason);
            }
        });

        try {

            networkManager.connect();

        } catch (IOException e) {

            System.out.println("Could not connect to server: " + e.getMessage());

            return;
        }

        Scanner scanner = new Scanner(System.in);

        System.out.println("Commands: start [seed], move <unitIndex> <x> <y>, end, "
                + "build <builderIndex> <TYPE> <x> <y>, exit");

        while (networkManager.isConnected()) {

            String input = scanner.nextLine();

            if ("exit".equalsIgnoreCase(input)) {
                break;
            }

            try {
                String[] command = input.trim().split("\\s+");
                switch (command[0].toLowerCase()) {
                    case "start" -> networkManager.startGame(15, 13,
                            command.length > 1 ? Long.parseLong(command[1]) : null);
                    case "move" -> networkManager.moveUnit(Integer.parseInt(command[1]),
                            new model.HexCoordinate(Integer.parseInt(command[2]), Integer.parseInt(command[3])));
                    case "end" -> networkManager.endTurn();
                    case "build" -> networkManager.build(Integer.parseInt(command[1]),
                            model.Constants.BuildingType.valueOf(command[2].toUpperCase()),
                            new model.HexCoordinate(Integer.parseInt(command[3]), Integer.parseInt(command[4])));
                    default -> System.out.println("Unknown command.");
                }
            } catch (RuntimeException exception) {
                System.out.println("Invalid command: " + exception.getMessage());
            }
        }

        networkManager.disconnect();

        scanner.close();

        System.out.println("Client stopped.");
    }
}
