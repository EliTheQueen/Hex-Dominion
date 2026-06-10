package model;

import java.util.HashMap;
import java.util.Map;

public class Hex {

    private final HexCoordinate coordinate;
    private Map<GameState.NaturalResourceType, Integer> naturalResources = new HashMap<>();
    private GameState.TerrainType terrainType;
    private boolean isExplored = false;
    private boolean isVisible = false;
    private boolean isExpanded = false;
    private boolean hasBuilding = false;


    public Hex(HexCoordinate coordinate, GameState.TerrainType terrainType) {
        this.coordinate = coordinate;
        this.terrainType = terrainType;
    };

    public HexCoordinate getCoordinate() { return coordinate; }
    public GameState.TerrainType getTerrainType() { return terrainType; }
    public void setTerrainType(GameState.TerrainType terrainType) { this.terrainType = terrainType; }
    public Map<GameState.NaturalResourceType, Integer> getNaturalResources() {
        return naturalResources;
    }
    public void addNaturalResource(GameState.NaturalResourceType resourceType, int amount) {
        if (resourceType == GameState.NaturalResourceType.NONE || amount <= 0) {
            return;
        }
        this.naturalResources.put(resourceType, amount);
    }
    public int getNaturalResourceAmount(GameState.NaturalResourceType resourceType) {
        return naturalResources.getOrDefault(resourceType, 0);
    }
    public boolean getIsExplored() { return isExplored; }
    public boolean getIsExpanded() { return isExpanded; }
    public void explore(boolean isExplored) { this.isExplored = isExplored; }
    public void expand(boolean isExpanded) { this.isExpanded = isExpanded; }
    public boolean isVisible() { return isVisible; }
    public void setVisible(boolean isVisible) { this.isVisible = isVisible;
    if (isVisible) {this.isExplored = true;} }
    public boolean getHasBuilding() { return hasBuilding; }
    public void setHasBuilding(boolean hasBuilding) { this.hasBuilding = hasBuilding; }
    public boolean hasNaturalResource() {
        return !naturalResources.isEmpty();
    }

    public boolean isResourceDepleted(GameState.NaturalResourceType resourceType) {
        return !naturalResources.containsKey(resourceType)
                || naturalResources.get(resourceType) <= 0;
    }

    public boolean isResourcesOnHexDepleted() {
        return naturalResources.isEmpty();
    }

    public void decreaseNaturalResource(GameState.NaturalResourceType resourceType, int amount) {
        if (amount <= 0 || !naturalResources.containsKey(resourceType)) {
            return;
        }

        int currentAmount = naturalResources.get(resourceType);
        int newAmount = currentAmount - amount;

        if (newAmount <= 0) {
            naturalResources.remove(resourceType);
        } else {
            naturalResources.put(resourceType, newAmount);
        }
    }

    public boolean canHoldBuilding() {
       if (getHasBuilding()) {
           return false;
       }
       return true;
    }

}
