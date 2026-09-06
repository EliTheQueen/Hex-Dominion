package network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

/** Central JSON codec; sockets never exchange ad-hoc colon-delimited strings. */
public final class JsonMessageCodec {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private JsonMessageCodec() {}

    public static String encode(NetworkMessage message) {
        if (message == null) throw new IllegalArgumentException("message is required");
        return GSON.toJson(message);
    }

    public static NetworkMessage decode(String json) {
        try {
            NetworkMessage message = GSON.fromJson(json, NetworkMessage.class);
            if (message == null || message.getType() == null) {
                throw new IllegalArgumentException("JSON message must contain a valid type");
            }
            return message;
        } catch (JsonParseException exception) {
            throw new IllegalArgumentException("Malformed JSON message", exception);
        }
    }

    public static JsonObject toObject(Object value) {
        if (value == null) return new JsonObject();
        JsonElement element = GSON.toJsonTree(value);
        if (!element.isJsonObject()) {
            JsonObject wrapper = new JsonObject();
            wrapper.add("value", element);
            return wrapper;
        }
        return element.getAsJsonObject();
    }

    public static <T> T payload(NetworkMessage message, Class<T> type) {
        try {
            return GSON.fromJson(message.getData(), type);
        } catch (JsonParseException exception) {
            throw new IllegalArgumentException("Invalid payload for " + message.getType(), exception);
        }
    }
}
