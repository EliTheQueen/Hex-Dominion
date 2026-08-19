package model;

public class Wall implements java.io.Serializable {

    private final int maxHp;
    private int currentHp;

    public Wall(int maxHp) {
        if (maxHp <= 0) {
            throw new IllegalArgumentException("maxHp must be positive");
        }

        this.maxHp = maxHp;
        this.currentHp = maxHp;
    }

    public void takeDamage(int damage) {
        if (damage < 0) {
            throw new IllegalArgumentException("damage must not be negative");
        }

        currentHp = Math.max(0, currentHp - damage);
    }

    public boolean isDestroyed() {
        return currentHp == 0;
    }

    public int getCurrentHp() {
        return currentHp;
    }

    public int getMaxHp() {
        return maxHp;
    }
}
