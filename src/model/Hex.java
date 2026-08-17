package model;

import java.util.HashMap;
import java.util.Map;

public class Hex {

    private final HexCoordinate coordinate;
    private Map<Constants.NaturalResourceType, Integer> naturalResources = new HashMap<>();
    private Constants.TerrainType terrainType;
    private boolean isExplored = false;
    private boolean isVisible = false;
    private boolean isExpanded = false;
    private boolean hasBuilding = false;
    private boolean everHadResource = false;
    private boolean hasRoad = false;

    private int blockedTurns = 0;


    public Hex(HexCoordinate coordinate, Constants.TerrainType terrainType) {
        this.coordinate = coordinate;
        this.terrainType = terrainType;
    }

    public HexCoordinate getCoordinate() { return coordinate; }
    public Constants.TerrainType getTerrainType() { return terrainType; }
    public void setTerrainType(Constants.TerrainType terrainType) { this.terrainType = terrainType; }
    public Map<Constants.NaturalResourceType, Integer> getNaturalResources() {
        return naturalResources;
    }
    public void addNaturalResource(Constants.NaturalResourceType resourceType, int amount) {
        if (resourceType == Constants.NaturalResourceType.NONE || amount <= 0) {
            return;
        }
        this.naturalResources.put(resourceType, amount);
        this.everHadResource = true;
    }

    public boolean isDepleted() {
        return everHadResource && naturalResources.isEmpty();
    }

    public boolean everHadResource() { return everHadResource; }

    public boolean hasResource(Constants.NaturalResourceType type) {
        return naturalResources.getOrDefault(type, 0) > 0;
    }
    public int getNaturalResourceAmount(Constants.NaturalResourceType resourceType) {
        return naturalResources.getOrDefault(resourceType, 0);
    }
    public boolean getIsExplored() { return isExplored; }
    public boolean getIsExpanded() { return isExpanded; }
    public void explore(boolean isExplored) { this.isExplored = isExplored; }
    public void expand(boolean isExpanded) { this.isExpanded = isExpanded; }
    public boolean isVisible() { return isVisible; }
    public void setVisible(boolean isVisible) {
        this.isVisible = isVisible;
        if (isVisible)
        {this.isExplored = true;}
    }
    public boolean getHasBuilding() { return hasBuilding; }
    public void setHasBuilding(boolean hasBuilding) { this.hasBuilding = hasBuilding; }
    public boolean hasNaturalResource() {
        return !naturalResources.isEmpty();
    }

    public boolean isResourceDepleted(Constants.NaturalResourceType resourceType) {
        return !naturalResources.containsKey(resourceType)
                || naturalResources.get(resourceType) <= 0;
    }

    public boolean isResourcesOnHexDepleted() {
        return naturalResources.isEmpty();
    }

    public void decreaseNaturalResource(Constants.NaturalResourceType resourceType, int amount) {
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

    public boolean isBlocked() {
        return blockedTurns > 0;
    }

    public int getBlockedTurns() {
        return blockedTurns;
    }

    public void blockForTurns(int turns) {
        if (turns < 0) {
            throw new IllegalArgumentException("turns must not be negative");
        }

        blockedTurns = Math.max(blockedTurns, turns);
    }

    public void advanceBlockedTurn() {
        if (blockedTurns > 0) {
            blockedTurns--;
        }
    }

    public boolean hasRoad() {
        return hasRoad;
    }

    public void buildRoad() {
        hasRoad = true;
    }

    public void removeRoad() {
        hasRoad = false;
    }
}
