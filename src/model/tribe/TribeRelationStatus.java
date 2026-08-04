package model.tribe;

public enum TribeRelationStatus {
    ENEMY,
    DISPLEASED,
    NEUTRAL,
    FRIENDLY,
    ALLIED;

    public static TribeRelationStatus fromScore(int score) {
        if (score < -100 || score > 100) {
            throw new IllegalArgumentException(
                    "relation score must be between -100 and 100"
            );
        }

        if (score <= -50) {
            return ENEMY;
        }

        if (score <= -20) {
            return DISPLEASED;
        }

        if (score <= 19) {
            return NEUTRAL;
        }

        if (score <= 69) {
            return FRIENDLY;
        }

        return ALLIED;
    }
}