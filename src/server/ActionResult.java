package server;

/** Result of an authoritative command, including a user-facing validation reason. */
public final class ActionResult {
    private final boolean successful;
    private final String code;
    private final String message;

    private ActionResult(boolean successful, String code, String message) {
        this.successful = successful;
        this.code = code;
        this.message = message;
    }

    public static ActionResult success(String message) {
        return new ActionResult(true, "OK", message);
    }

    public static ActionResult failure(String code, String message) {
        return new ActionResult(false, code, message);
    }

    public boolean isSuccessful() { return successful; }
    public String getCode() { return code; }
    public String getMessage() { return message; }
}
