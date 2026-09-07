package server;

import network.JsonMessageCodec;
import network.MessageType;
import network.NetworkMessage;
import network.messages.BuildRequest;
import network.messages.ErrorResponse;
import network.messages.GameStateUpdate;
import network.messages.MoveUnitRequest;
import network.messages.StartGameRequest;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/** Multi-threaded TCP host for one authoritative single-player session. */
public final class GameServer implements Closeable {
    public static final int TCP_PORT = 8082;

    private final int requestedPort;
    private final AtomicInteger nextClientId = new AtomicInteger(1);
    private final Map<Integer, ClientHandler> clients = new ConcurrentHashMap<>();
    private final ExecutorService clientPool = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "game-client-handler");
        thread.setDaemon(true);
        return thread;
    });
    private final AuthoritativeGameService game = new AuthoritativeGameService();
    private volatile boolean running;
    private volatile int controllerClientId = -1;
    private ServerSocket serverSocket;

    public GameServer() { this(TCP_PORT); }

    public GameServer(int port) {
        if (port < 0 || port > 65535) throw new IllegalArgumentException("invalid TCP port");
        requestedPort = port;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(requestedPort);
        running = true;
        System.out.println("TCP server started on port " + getPort());
        try {
            while (running) {
                Socket socket = serverSocket.accept();
                socket.setTcpNoDelay(true);
                int clientId = nextClientId.getAndIncrement();
                ClientHandler handler = new ClientHandler(this, socket, clientId);
                clients.put(clientId, handler);
                clientPool.execute(handler);
            }
        } catch (SocketException exception) {
            if (running) throw exception;
        } finally {
            close();
        }
    }

    void handle(ClientHandler client, NetworkMessage message) {
        if (message.getType() == MessageType.HELLO) {
            if (!client.identify()) {
                sendError(client, message, "ALREADY_IDENTIFIED",
                        "HELLO has already been completed for this connection.");
                return;
            }
            assignControllerIfNeeded(client);
            client.send(NetworkMessage.response(MessageType.HELLO_ACK, message.getRequestId(),
                    new HelloResponse(client.getClientId(), client.getClientId() == controllerClientId,
                            getPort())));
            AuthoritativeGameService.Snapshot snapshot = game.snapshot();
            if (snapshot != null) client.send(stateMessage(null, snapshot));
            return;
        }
        if (!client.isIdentified()) {
            sendError(client, message, "HELLO_REQUIRED",
                    "Send HELLO before gameplay requests.");
            return;
        }
        if (client.getClientId() != controllerClientId) {
            sendError(client, message, "OBSERVER_READ_ONLY",
                    "This client is an observer; only the controller client may change the game.");
            return;
        }

        ActionResult result;
        try {
            result = switch (message.getType()) {
                case START_GAME -> game.start(JsonMessageCodec.payload(message, StartGameRequest.class));
                case MOVE_UNIT -> game.move(JsonMessageCodec.payload(message, MoveUnitRequest.class));
                case END_TURN -> game.endTurn();
                case BUILD -> game.build(JsonMessageCodec.payload(message, BuildRequest.class));
                default -> ActionResult.failure("UNSUPPORTED_MESSAGE",
                        "Server does not accept " + message.getType() + " as a request.");
            };
        } catch (IllegalArgumentException exception) {
            result = ActionResult.failure("MALFORMED_REQUEST", exception.getMessage());
        } catch (RuntimeException exception) {
            result = ActionResult.failure("SERVER_ERROR", "The server could not complete the action.");
            exception.printStackTrace();
        }

        if (!result.isSuccessful()) {
            sendError(client, message, result.getCode(), result.getMessage());
            return;
        }
        AuthoritativeGameService.Snapshot snapshot = game.snapshot();
        if (snapshot != null) broadcast(stateMessage(message.getRequestId(), snapshot));
    }

    private NetworkMessage stateMessage(String requestId, AuthoritativeGameService.Snapshot snapshot) {
        return NetworkMessage.response(MessageType.STATE_UPDATE, requestId,
                new GameStateUpdate(snapshot.getRevision(), snapshot.getState()));
    }

    private void sendError(ClientHandler client, NetworkMessage request, String code, String text) {
        client.send(NetworkMessage.response(MessageType.ERROR, request.getRequestId(),
                new ErrorResponse(code, text)));
    }

    public void broadcast(NetworkMessage message) {
        for (ClientHandler client : clients.values()) client.send(message);
    }

    void removeClient(ClientHandler client) {
        clients.remove(client.getClientId(), client);
        if (controllerClientId == client.getClientId()) {
            controllerClientId = -1;
        }
    }

    private synchronized void assignControllerIfNeeded(ClientHandler candidate) {
        if (controllerClientId != -1 && clients.containsKey(controllerClientId)) return;
        if (candidate.isIdentified() && clients.containsKey(candidate.getClientId())) {
            controllerClientId = candidate.getClientId();
        }
    }

    public int getPort() {
        return serverSocket == null ? requestedPort : serverSocket.getLocalPort();
    }

    public int getConnectedClientCount() { return clients.size(); }

    @Override
    public void close() {
        running = false;
        if (serverSocket != null) {
            try { serverSocket.close(); } catch (IOException ignored) {}
        }
        for (ClientHandler client : clients.values()) client.close();
        clients.clear();
        clientPool.shutdownNow();
    }

    private static final class HelloResponse {
        private final int clientId;
        private final boolean controller;
        private final int tcpPort;

        private HelloResponse(int clientId, boolean controller, int tcpPort) {
            this.clientId = clientId;
            this.controller = controller;
            this.tcpPort = tcpPort;
        }
    }
}
