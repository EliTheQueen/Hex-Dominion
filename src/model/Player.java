package model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static model.Constants.BuildingType;
import static model.Constants.INITIAL_FOOD;
import static model.Constants.INITIAL_STORAGE;
import static model.Constants.INITIAL_WOOD;
import static model.Constants.INITIAL_IRON;
import static model.Constants.TechnologyType;

public class Player {
    private final String name;
    private final ResourceStorage resources;
    private final List<Unit> units;
    private final List<Building> buildings;
    private final Set<HexCoordinate> territory;
    private final Set<TechnologyType> researched;

    public Player(String name) {
        this.name = name;
        this.resources = new ResourceStorage(INITIAL_STORAGE);
        this.units = new ArrayList<>();
        this.buildings = new ArrayList<>();
        this.territory = new HashSet<>();
        this.researched = new HashSet<>();

        resources.addResources(ResourceAmount.of(INITIAL_FOOD, INITIAL_WOOD, 0, INITIAL_IRON));
    }

    public String getName() { return name; }
    public ResourceStorage getResources() { return resources; }
    public List<Unit> getUnits() { return units; }
    public List<Building> getBuildings() { return buildings; }
    public Set<HexCoordinate> getTerritory() { return territory; }
    public Set<TechnologyType> getResearched() { return researched; }

    public void addUnit(Unit u) { units.add(u); }
    public void removeUnit(Unit u) { units.remove(u); }
    public void addBuilding(Building b) { buildings.add(b); }
    public void removeBuilding(Building b) { buildings.remove(b); }

    public void expandTerritory(HexCoordinate coord) { territory.add(coord); }
    public boolean isInTerritory(HexCoordinate coord) { return territory.contains(coord); }

    public boolean hasResearched(TechnologyType tech) { return researched.contains(tech); }

    public ResourceAmount getTechCost(TechnologyType tech) {
        switch (tech) {
            case STORAGE_I:          return ResourceAmount.of(0, 20, 10, 0);
            case STORAGE_II:         return ResourceAmount.of(0, 30, 20, 0);
            case STONE_MINING:       return ResourceAmount.of(0, 15, 0, 0);
            case IRON_MINING:        return ResourceAmount.of(0, 20, 15, 0);
            case PROFESSIONAL_TOOLS: return ResourceAmount.of(0, 20, 0, 10);
            case TOWNSHIP:           return ResourceAmount.of(0, 25, 25, 15);
            default:                 return ResourceAmount.zero();
        }
    }

    public boolean canResearch(TechnologyType tech) {
        if (researched.contains(tech)) return false;
        if (tech == TechnologyType.STORAGE_II && !researched.contains(TechnologyType.STORAGE_I)) return false;
        if (tech == TechnologyType.IRON_MINING && !researched.contains(TechnologyType.STONE_MINING)) return false;
        return resources.canAfford(getTechCost(tech));
    }

    public boolean research(TechnologyType tech) {
        if (!canResearch(tech)) return false;
        resources.spend(getTechCost(tech));
        researched.add(tech);
        if (tech == TechnologyType.STORAGE_I) {
            resources.upgradeCapacity(ResourceAmount.of(50, 50, 50, 50));
        } else if (tech == TechnologyType.STORAGE_II) {
            resources.upgradeCapacity(ResourceAmount.of(100, 100, 100, 100));
        }
        return true;
    }

    public boolean canAfford(ResourceAmount cost) { return resources.canAfford(cost); }
    public boolean spend(ResourceAmount cost) { return resources.spend(cost); }
    public void addResources(ResourceAmount amount) { resources.addResources(amount); }

    public int getUnitCount() {
        int count = 0;
        for (Unit u : units) if (u.isAlive()) count++;
        return count;
    }

    public int getBuildingCount() { return buildings.size(); }
    public int getTerritorySize() { return territory.size(); }
    public boolean hasProfessionalTools() { return researched.contains(TechnologyType.PROFESSIONAL_TOOLS); }

    public Unit getUnitAt(HexCoordinate coord) {
        for (Unit u : units) {
            if (u.isAlive() && u.getPosition().equals(coord)) return u;
        }
        return null;
    }

    public Building getBuildingAt(HexCoordinate coord) {
        for (Building b : buildings) {
            if (b.getPosition().equals(coord)) return b;
        }
        return null;
    }

    public boolean canBuildAt(HexCoordinate coord, BuildingType type) {
        if (!isInTerritory(coord)) return false;
        if (getBuildingAt(coord) != null) return false;
        if (type == BuildingType.STONE_MINE && !hasResearched(TechnologyType.STONE_MINING)) return false;
        if (type == BuildingType.IRON_MINE && !hasResearched(TechnologyType.IRON_MINING)) return false;
        if (type == BuildingType.TOWNSHIP && !hasResearched(TechnologyType.TOWNSHIP)) return false;
        return true;
    }

    public void removeDeadUnits() {
        units.removeIf(u -> !u.isAlive());
    }
}
