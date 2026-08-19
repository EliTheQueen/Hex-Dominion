package model.tribe;

import model.ActionAvailability;

/** A UI-safe action decision that always carries a useful explanation. */
public final class TribeActionAvailability extends ActionAvailability {
    private TribeActionAvailability(boolean available, String reason) {
        super(available, reason);
    }

    public static TribeActionAvailability available(String description) {
        return new TribeActionAvailability(true, description);
    }

    public static TribeActionAvailability unavailable(String reason) {
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("disabled reason is required");
        return new TribeActionAvailability(false, reason);
    }

    public boolean isAvailable() { return isEnabled(); }
}
