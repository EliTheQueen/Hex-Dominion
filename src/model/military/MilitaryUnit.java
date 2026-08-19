package model.military;

import model.Constants;
import model.HexCoordinate;
import model.Unit;

public abstract class MilitaryUnit extends Unit {
    private static final long serialVersionUID = 763958483051858437L;

    private final int structureDamage;
    private final int range;

    public MilitaryUnit(
            HexCoordinate position,
            int maxHp,
            int structureDamage,
            int range,
            int maxAP
    ) {
        super(
                position,
                Constants.UnitType.MILITARY,
                maxAP,
                maxHp
        );

        if (structureDamage <= 0) {
            throw new IllegalArgumentException(
                    "structureDamage must be positive"
            );
        }

        if (range <= 0) {
            throw new IllegalArgumentException(
                    "range must be positive"
            );
        }

        this.structureDamage = structureDamage;
        this.range = range;
    }

    public int getStructureDamage() {
        return structureDamage;
    }

    public int getRange() {
        return range;
    }

    public boolean isDead() {
        return !isAlive();
    }

    public boolean canAttack() {
        return canAct() && !isDead();
    }

    public void spendAttackAP() {
        if (!spendAP(1)) {
            throw new IllegalStateException(
                    "Not enough AP to attack"
            );
        }
    }

    public boolean contributesCombatDie() {
        return getMilitaryUnitType() != MilitaryUnitType.CATAPULT;
    }

    public abstract MilitaryUnitType getMilitaryUnitType();
}
