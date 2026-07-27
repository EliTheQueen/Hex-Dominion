package model.townhall;

public enum TownHallLevel {

    BASE_CAMP(1, 100, 200),
    SETTLEMENT(2, 200, 250),
    CAPITAL(3, 350, 350);

    private final int levelNumber;
    private final int storageCapacity;
    private final int maxHp;

    TownHallLevel(int levelNumber, int storageCapacity, int maxHp) {
        this.levelNumber = levelNumber;
        this.storageCapacity = storageCapacity;
        this.maxHp = maxHp;
    }

    public int getLevelNumber() {
        return levelNumber;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public int getStorageCapacity() {
        return storageCapacity;
    }

    public TownHallLevel next() {
        switch (this) {
            case BASE_CAMP:
                return SETTLEMENT;

            case SETTLEMENT:
                return CAPITAL;

            case CAPITAL:
                return null;
        }
        throw  new RuntimeException("Unknown Town Hall level: " + this);
    }
}
