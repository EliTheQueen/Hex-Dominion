package client;

import model.GameState;
import network.JsonMessageCodec;
import network.NetworkMessage;
import network.messages.ErrorResponse;
import network.messages.GameStateUpdate;

import javax.swing.SwingUtilities;
import java.io.BufferedReader;
import java.io.IOException;

/** Dedicated blocking TCP reader. Parsed events cross onto the Swing EDT. */
final class ServerListener implements Runnable {
    private final BufferedReader reader;
    private final NetworkManager manager;

    ServerListener(BufferedReader reader, NetworkManager manager) {
        this.reader = reader;
        this.manager = manager;
    }

    @Override
    public void run() {
        try {
            String line;
            while (manager.isConnected() && (line = reader.readLine()) != null) {
                try {
                    dispatch(JsonMessageCodec.decode(line));
                } catch (IllegalArgumentException exception) {
                    manager.deliver(listener -> listener.onError(
                            "INVALID_SERVER_MESSAGE", exception.getMessage()));
                }
            }
            if (manager.isConnected()) manager.connectionClosed("Server closed the connection.");
        } catch (IOException exception) {
            if (manager.isConnected()) manager.connectionClosed("Connection lost: " + exception.getMessage());
        }
    }

    private void dispatch(NetworkMessage message) {
        switch (message.getType()) {
            case HELLO_ACK -> {
                HelloPayload hello = JsonMessageCodec.payload(message, HelloPayload.class);
                manager.deliver(listener -> listener.onHello(hello.clientId, hello.controller));
            }
            case STATE_UPDATE -> {
                GameStateUpdate update = JsonMessageCodec.payload(message, GameStateUpdate.class);
                GameState state = new ClientGameStateProjection(update.getSnapshot());
                manager.deliver(listener -> listener.onStateUpdate(state, update.getRevision()));
            }
            case ERROR -> {
                ErrorResponse error = JsonMessageCodec.payload(message, ErrorResponse.class);
                manager.deliver(listener -> listener.onError(error.getCode(), error.getMessage()));
            }
            default -> manager.deliver(listener -> listener.onError(
                    "UNEXPECTED_MESSAGE", "Unexpected server message: " + message.getType()));
        }
    }

    private static final class HelloPayload {
        private int clientId;
        private boolean controller;
    }
}
