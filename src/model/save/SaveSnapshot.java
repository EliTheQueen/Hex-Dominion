package model.save;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import model.GameState;

/** Version-3 model-only snapshot boundary with a semantic graph checksum. */
public final class SaveSnapshot implements Serializable {
    private static final long serialVersionUID = 1L;

    private final byte[] modelPayload;
    private final String payloadChecksum;

    private SaveSnapshot(byte[] modelPayload, String payloadChecksum) {
        this.modelPayload = modelPayload.clone();
        this.payloadChecksum = payloadChecksum;
    }

    public static SaveSnapshot capture(GameState state) throws IOException {
        if (state == null) throw new IllegalArgumentException("state must not be null");
        state.validatePersistentState();
        byte[] payload = serialize(state);
        return new SaveSnapshot(payload, checksum(payload));
    }

    public GameState restoreVerified() throws IOException {
        if (modelPayload == null || payloadChecksum == null
                || !payloadChecksum.equals(checksum(modelPayload))) {
            throw new IOException("save snapshot checksum mismatch");
        }
        try (ObjectInputStream input = new ObjectInputStream(
                new ByteArrayInputStream(modelPayload))) {
            Object value = input.readObject();
            if (!(value instanceof GameState)) {
                throw new IOException("snapshot payload is not a game state");
            }
            return (GameState) value;
        } catch (ClassNotFoundException ex) {
            throw new IOException("snapshot model class is unavailable", ex);
        }
    }

    public static String fingerprint(GameState state) throws IOException {
        if (state == null) throw new IllegalArgumentException("state must not be null");
        return checksum(serialize(state));
    }

    private static byte[] serialize(GameState state) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(state);
        }
        return bytes.toByteArray();
    }

    private static String checksum(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }
}
