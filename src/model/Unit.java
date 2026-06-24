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

    public boolean canMoveTo(GameMap map, HexCoordinate dest) {
        if (!map.containsCoordinate(dest)) return false;
        Hex destHex = map.getHex(dest);
        if (destHex == null) return false;
        int cost = Constants.MOVE_COST.getOrDefault(destHex.getTerrainType(), 1);
        return currentAP >= cost;
    }

    public boolean moveTo(GameMap map, HexCoordinate dest) {
        if (!canMoveTo(map, dest)) return false;
        Hex destHex = map.getHex(dest);
        int cost = Constants.MOVE_COST.getOrDefault(destHex.getTerrainType(), 1);
        spendAP(cost);
        position = dest;
        state = Constants.UnitState.MOVING;
        return true;
    }
}
