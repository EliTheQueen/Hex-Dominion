package model.townhall;

public class TownHall implements java.io.Serializable {
    private TownHallLevel level;
    private int currentHp;
    private final SingleCommandSlot singleCommandSlot;
    private int defense = 10;

    public TownHall() {
        level = TownHallLevel.BASE_CAMP;
        currentHp = level.getMaxHp();
        singleCommandSlot = new SingleCommandSlot();
    }

    public TownHallLevel getLevel() {
        return level;
    }
    public int getCurrentHp() {
        return currentHp;
    }
    public SingleCommandSlot getCommandSlot() {
        return singleCommandSlot;
    }
    public int getMaxHp() {
        return level.getMaxHp();
    }
    public int getStorageCapacity() {
        return level.getStorageCapacity();
    }

    public boolean canUpgrade() {
        return level.next() != null;
    }

    public void upgrade() {
        if (canUpgrade()) {
            level = level.next();
            heal(50);
        }
    }

    public void takeDamage(int damage) {
        if (damage < 0) {
            throw  new IllegalArgumentException("Invalid damage: " + damage);
        }

        currentHp = Math.max(currentHp - damage, 0);
    }

    public void heal(int heal) {
        if (heal < 0) {
            throw  new IllegalArgumentException("Invalid heal: " + heal);
        }
        currentHp = Math.min(currentHp + heal, getMaxHp());
    }

    public boolean isDestroyed() {
        return currentHp == 0;
    }
    public int getDefense() { return defense; }
    public void enableDefensiveArchitecture() { defense = 30; }
}
