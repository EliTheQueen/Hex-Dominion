package model.save;

/** A complete answer for both enforcement and disabled UI messaging. */
public final class SaveAvailability {
    private final boolean enabled;
    private final String reason;

    private SaveAvailability(boolean enabled, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("save availability needs a reason");
        }
        this.enabled = enabled;
        this.reason = reason;
    }

    public static SaveAvailability enabled(String reason) {
        return new SaveAvailability(true, reason);
    }

    public static SaveAvailability disabled(String reason) {
        return new SaveAvailability(false, reason);
    }

    public boolean isEnabled() { return enabled; }
    public String getReason() { return reason; }
}
