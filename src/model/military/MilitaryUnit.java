package model.military;

import model.HexCoordinate;

public abstract class MilitaryUnit {
    protected HexCoordinate position;
    protected int maxAP;
    protected int currentAP;
    protected int currentHp;
    protected int maxHp;
    protected int structureDamage;
    protected int range;

    public MilitaryUnit(HexCoordinate position, int maxHp, int maxAP, int attackPower, int range) {
        if (maxHp <= 0) {
            throw new IllegalArgumentException("maxHp <= 0");
        }
        if (maxAP <= 0) {
            throw new IllegalArgumentException("maxAP <= 0");
        }
        if (attackPower <= 0 || range <= 0) {
            throw new IllegalArgumentException("attack pewer <= 0 || range <= 0");
        }
        if (position == null) {
            throw new IllegalArgumentException("position == null");
        }

        this.position = position;
        this.maxHp = maxHp;
        this.maxAP = maxAP;
        currentAP = maxAP;
        currentHp = maxHp;
        this.range = range;
        this.structureDamage = attackPower;
    }

    public HexCoordinate getPosition() {
        return position;
    }
    public void setPosition(HexCoordinate position) {
        if (position == null) {
            throw new IllegalArgumentException("position is null");
        }
        this.position = position;
    }
    public int getMaxAP() {
        return maxAP;
    }
    public int getCurrentAP() {
        return currentAP;
    }
    public void setCurrentAP(int currentAP) {
        if (currentAP < 0) {
            throw new IllegalArgumentException("currentAP <= 0");
        }
        this.currentAP = currentAP;
    }
    public int getCurrentHp() {
        return currentHp;
    }
    public int getMaxHp() {
        return maxHp;
    }
    public int getRange() {
        return range;
    }
    public int getStructureDamage() {
        return structureDamage;
    }

    public boolean isDead() {
        return currentHp <= 0;
    }

    public void decreaseHp(int num) {
        if (num < 0) {
            throw new IllegalArgumentException("num <= 0");
        }

        currentHp = Math.max(0, currentHp - num);
    }

    public void decreaseAP(int num) {
        if (num < 0) {
            throw new IllegalArgumentException("num <= 0");
        }
        currentAP =  Math.max(0, currentAP - num);
    }

    public void resetAp() {
        currentAP = maxAP;
    }

    public boolean canAttack() {
        return currentAP > 0 && !isDead();
    }

    public abstract MilitaryUnitType getUnitType();

}
