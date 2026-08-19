package model.tribe.behavior;

//تمام اطلاعات Turn
public class TribeTurnContext {

    private final int currentTurn;
    private final boolean campUnderAttack;
    private final int currentGuardCount;
    private final boolean activeMissionExists;

    public TribeTurnContext(
            int currentTurn,
            boolean campUnderAttack,
            int currentGuardCount,
            boolean activeMissionExists
    ) {
        if (currentTurn < 0) {
            throw new IllegalArgumentException("currentTurn must not be negative");
        }

        if (currentGuardCount < 0) {
            throw new IllegalArgumentException("currentGuardCount must not be negative");
        }

        this.currentTurn = currentTurn;
        this.campUnderAttack = campUnderAttack;
        this.currentGuardCount = currentGuardCount;
        this.activeMissionExists = activeMissionExists;
    }

    public int getCurrentTurn() {
        return currentTurn;
    }

    public boolean isCampUnderAttack() {
        return campUnderAttack;
    }

    public int getCurrentGuardCount() {
        return currentGuardCount;
    }

    public boolean isActiveMissionExists() {
        return activeMissionExists;
    }
}