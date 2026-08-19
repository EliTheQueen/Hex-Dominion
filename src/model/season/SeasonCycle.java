package model.season;

public class SeasonCycle implements java.io.Serializable {

    public static final int TURNS_PER_SEASON = 10;

    private int currentTurn;

    public SeasonCycle() {
        this(1);
    }

    public SeasonCycle(int currentTurn) {
        if (currentTurn <= 0) {
            throw new IllegalArgumentException("currentTurn must be greater than zero");
        }

        this.currentTurn = currentTurn;
    }

    public int getCurrentTurn() {
        return currentTurn;
    }

    public Season getCurrentSeason() {
        int seasonIndex = ((currentTurn - 1) / TURNS_PER_SEASON) % Season.values().length;

        return Season.values()[seasonIndex];
    }

    public int getTurnInsideSeason() {
        return ((currentTurn - 1) % TURNS_PER_SEASON) + 1;
    }

    public boolean isLastTurnOfSeason() {
        return getTurnInsideSeason() == TURNS_PER_SEASON;
    }

    public void advanceTurn() {
        currentTurn++;
    }

    public void restoreTurn(int turn) {
        if (turn <= 0) {
            throw new IllegalArgumentException("turn must be greater than zero");
        }

        currentTurn = turn;
    }
}
