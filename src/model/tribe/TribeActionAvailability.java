package model.tribe;

/** A UI-safe action decision that always carries a useful explanation. */
public final class TribeActionAvailability {
    private final boolean available;
    private final String reason;

    private TribeActionAvailability(boolean available, String reason) {
        this.available = available;
        this.reason = reason;
    }

    public static TribeActionAvailability available(String description) {
        return new TribeActionAvailability(true, description);
    }

    public static TribeActionAvailability unavailable(String reason) {
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("disabled reason is required");
        return new TribeActionAvailability(false, reason);
    }

    public boolean isAvailable() { return available; }
    public String getReason() { return reason; }
}
