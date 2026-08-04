package model.tribe;

public final class TribeRelation {

    public static final int MIN_SCORE = -100;
    public static final int MAX_SCORE = 100;

    private int score;

    public TribeRelation() {
        this(0);
    }

    public TribeRelation(int initialScore) {
        validateScore(initialScore);
        this.score = initialScore;
    }

    public int getScore() {
        return score;
    }

    public TribeRelationStatus getStatus() {
        return TribeRelationStatus.fromScore(score);
    }

    public void increase(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException(
                    "increase amount must be greater than zero"
            );
        }

        score = Math.min(MAX_SCORE, score + amount);
    }

    public void decrease(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException(
                    "decrease amount must be greater than zero"
            );
        }

        score = Math.max(MIN_SCORE, score - amount);
    }

    public void becomeEnemy() {
        score = MIN_SCORE;
    }

    public void setScore(int score) {
        validateScore(score);
        this.score = score;
    }

    public boolean isEnemy() {
        return getStatus() == TribeRelationStatus.ENEMY;
    }

    public boolean isFriendlyOrAllied() {
        TribeRelationStatus status = getStatus();

        return status == TribeRelationStatus.FRIENDLY
                || status == TribeRelationStatus.ALLIED;
    }

    private static void validateScore(int score) {
        if (score < MIN_SCORE || score > MAX_SCORE) {
            throw new IllegalArgumentException(
                    "relation score must be between -100 and 100"
            );
        }
    }
}