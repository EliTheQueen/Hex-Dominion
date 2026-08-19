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
    private BearActivity activity = BearActivity.HUNTING;
    private long damageRevision;

    public Bear(
            HexCoordinate position,
            HexCoordinate den
    ) {
        super(
                position,
                Constants.UnitType.BEAR,
                Constants.UNIT_AP.getOrDefault(Constants.UnitType.BEAR, 1),
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

    public boolean moveOneStepToward(
            Unit target,
            GameMap map
    ) {
        if (target == null || map == null) {
            return false;
        }

        return moveOneStepToward(target.getPosition(), map);
    }

    public boolean moveOneStepToward(
            HexCoordinate destination,
            GameMap map
    ) {
        if (destination == null || map == null) {
            return false;
        }

        HexCoordinate best = null;
        int bestDistance =
                getPosition().distanceTo(destination);

        for (model.Hex neighbour : map.getNeighboursOf(getPosition())) {

            if (!canEnter(neighbour)) {
                continue;
            }

            int distance = neighbour.getCoordinate().distanceTo(destination);

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

    @Override
    public void takeDamage(int amount) {
        int before = getCurrentHp();
        super.takeDamage(amount);
        if (getCurrentHp() < before) {
            damageRevision++;
            activity = BearActivity.DAMAGED;
        }
    }

    public BearActivity getActivity() {
        return activity;
    }

    public void setActivity(BearActivity activity) {
        if (activity == null) {
            throw new IllegalArgumentException("activity must not be null");
        }
        this.activity = activity;
    }

    public long getDamageRevision() {
        return damageRevision;
    }

    public boolean hasReturnedToDen() {
        return activity == BearActivity.RETURNED && getPosition().equals(den);
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

    public int getMaxHp() {
        return MAX_HP;
    }
}
