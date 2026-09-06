package network;

import model.GameState;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;

/**
 * Produces an isolated transport snapshot without making the domain graph JSON-aware.
 * The Base64 value is carried inside the versioned JSON STATE_UPDATE envelope.
 */
public final class GameStateSnapshotCodec {
    private GameStateSnapshotCodec() {}

    public static String encode(GameState state) {
        if (state == null) throw new IllegalArgumentException("game state is required");
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(state);
            output.flush();
            return Base64.getEncoder().encodeToString(bytes.toByteArray());
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create game-state snapshot", exception);
        }
    }

    public static GameState decode(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            throw new IllegalArgumentException("state snapshot is required");
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(encoded);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid state snapshot encoding", exception);
        }
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            input.setObjectInputFilter(ObjectInputFilter.Config.createFilter(
                    "model.**;java.base/**;!*"));
            Object value = input.readObject();
            if (!(value instanceof GameState)) {
                throw new IllegalArgumentException("Snapshot does not contain a GameState");
            }
            return (GameState) value;
        } catch (IOException | ClassNotFoundException exception) {
            throw new IllegalArgumentException("Could not read game-state snapshot", exception);
        }
    }
}
