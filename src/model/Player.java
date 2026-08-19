package model;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static model.Constants.BuildingType;
import static model.Constants.INITIAL_FOOD;
import static model.Constants.INITIAL_STORAGE;
import static model.Constants.INITIAL_WOOD;
import static model.Constants.INITIAL_STONE;
import static model.Constants.INITIAL_IRON;
import static model.Constants.TechnologyType;
import static model.Constants.UnitType;

public class Player implements java.io.Serializable {
    private final String name;
    private final ResourceStorage resources;
    private final List<Unit> units;
    private final List<Building> buildings;
    private final Set<HexCoordinate> territory;
    private final Set<TechnologyType> researched; //مجموعه‌ی تکنولوژی‌های تمام‌شده.
    private final Set<TechnologyType> queuedTech; //تکنولوژی‌هایی که در صف هستند
    private final ProductionQueue productionQueue;

    public Player(String name) {
        this.name = name;
        this.resources = new ResourceStorage(INITIAL_STORAGE);
        this.units = new ArrayList<>();
        this.buildings = new ArrayList<>();
        this.territory = new HashSet<>();
        this.researched = new HashSet<>();
        this.queuedTech = new HashSet<>();
        this.productionQueue = new ProductionQueue();

        resources.addResources(ResourceAmount.of(INITIAL_FOOD, INITIAL_WOOD, INITIAL_STONE, INITIAL_IRON));
    }

    public String getName() {
        return name;
    }

    public ResourceStorage getResources() {
        return resources;
    }

    public List<Unit> getUnits() {
        return units;
    }

    public List<Building> getBuildings() {
        return buildings;
    }

    public Set<HexCoordinate> getTerritory() {
        return territory;
    }

    public Set<TechnologyType> getResearched() {
        return researched;
    }

    public ProductionQueue getProductionQueue() {
        return productionQueue;
    }

    public void addUnit(Unit u) {
        units.add(u);
    }

    public void removeUnit(Unit u) {
        units.remove(u);
    }

    public void addBuilding(Building b) {
        buildings.add(b);
    }

    /**
     * Authoritative building destruction lifecycle.  All gameplay systems that
     * permanently destroy a building must use this method rather than merely
     * marking the Building as ruined.
     */
    public boolean destroyBuilding(GameMap map, Building building) {
        if (map == null || building == null || !buildings.contains(building)) {
            return false;
        }

        building.ruin(); // releases every stationed worker safely
        buildings.remove(building); // invalidates production, upkeep and effects

        Hex hex = map.getHex(building.getPosition());
        if (hex != null) {
            hex.setHasBuilding(false);
        }
        return true;
    }

    public boolean destroyBuilding(GameMap map, HexCoordinate coordinate) {
        return destroyBuilding(map, getBuildingAt(coordinate));
    }

    public void expandTerritory(HexCoordinate coord) {
        territory.add(coord);
    }

    public boolean isInTerritory(HexCoordinate coord) {
        return territory.contains(coord);
    }

    public boolean hasResearched(TechnologyType tech) {
        return researched.contains(tech);
    }

    public boolean isTechQueued(TechnologyType tech) {
        return queuedTech.contains(tech);
    }

    public ResourceAmount getTechCost(TechnologyType tech) {
        switch (tech) {
            case STORAGE_I:
                return ResourceAmount.of(0, 20, 10, 0);
            case STORAGE_II:
                return ResourceAmount.of(0, 30, 20, 0);
            case STONE_MINING:
                return ResourceAmount.of(0, 15, 0, 0);
            case IRON_MINING:
                return ResourceAmount.of(0, 20, 15, 0);
            case PROFESSIONAL_TOOLS:
                return ResourceAmount.of(0, 20, 0, 10);
            case TOWNSHIP:
                return ResourceAmount.of(0, 25, 25, 15);
            default:
                return ResourceAmount.zero();
        }
    }

    //آیا پیش‌نیازهای این تکنولوژی فراهم است؟
    /*
        STORAGE_I → STORAGE_II
        STONE_MINING → IRON_MINING → PROFESSIONAL_TOOLS
        TOWNSHIP (مستقل)
     */
    public boolean prerequisitesMet(TechnologyType tech) {
        if (researched.contains(tech) || queuedTech.contains(tech)) return false;
        switch (tech) {
            case STORAGE_II:
                return researched.contains(TechnologyType.STORAGE_I);
            case IRON_MINING:
                return researched.contains(TechnologyType.STONE_MINING);
            case PROFESSIONAL_TOOLS:
                return researched.contains(TechnologyType.IRON_MINING);
            default:
                return true;
        }
    }

    //آیا الان می‌توان این تکنولوژی را تحقیق کرد؟
    // دو شرط: پیش‌نیاز فراهم باشد و پولش را داشته باشیم. ترکیبِ تمیزِ دو متدِ قبلی.
    public boolean canResearch(TechnologyType tech) {
        return prerequisitesMet(tech) && resources.canAfford(getTechCost(tech));
    }

    public boolean queueTech(TechnologyType tech) {
        if (!canResearch(tech)) return false;
        resources.spend(getTechCost(tech));
        queuedTech.add(tech);
        //به صفِ تولید اضافه می‌شود تا در نوبت‌های بعد پیش برود.
        productionQueue.enqueue(ProductionTask.forTech(tech));
        return true;
    }

    public boolean markTechQueued(TechnologyType tech) {
        if (!prerequisitesMet(tech)) return false;
        queuedTech.add(tech); return true;
    }

    public void cancelQueuedTech(TechnologyType tech) { queuedTech.remove(tech); }

    public void applyTech(TechnologyType tech) {
        queuedTech.remove(tech);
        researched.add(tech);
        if (tech == TechnologyType.STORAGE_I) {
            resources.upgradeCapacity(ResourceAmount.of(50, 50, 50, 50));
        } else if (tech == TechnologyType.STORAGE_II) {
            resources.upgradeCapacity(ResourceAmount.of(100, 100, 100, 100));
        }
    }

    public boolean canAfford(ResourceAmount cost) {
        return resources.canAfford(cost);
    }

    public boolean spend(ResourceAmount cost) {
        return resources.spend(cost);
    }

    public void addResources(ResourceAmount amount) {
        resources.addResources(amount);
    }

    public int getUnitCount() {
        int count = 0;
        for (Unit u : units) if (u.isAlive()) count++;
        return count;
    }

    /**
     * Living units in the queue count toward the cap too, so the player can't over-queue.
     */
    public int getEffectiveUnitCount() {
        int queued = 0;
        for (ProductionTask t : productionQueue.getTasks()) {
            if (t.getKind() == ProductionTask.Kind.UNIT) queued++;
        }
        return getUnitCount() + queued;
    }

    public int getTownshipCount() {
        int n = 0;
        for (Building b : buildings) {
            if (b.getType() == BuildingType.TOWNSHIP && b.isActive()) n++;
        }
        return n;
    }

    public int getUnitCap() {
        return Constants.INITIAL_CAP + getTownshipCount() * Constants.CAP_PER_TOWNSHIP;
    }

    public boolean atUnitCap() {
        return getEffectiveUnitCount() >= getUnitCap();
    }

    public Map<UnitType, Integer> countUnitsByType() {
        Map<UnitType, Integer> counts = new EnumMap<>(UnitType.class);
        for (UnitType t : UnitType.values()) counts.put(t, 0);
        for (Unit u : units) {
            if (u.isAlive()) counts.put(u.getUnitType(), counts.get(u.getUnitType()) + 1);
        }
        return counts;
    }

    public int getBuildingCount() {
        return buildings.size();
    }

    public int getActiveBuildingCount() {
        int n = 0;
        for (Building b : buildings) if (b.isActive()) n++;
        return n;
    }

    public int getTerritorySize() {
        return territory.size();
    }

    public boolean hasProfessionalTools() {
        return researched.contains(TechnologyType.PROFESSIONAL_TOOLS);
    }

    public Unit getUnitAt(HexCoordinate coord) {
        for (Unit u : units) {
            if (u.isAlive() && u.getPosition().equals(coord)) return u;
        }
        return null;
    }

    public Building getBuildingAt(HexCoordinate coord) {
        for (Building b : buildings) {
            if (b.isActive() && b.getPosition().equals(coord)) return b;
        }
        return null;
    }

    public void removeDeadUnits() {
        units.removeIf(u -> !u.isAlive());
    }

    public boolean canStore(ResourceAmount cost) {
        return resources.canStore(cost);
    }

    public boolean hasBuildingType(Constants.BuildingType type) {
        for (Building building : buildings) {
            if (building.getType() == type && building.isActive()) {
                return true;
            }
        }

        return false;
    }
}
