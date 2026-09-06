package network;

/**
 * @deprecated Kept only as a source-compatible bridge for the original typo.
 * Use {@link NetworkMessage} for all network traffic.
 */
@Deprecated
public class Massage extends NetworkMessage {
    public Massage(MessageType type) {
        super(type);
    }
}
