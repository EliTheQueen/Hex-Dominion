package model.military;

import model.Constants;
import model.HexCoordinate;
import model.Unit;

public abstract class MilitaryUnit extends Unit {
    private int currentHp;
    private final int maxHp;
    private final int structureDamage;
    private final int range;

    public MilitaryUnit(HexCoordinate position,
                        Constants.UnitType unitType,
                        int maxHp,
                        int structureDamage,
                        int range
    ) {
        super(position, unitType);

        if (maxHp <= 0) {
            throw new IllegalArgumentException("maxHp <= 0");
        }
        if (structureDamage <= 0 || range <= 0) {
            throw new IllegalArgumentException("attack pewer <= 0 || range <= 0");
        }

        this.maxHp = maxHp;
        this.currentHp = maxHp;
        this.structureDamage = structureDamage;
        this.range = range;
    }

    public int getCurrentHp() {
        return currentHp;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public int getStructureDamage() {
        return structureDamage;
    }

    public int getRange() {
        return range;
    }

    public boolean isDead() {
        return currentHp <= 0;
    }

    public void decreaseHp(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount cannot be negative");
        }

        currentHp = Math.max(0, currentHp - amount);

        if (currentHp == 0) {
            kill();
        }
    }

    public boolean canAttack() {
        return canAct() && !isDead();
    }

    public void spendAttackAP() {
        if (!spendAP(1)) {
            throw new IllegalStateException("Not enough AP");
        }
    }

    public abstract MilitaryUnitType getMilitaryUnitType();

}
