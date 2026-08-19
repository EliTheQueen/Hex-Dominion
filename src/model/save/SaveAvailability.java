package model.save;

import model.ActionAvailability;

/** A complete answer for both enforcement and disabled UI messaging. */
public final class SaveAvailability extends ActionAvailability {
    private SaveAvailability(boolean enabled, String reason) {
        super(enabled, reason);
    }

    public static SaveAvailability enabled(String reason) {
        return new SaveAvailability(true, reason);
    }

    public static SaveAvailability disabled(String reason) {
        return new SaveAvailability(false, reason);
    }

}
