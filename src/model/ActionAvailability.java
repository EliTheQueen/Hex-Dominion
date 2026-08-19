package model;

/**
 * A UI-neutral action decision. Every action is accompanied by a useful reason,
 * whether it is currently enabled or disabled.
 */
public class ActionAvailability {
    private final boolean enabled;
    private final String reason;

    protected ActionAvailability(boolean enabled, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("action availability needs a reason");
        }
        this.enabled = enabled;
        this.reason = reason;
    }

    public static ActionAvailability enabled(String reason) {
        return new ActionAvailability(true, reason);
    }

    public static ActionAvailability disabled(String reason) {
        return new ActionAvailability(false, reason);
    }

    public boolean isEnabled() { return enabled; }
    public String getReason() { return reason; }
}
