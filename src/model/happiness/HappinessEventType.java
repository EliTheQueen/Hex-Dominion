package model.happiness;

public enum HappinessEventType {

    TOWNSHIP_BUILT(-1),
    UNIT_CAP_REACHED(-1),
    MONUMENT_ACTIVATED(2),
    TOWN_HALL_GARRISONED(1),
    FRIENDLY_TRIBE_ATTACKED(-5),
    ALLIED_TRIBE_ATTACKED(-15),
    MISSION_CANCELLED(-5),
    MISSION_FAILED(-10);

    private final int scoreChange;

    HappinessEventType(int scoreChange) {
        this.scoreChange = scoreChange;
    }

    public int getScoreChange() {
        return scoreChange;
    }
}