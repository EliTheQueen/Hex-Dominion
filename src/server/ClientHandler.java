package server;

import network.JsonMessageCodec;
import network.MessageType;
import network.NetworkMessage;
import network.messages.ErrorResponse;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

/** One blocking reader per TCP client; writes are synchronized to preserve JSON frames. */
public final class ClientHandler implements Runnable, Closeable {
    private final GameServer server;
    private final Socket socket;
    private final int clientId;
    private final AtomicBoolean closed = new AtomicBoolean();
    private final AtomicBoolean identified = new AtomicBoolean();
    private PrintWriter writer;

    ClientHandler(GameServer server, Socket socket, int clientId) {
        this.server = server;
        this.socket = socket;
        this.clientId = clientId;
    }

    @Override
    public void run() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter output = new PrintWriter(socket.getOutputStream(), true,
                     StandardCharsets.UTF_8)) {
            writer = output;
            String line;
            while (!closed.get() && (line = reader.readLine()) != null) {
                try {
                    server.handle(this, JsonMessageCodec.decode(line));
                } catch (IllegalArgumentException exception) {
                    send(NetworkMessage.response(MessageType.ERROR, null,
                            new ErrorResponse("MALFORMED_JSON", exception.getMessage())));
                }
            }
        } catch (IOException exception) {
            if (!closed.get()) {
                System.out.println("Connection with client " + clientId + " was lost: "
                        + exception.getMessage());
            }
        } finally {
            close();
            server.removeClient(this);
            System.out.println("Client " + clientId + " disconnected.");
        }
    }

    public synchronized void send(NetworkMessage message) {
        if (closed.get() || writer == null) return;
        writer.println(JsonMessageCodec.encode(message));
        if (writer.checkError()) close();
    }

    int getClientId() { return clientId; }
    boolean identify() { return identified.compareAndSet(false, true); }
    boolean isIdentified() { return identified.get(); }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) return;
        try { socket.close(); } catch (IOException ignored) {}
    }
}
