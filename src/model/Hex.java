package model;

public class Hex {

    private final HexCoordinate coordinate;
    private final GameState.TerrainType terrainType;
    private GameState.NaturalResourceType naturalResourceType;
    private int naturalResourceAmount;
    private boolean isExplored = false;
    private boolean isVisible = false;
    private boolean isExpanded = false;
    private boolean hasBuilding = false;


    public Hex(HexCoordinate coordinate, GameState.TerrainType terrainType, GameState.NaturalResourceType naturalResourceType, int naturalResourceAmount) {
        this.coordinate = coordinate;
        this.terrainType = terrainType;
        this.naturalResourceType = naturalResourceType;
        this.naturalResourceAmount = naturalResourceAmount;
    };

    public HexCoordinate getCoordinate() { return coordinate; }
    public GameState.TerrainType getTerrainType() { return terrainType; }
    public GameState.NaturalResourceType getNaturalResourceType() { return naturalResourceType; }
    public void setNaturalResourceType(GameState.NaturalResourceType naturalResourceType) { this.naturalResourceType = naturalResourceType; }
    public int getNaturalResourceAmount() { return naturalResourceAmount; }
    public void setNaturalResourceAmount(int naturalResourceAmount) {}
    public boolean getIsExplored() { return isExplored; }
    public boolean getIsExpanded() { return isExpanded; }
    public void explore(boolean isExplored) { this.isExplored = isExplored; }
    public void expand(boolean isExpanded) { this.isExpanded = isExpanded; }
    public boolean isVisible() { return isVisible; }
    public void setVisible(boolean isVisible) { this.isVisible = isVisible;
    if (isVisible) {this.isExplored = true;} }
    public boolean getHasBuilding() { return hasBuilding; }
    public void setHasBuilding(boolean hasBuilding) { this.hasBuilding = hasBuilding; }
    public boolean hasNaturalResource() { return naturalResourceType != GameState.NaturalResourceType.NONE && naturalResourceAmount > 0; }

    public boolean isResourceDepleted() {
        if (naturalResourceType != naturalResourceType.NONE && naturalResourceAmount == 0) {
            return true;
        }

        return false;

    }

    public void decreaseNaturalResource(int amount) {
        if (amount <= 0 || naturalResourceType == naturalResourceType.NONE) {
            return;
        }

        naturalResourceAmount -= amount;

        if (naturalResourceAmount <= 0) {
            naturalResourceAmount = 0;
        }
    }

}
