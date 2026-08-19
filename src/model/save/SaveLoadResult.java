package model.save;

import model.GameState;

public final class SaveLoadResult {
    private final boolean successful;
    private final String message;
    private final GameState gameState;
    private SaveLoadResult(boolean successful, String message, GameState gameState) {
        this.successful = successful; this.message = message; this.gameState = gameState;
    }
    public static SaveLoadResult success(GameState state) { return new SaveLoadResult(true, "Loaded", state); }
    public static SaveLoadResult failure(String message) { return new SaveLoadResult(false, message, null); }
    public boolean isSuccessful() { return successful; }
    public String getMessage() { return message; }
    public GameState getGameState() { return gameState; }
}
