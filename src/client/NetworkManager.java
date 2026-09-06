package client;

import model.Constants;
import model.HexCoordinate;
import network.MessageType;
import network.NetworkMessage;
import network.messages.BuildRequest;
import network.messages.EndTurnRequest;
import network.messages.MoveUnitRequest;
import network.messages.StartGameRequest;
import network.JsonMessageCodec;

import javax.swing.SwingUtilities;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** UI-neutral client facade. All blocking connect/read/write work happens off the EDT. */
public final class NetworkManager implements Closeable {
    private final String host;
    private final int tcpPort;
    private final int udpPort;
    private final CopyOnWriteArrayList<NetworkEventListener> listeners = new CopyOnWriteArrayList<>();
    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "network-sender");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicBoolean connected = new AtomicBoolean();

    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private UdpHeartbeatClient heartbeat;

    public NetworkManager(String host, int tcpPort) {
        this(host, tcpPort, tcpPort + 1);
    }

    public NetworkManager(String host, int tcpPort, int udpPort) {
        this.host = Objects.requireNonNull(host, "host is required");
        this.tcpPort = tcpPort;
        this.udpPort = udpPort;
    }

    /** Intended for CLI/tests; Swing callers should use {@link #connectAsync()}. */
    public synchronized void connect() throws IOException {
        if (connected.get()) return;
        Socket newSocket = new Socket();
        newSocket.connect(new InetSocketAddress(host, tcpPort), 3000);
        newSocket.setTcpNoDelay(true);
        socket = newSocket;
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        writer = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
        connected.set(true);

        Thread listenerThread = new Thread(new ServerListener(reader, this), "server-listener");
        listenerThread.setDaemon(true);
        listenerThread.start();
        startHeartbeat();
        send(NetworkMessage.request(MessageType.HELLO, new HelloRequest("Hex Dominion client")));
    }

    public void connectAsync() {
        networkExecutor.execute(() -> {
            try {
                connect();
            } catch (IOException exception) {
                connectionClosed("Could not connect: " + exception.getMessage());
            }
        });
    }

    public void addListener(NetworkEventListener listener) {
        listeners.add(Objects.requireNonNull(listener, "listener is required"));
    }

    public void removeListener(NetworkEventListener listener) { listeners.remove(listener); }

    public void startGame(int width, int height, Long seed) {
        send(NetworkMessage.request(MessageType.START_GAME, new StartGameRequest(width, height, seed)));
    }

    public void moveUnit(int unitIndex, HexCoordinate destination) {
        Objects.requireNonNull(destination, "destination is required");
        send(NetworkMessage.request(MessageType.MOVE_UNIT,
                new MoveUnitRequest(unitIndex, destination.getQ(), destination.getR())));
    }

    public void endTurn() {
        send(NetworkMessage.request(MessageType.END_TURN, new EndTurnRequest()));
    }

    public void build(int builderIndex, Constants.BuildingType type, HexCoordinate coordinate) {
        Objects.requireNonNull(type, "building type is required");
        Objects.requireNonNull(coordinate, "coordinate is required");
        send(NetworkMessage.request(MessageType.BUILD,
                new BuildRequest(builderIndex, type.name(), coordinate.getQ(), coordinate.getR())));
    }

    public void send(NetworkMessage message) {
        networkExecutor.execute(() -> {
            PrintWriter current = writer;
            if (!connected.get() || current == null) {
                deliver(listener -> listener.onError("NOT_CONNECTED", "Not connected to the server."));
                return;
            }
            current.println(JsonMessageCodec.encode(message));
            if (current.checkError()) connectionClosed("Could not send data to the server.");
        });
    }

    private void startHeartbeat() {
        try {
            heartbeat = new UdpHeartbeatClient(host, udpPort,
                    reachable -> deliver(listener -> listener.onHeartbeat(reachable)));
            Thread heartbeatThread = new Thread(heartbeat, "udp-heartbeat-client");
            heartbeatThread.setDaemon(true);
            heartbeatThread.start();
        } catch (IOException exception) {
            deliver(listener -> listener.onHeartbeat(false));
        }
    }

    void deliver(Consumer<NetworkEventListener> event) {
        Runnable delivery = () -> listeners.forEach(event);
        if (SwingUtilities.isEventDispatchThread()) delivery.run();
        else SwingUtilities.invokeLater(delivery);
    }

    void connectionClosed(String reason) {
        if (!connected.compareAndSet(true, false)) {
            if (socket == null) deliver(listener -> listener.onDisconnected(reason));
            return;
        }
        closeResources();
        deliver(listener -> listener.onDisconnected(reason));
    }

    public boolean isConnected() { return connected.get(); }

    public void disconnect() { close(); }

    @Override
    public void close() {
        boolean wasConnected = connected.getAndSet(false);
        closeResources();
        networkExecutor.shutdownNow();
        if (wasConnected) deliver(listener -> listener.onDisconnected("Disconnected."));
    }

    private synchronized void closeResources() {
        if (heartbeat != null) heartbeat.close();
        if (socket != null) {
            try { socket.close(); } catch (IOException ignored) {}
        }
        socket = null;
        reader = null;
        writer = null;
    }

    private static final class HelloRequest {
        private final String clientName;
        private HelloRequest(String clientName) { this.clientName = clientName; }
    }
}
