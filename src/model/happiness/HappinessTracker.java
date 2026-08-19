package model.happiness;

public class HappinessTracker implements java.io.Serializable {

    private int score;

    public HappinessTracker() {
        this(0);
    }

    public HappinessTracker(int initialScore) {
        this.score = initialScore;
    }

    public int getScore() {
        return score;
    }

    public HappinessLevel getLevel() {
        return HappinessLevel.fromScore(score);
    }

    public void increase(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount must not be negative");
        }

        score += amount;
    }

    public void decrease(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount must not be negative");
        }

        score -= amount;
    }

    //برای Eventهایی که خودشان مقدار signed دارند استفاده می‌شود.
    public void applyChange(int amount) {
        score += amount;
    }

    public void restore(int score) {
        this.score = score;
    }
}
