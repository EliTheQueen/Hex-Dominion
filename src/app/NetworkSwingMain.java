package app;

import client.NetworkManager;
import controller.GameController;
import server.GameServer;
import server.UdpHeartbeatServer;
import view.MainWindow;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/** Starts the existing Swing UI in server-authoritative client mode. */
public final class NetworkSwingMain {
    private NetworkSwingMain() {}

    public static void main(String[] args) {
        String host = args.length < 1 ? "localhost" : args[0];
        int tcpPort = args.length < 2 ? GameServer.TCP_PORT : Integer.parseInt(args[1]);
        int udpPort = args.length < 3 ? UdpHeartbeatServer.UDP_PORT : Integer.parseInt(args[2]);
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            NetworkManager network = new NetworkManager(host, tcpPort, udpPort);
            GameController controller = new GameController(network);
            new MainWindow(controller);
            network.connectAsync();
        });
    }
}
