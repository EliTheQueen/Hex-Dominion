package model.tribe;

/** Serializable diplomacy/AI notification exposed to the HUD and tribe UI. */
public final class TribeNotification implements java.io.Serializable {
    private final int turn;
    private final String tribeId;
    private final String tribeName;
    private final TribeNotificationType type;
    private final String message;

    public TribeNotification(int turn, Tribe tribe, TribeNotificationType type, String message) {
        if (turn < 0 || tribe == null || type == null || message == null || message.isBlank()) {
            throw new IllegalArgumentException("invalid tribe notification");
        }
        this.turn = turn;
        this.tribeId = tribe.getId();
        this.tribeName = tribe.getName();
        this.type = type;
        this.message = message;
    }

    public int getTurn() { return turn; }
    public String getTribeId() { return tribeId; }
    public String getTribeName() { return tribeName; }
    public TribeNotificationType getType() { return type; }
    public String getMessage() { return message; }
}
