package model;

public abstract class Unit {
    protected HexCoordinate position;
    protected Constants.UnitType unitType;
    protected int maxAP;
    protected int currentAP;
    protected Constants.UnitState state;
    protected boolean alive;

    public Unit(HexCoordinate position, Constants.UnitType unitType) {
        this.position = position;
        this.unitType = unitType;
        this.maxAP = Constants.UNIT_AP.get(unitType);
        this.currentAP = maxAP;
        this.state = Constants.UnitState.IDLE;
        this.alive = true;
    }

    public HexCoordinate getPosition() { return position; }
    public void setPosition(HexCoordinate pos) { this.position = pos; }
    public Constants.UnitType getUnitType() { return unitType; }
    public int getCurrentAP() { return currentAP; }
    public int getMaxAP() { return maxAP; }
    public void resetAP() { currentAP = maxAP; }
    public boolean spendAP(int amount) {
        if (currentAP < amount) return false;
        currentAP -= amount;
        return true;
    }
    public boolean canAct() { return alive && currentAP > 0; }
    public Constants.UnitState getState() { return state; }
    public void setState(Constants.UnitState state) { this.state = state; }
    public boolean isAlive() { return alive; }
    public void kill() { alive = false; }
    public int getVisionRadius() { return Constants.UNIT_VISION.getOrDefault(unitType, 1); }

    /** Whether the unit can reach {@code dest} this turn following a valid path within its AP. */
    public boolean canMoveTo(GameMap map, HexCoordinate dest) {
        if (dest == null || dest.equals(position)) return false;
        if (!map.containsCoordinate(dest)) return false;
        java.util.List<HexCoordinate> path = PathFinder.findPath(map, position, dest, currentAP);
        return path != null && path.size() > 1;
    }

    /** Total terrain cost of walking the given path (excluding the starting hex). */
    private int pathCost(GameMap map, java.util.List<HexCoordinate> path) {
        int cost = 0;
        for (int i = 1; i < path.size(); i++) {
            Hex h = map.getHex(path.get(i));
            cost += Constants.MOVE_COST.getOrDefault(h.getTerrainType(), 1);
        }
        return cost;
    }

    public boolean moveTo(GameMap map, HexCoordinate dest) {
        if (dest == null || dest.equals(position)) return false;
        java.util.List<HexCoordinate> path = PathFinder.findPath(map, position, dest, currentAP);
        if (path == null || path.size() < 2) return false;
        int cost = pathCost(map, path);
        if (cost > currentAP) return false;
        spendAP(cost);
        position = dest;
        state = Constants.UnitState.MOVING;
        return true;
    }
}
