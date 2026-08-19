package model.happiness;

public class HappinessService implements java.io.Serializable {

    private final HappinessTracker tracker;
    private boolean townHallGarrisoned;

    public HappinessService(HappinessTracker tracker) {
        if (tracker == null) {
            throw new IllegalArgumentException("tracker must not be null");
        }

        this.tracker = tracker;
    }

    public void applyEvent(HappinessEventType eventType) {
        if (eventType == null) {
            throw new IllegalArgumentException("eventType must not be null");
        }

        tracker.applyChange(eventType.getScoreChange());
    }

    public HappinessLevel getCurrentLevel() {
        return HappinessLevel.fromScore(getCurrentScore());
    }

    public int getCurrentScore() {
        return tracker.getScore() + (townHallGarrisoned ? 1 : 0);
    }

    /** A current condition, not a cumulative event; multiple guards still contribute only +1. */
    public void setTownHallGarrisoned(boolean garrisoned) {
        townHallGarrisoned = garrisoned;
    }

    public boolean isTownHallGarrisoned() { return townHallGarrisoned; }
}
