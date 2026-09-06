package network;

import com.google.gson.JsonObject;

import java.util.Objects;
import java.util.UUID;

/** A small JSON envelope shared by requests, responses and server pushes. */
public class NetworkMessage {
    private final MessageType type;
    private final String requestId;
    private final JsonObject data;

    public NetworkMessage(MessageType type) {
        this(type, UUID.randomUUID().toString(), new JsonObject());
    }

    public NetworkMessage(MessageType type, String requestId, JsonObject data) {
        this.type = Objects.requireNonNull(type, "message type is required");
        this.requestId = requestId == null || requestId.isBlank()
                ? UUID.randomUUID().toString() : requestId;
        this.data = data == null ? new JsonObject() : data;
    }

    public MessageType getType() { return type; }
    public String getRequestId() { return requestId; }
    public JsonObject getData() { return data; }

    public static NetworkMessage request(MessageType type, Object payload) {
        return new NetworkMessage(type, UUID.randomUUID().toString(),
                JsonMessageCodec.toObject(payload));
    }

    public static NetworkMessage response(MessageType type, String requestId, Object payload) {
        return new NetworkMessage(type, requestId, JsonMessageCodec.toObject(payload));
    }
}
