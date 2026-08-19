package model.happiness;

public enum HappinessLevel {

    GOLDEN_AGE(3, Integer.MAX_VALUE),
    NORMAL(-2, 2),
    UNHAPPY(-4, -3),
    REVOLT(Integer.MIN_VALUE, -5);

    private final int minimumScore;
    private final int maximumScore;

    HappinessLevel(int minimumScore, int maximumScore) {
        this.minimumScore = minimumScore;
        this.maximumScore = maximumScore;
    }

    public boolean contains(int score) {
        return score >= minimumScore && score <= maximumScore;
    }

    public static HappinessLevel fromScore(int score) {
        for (HappinessLevel level : values()) {
            if (level.contains(score)) {
                return level;
            }
        }

        throw new IllegalStateException("No happiness level found for score: " + score);
    }
}