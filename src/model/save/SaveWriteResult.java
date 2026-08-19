package model.save;

public final class SaveWriteResult {
    private final boolean successful;
    private final String message;

    private SaveWriteResult(boolean successful, String message) {
        this.successful = successful;
        this.message = message;
    }

    public static SaveWriteResult success(String message) {
        return new SaveWriteResult(true, message);
    }

    public static SaveWriteResult failure(String message) {
        return new SaveWriteResult(false, message);
    }

    public boolean isSuccessful() { return successful; }
    public String getMessage() { return message; }
}
