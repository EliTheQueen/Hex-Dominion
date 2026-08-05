package model.happiness;

public class HappinessService {

    private final HappinessTracker tracker;

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
        return tracker.getLevel();
    }

    public int getCurrentScore() {
        return tracker.getScore();
    }
}