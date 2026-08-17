package model.disaster;

import model.Constants;
import model.GameMap;
import model.HexCoordinate;
import model.Unit;

public class Bear extends Unit {

    private static final int MAX_HP = 120;
    private static final int ATTACK_DAMAGE = 35;
    private static final int ATTACK_RANGE = 1;

    private final HexCoordinate den;

    public Bear(
            HexCoordinate position,
            HexCoordinate den
    ) {
        super(
                position,
                Constants.UnitType.BEAR,
                MAX_HP
        );

        if (den == null) {
            throw new IllegalArgumentException("den must not be null");
        }

        this.den = den;
    }

    public HexCoordinate getDen() {
        return den;
    }

    public int getAttackDamage() {
        return ATTACK_DAMAGE;
    }

    public int getAttackRange() {
        return ATTACK_RANGE;
    }

    public boolean canAttack(Unit target) {
        if (target == null || !target.isAlive()) {
            return false;
        }

        return getPosition().distanceTo(target.getPosition()) <= ATTACK_RANGE;
    }

    public void attack(Unit target) {
        if (!canAttack(target)) {
            throw new IllegalArgumentException("target is not in attack range");
        }

        if (!spendAP(1)) {
            throw new IllegalStateException("bear has no AP");
        }

        target.takeDamage(ATTACK_DAMAGE);
    }

    public boolean moveOneStepToward(
            Unit target,
            GameMap map
    ) {
        if (target == null || map == null) {
            return false;
        }

        HexCoordinate best = null;
        int bestDistance =
                getPosition().distanceTo(target.getPosition());

        for (model.Hex neighbour : map.getNeighboursOf(getPosition())) {

            if (!canEnter(neighbour)) {
                continue;
            }

            int distance = neighbour.getCoordinate().distanceTo(target.getPosition());

            if (distance < bestDistance) {
                bestDistance = distance;
                best = neighbour.getCoordinate();
            }
        }

        if (best == null || !spendAP(1)) {
            return false;
        }

        setPosition(best);
        return true;
    }

    private boolean canEnter(model.Hex hex) {
        switch (hex.getTerrainType()) {

            case SEA:
            case MOUNTAIN_RANGE:
                return false;

            default:
                return !hex.isBlocked();
        }
    }
}