package model;

import java.util.List;

public abstract class Unit implements java.io.Serializable {

    private static final int DEFAULT_MAX_HP = 100;

    protected HexCoordinate position;
    protected Constants.UnitType unitType;

    protected int maxAP;
    protected int currentAP;

    protected int maxHp;
    protected int currentHp;

    protected Constants.UnitState state;
    protected boolean alive;

    public Unit(HexCoordinate position, Constants.UnitType unitType) {
        this(
                position,
                unitType,
                getConfiguredMaxAP(unitType),
                DEFAULT_MAX_HP
        );
    }

    protected Unit(
            HexCoordinate position,
            Constants.UnitType unitType,
            int maxAP
    ) {
        this(
                position,
                unitType,
                maxAP,
                DEFAULT_MAX_HP
        );
    }

    protected Unit(
            HexCoordinate position,
            Constants.UnitType unitType,
            int maxAP,
            int maxHp
    ) {
        if (position == null) {
            throw new IllegalArgumentException("position cannot be null");
        }

        if (unitType == null) {
            throw new IllegalArgumentException("unitType cannot be null");
        }

        if (maxAP <= 0) {
            throw new IllegalArgumentException("maxAP must be positive");
        }

        if (maxHp <= 0) {
            throw new IllegalArgumentException("maxHp must be positive");
        }

        this.position = position;
        this.unitType = unitType;

        this.maxAP = maxAP;
        this.currentAP = maxAP;

        this.maxHp = maxHp;
        this.currentHp = maxHp;

        this.state = Constants.UnitState.IDLE;
        this.alive = true;
    }

    private static int getConfiguredMaxAP(Constants.UnitType unitType) {
        Integer configuredAP = Constants.UNIT_AP.get(unitType);

        if (configuredAP == null || configuredAP <= 0) {
            throw new IllegalStateException(
                    "No valid AP configured for unit type: " + unitType
            );
        }

        return configuredAP;
    }

    public HexCoordinate getPosition() {
        return position;
    }

    public void setPosition(HexCoordinate position) {
        if (position == null) {
            throw new IllegalArgumentException("position cannot be null");
        }

        this.position = position;
    }

    public Constants.UnitType getUnitType() {
        return unitType;
    }

    public int getCurrentAP() {
        return currentAP;
    }

    public int getMaxAP() {
        return maxAP;
    }

    public void resetAP() {
        if (!alive) {
            currentAP = 0;
            return;
        }

        currentAP = maxAP;
    }

    public void setCurrentAP(int ap) {
        currentAP = Math.max(0, Math.min(ap, maxAP));
    }

    public boolean spendAP(int amount) {
        if (amount <= 0) {
            return false;
        }

        if (!alive || currentAP < amount) {
            return false;
        }

        currentAP -= amount;
        return true;
    }

    public boolean canAct() {
        return alive && currentAP > 0;
    }

    public Constants.UnitState getState() {
        return state;
    }

    public void setState(Constants.UnitState state) {
        if (state == null) {
            throw new IllegalArgumentException("state cannot be null");
        }

        this.state = state;
    }

    public boolean isAlive() {
        return alive;
    }

    public int getCurrentHp() {
        return currentHp;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public void takeDamage(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("damage cannot be negative");
        }

        if (!alive || amount == 0) {
            return;
        }

        currentHp = Math.max(0, currentHp - amount);

        if (currentHp == 0) {
            kill();
        }
    }

    public void kill() {
        alive = false;
        currentHp = 0;
        currentAP = 0;
    }

    public int getVisionRadius() {
        return Constants.UNIT_VISION.getOrDefault(unitType, 1);
    }

    public boolean canMoveTo(GameMap map, HexCoordinate destination) {
        return canMoveTo(map, destination, MovementPolicy.basic());
    }

    public boolean canMoveTo(GameMap map, HexCoordinate destination, MovementPolicy policy) {
        if (map == null || destination == null || destination.equals(position)) {
            return false;
        }

        if (!map.containsCoordinate(destination)) {
            return false;
        }

        List<HexCoordinate> path = PathFinder.findPath(
                map,
                position,
                destination,
                currentAP, policy
        );

        return path != null && path.size() > 1;
    }

    public boolean moveTo(GameMap map, HexCoordinate destination) {
        return moveTo(map, destination, MovementPolicy.basic());
    }

    public boolean moveTo(GameMap map, HexCoordinate destination, MovementPolicy policy) {
        if (!alive || map == null || destination == null || destination.equals(position)) {
            return false;
        }

        List<HexCoordinate> path = PathFinder.findPath(
                map,
                position,
                destination,
                currentAP, policy
        );

        if (path == null || path.size() < 2) {
            return false;
        }

        int cost = PathFinder.pathCost(map, position, destination, currentAP, policy);

        if (cost > currentAP) {
            return false;
        }

        if (!spendAP(cost)) {
            return false;
        }

        boolean enteredWater = policy.enteringWater(map, position, destination);
        position = destination;
        if (enteredWater) currentAP = 0;
        state = Constants.UnitState.MOVING;

        return true;
    }
}
