package model;

import java.util.EnumMap;
import java.util.Map;

public final class Constants {

    public enum TerrainType {
        FOREST,
        MOUNTAIN,
        MOUNTAIN_RANGE,
        VOLCANO,
        PLAIN,
        GRASSLAND,
        SEA
    }
    public enum ResourceType { FOOD, WOOD, STONE, IRON }
    public enum NaturalResourceType {
        NONE,
        WOOD,
        STONE,
        IRON,
        WHEAT,
        RICE,
        COW,
        SHEEP,
        FISH
    }
    public enum UnitType {
        EXPLORER,
        BUILDER,
        WORKER,
        BORDER_EXPANDER,
        MILITARY,
        BEAR
    }
    public enum BuildingType {
        TOWN_HALL,
        LUMBER_MILL,
        STONE_MINE,
        IRON_MINE,
        FARM,
        STABLE,
        TOWNSHIP,
        DOCK,
        MONUMENT,
        BAZAAR
    }
    public enum TechnologyType { STORAGE_I, STORAGE_II, STONE_MINING, IRON_MINING, PROFESSIONAL_TOOLS, TOWNSHIP }
    public enum UnitState { IDLE, MOVING, STATIONED, AUTO_EXPLORE }

    private Constants() {}

    public static final int MAP_RADIUS = 7;
    public static final int INITIAL_CAP = 6;
    public static final int CAP_PER_TOWNSHIP = 4;
    public static final int INITIAL_STORAGE = 100;
    public static final int INITIAL_FOOD = 40, INITIAL_WOOD = 30, INITIAL_STONE = 10, INITIAL_IRON = 0;
    public static final int BUILDER_CHARGES = 3;
    public static final int WORKER_STATION_AP_COST = 1;
    public static final int FOOD_PER_UNIT = 1;

    public static final int TOWN_HALL_FOOD = 1;
    public static final int TOWN_HALL_WOOD = 1;

    public static final int UPKEEP_GRACE_TURNS = 3;

    public static final double STARVATION_AP_FACTOR = 0.5;

    public static final int DEPLETION_PER_PRODUCTION = 1;

    public static final int BUILDING_VISION = 2;

    public static final Map<TerrainType,Integer> MOVE_COST = new EnumMap<>(TerrainType.class);
    public static final Map<UnitType,Integer> UNIT_AP = new EnumMap<>(UnitType.class);
    public static final Map<UnitType,Integer> UNIT_VISION = new EnumMap<>(UnitType.class);
    public static final Map<BuildingType,Integer> BUILD_AP = new EnumMap<>(BuildingType.class);
    public static final Map<BuildingType,Integer> WORKER_CAP = new EnumMap<>(BuildingType.class);
    public static final Map<BuildingType,ResourceType> PRODUCES = new EnumMap<>(BuildingType.class);
    public static final Map<BuildingType,Integer> BASE_RATE = new EnumMap<>(BuildingType.class);
    public static final Map<BuildingType,ResourceAmount> BUILD_COST = new EnumMap<>(BuildingType.class);
    public static final Map<BuildingType,ResourceAmount> UPKEEP = new EnumMap<>(BuildingType.class);
    public static final Map<UnitType,ResourceAmount> UNIT_COST = new EnumMap<>(UnitType.class);
    public static final Map<UnitType,Integer> UNIT_BUILD_TURNS = new EnumMap<>(UnitType.class);

    static {
        MOVE_COST.put(TerrainType.PLAIN,1);
        MOVE_COST.put(TerrainType.GRASSLAND,1);
        MOVE_COST.put(TerrainType.FOREST,2);
        MOVE_COST.put(TerrainType.MOUNTAIN,4);
        MOVE_COST.put(TerrainType.VOLCANO, 4);
        UNIT_AP.put(UnitType.EXPLORER,6);
        UNIT_AP.put(UnitType.BUILDER,4);
        UNIT_AP.put(UnitType.WORKER,3);
        UNIT_AP.put(UnitType.BORDER_EXPANDER,3);
        UNIT_AP.put(UnitType.BEAR, 2);
        UNIT_VISION.put(UnitType.EXPLORER,3);
        UNIT_VISION.put(UnitType.BUILDER,2);
        UNIT_VISION.put(UnitType.WORKER,1);
        UNIT_VISION.put(UnitType.BORDER_EXPANDER,2);
        UNIT_VISION.put(UnitType.BEAR, 3);
        for (BuildingType b: BuildingType.values()) {
            BUILD_AP.put(b,1);
            WORKER_CAP.put(b,0);
            UPKEEP.put(b, ResourceAmount.of(0,0,0,0));
        }
        BUILD_AP.put(BuildingType.TOWNSHIP,2);
        BUILD_AP.put(BuildingType.DOCK, 2);
        BUILD_AP.put(BuildingType.MONUMENT, 3);
        BUILD_AP.put(BuildingType.BAZAAR, 2);
        WORKER_CAP.put(BuildingType.LUMBER_MILL,2);
        WORKER_CAP.put(BuildingType.STONE_MINE,2);
        WORKER_CAP.put(BuildingType.IRON_MINE,2);
        WORKER_CAP.put(BuildingType.FARM,3);
        WORKER_CAP.put(BuildingType.STABLE,2);
        WORKER_CAP.put(BuildingType.DOCK, 2);
        WORKER_CAP.put(BuildingType.MONUMENT, 0);
        WORKER_CAP.put(BuildingType.BAZAAR, 0);
        PRODUCES.put(BuildingType.LUMBER_MILL, ResourceType.WOOD);
        PRODUCES.put(BuildingType.STONE_MINE, ResourceType.STONE);
        PRODUCES.put(BuildingType.IRON_MINE, ResourceType.IRON);
        PRODUCES.put(BuildingType.FARM, ResourceType.FOOD);
        PRODUCES.put(BuildingType.STABLE, ResourceType.FOOD);
        BASE_RATE.put(BuildingType.LUMBER_MILL,5);
        BASE_RATE.put(BuildingType.STONE_MINE,4);
        BASE_RATE.put(BuildingType.IRON_MINE,3);
        BASE_RATE.put(BuildingType.FARM,6);
        BASE_RATE.put(BuildingType.STABLE,5);
        BUILD_COST.put(BuildingType.LUMBER_MILL, ResourceAmount.of(0,8,0,0));
        BUILD_COST.put(BuildingType.FARM, ResourceAmount.of(0,6,0,0));
        BUILD_COST.put(BuildingType.STABLE, ResourceAmount.of(0,10,0,0));
        BUILD_COST.put(BuildingType.STONE_MINE, ResourceAmount.of(0,12,0,0));
        BUILD_COST.put(BuildingType.IRON_MINE, ResourceAmount.of(0,16,8,0));
        BUILD_COST.put(BuildingType.TOWNSHIP, ResourceAmount.of(0,25,25,8));
        BUILD_COST.put(BuildingType.TOWN_HALL, ResourceAmount.zero());
        BUILD_COST.put(BuildingType.DOCK, ResourceAmount.of(0, 20, 10, 0));
        BUILD_COST.put(BuildingType.MONUMENT, ResourceAmount.of(0, 30, 40, 20));
        BUILD_COST.put(BuildingType.BAZAAR, ResourceAmount.of(0, 20, 15, 5));
        UPKEEP.put(BuildingType.LUMBER_MILL, ResourceAmount.of(0,1,0,0));
        UPKEEP.put(BuildingType.FARM, ResourceAmount.of(0,1,0,0));
        UPKEEP.put(BuildingType.STABLE, ResourceAmount.of(0,1,0,0));
        UPKEEP.put(BuildingType.STONE_MINE, ResourceAmount.of(0,1,1,0));
        UPKEEP.put(BuildingType.IRON_MINE, ResourceAmount.of(0,1,1,0));
        UPKEEP.put(BuildingType.TOWNSHIP, ResourceAmount.of(1,1,1,0));
        UPKEEP.put(BuildingType.DOCK, ResourceAmount.of(0, 1, 0, 0));
        UPKEEP.put(BuildingType.MONUMENT, ResourceAmount.of(1, 0, 0, 0));
        UPKEEP.put(BuildingType.BAZAAR, ResourceAmount.of(0, 1, 0, 0));
        UNIT_COST.put(UnitType.EXPLORER, ResourceAmount.of(8,8,0,0));
        UNIT_COST.put(UnitType.BUILDER, ResourceAmount.of(10,12,0,0));
        UNIT_COST.put(UnitType.WORKER, ResourceAmount.of(8,8,0,0));
        UNIT_COST.put(UnitType.BORDER_EXPANDER, ResourceAmount.of(12,20,8,0));
        UNIT_BUILD_TURNS.put(UnitType.EXPLORER,2);
        UNIT_BUILD_TURNS.put(UnitType.BUILDER,3);
        UNIT_BUILD_TURNS.put(UnitType.WORKER,2);
        UNIT_BUILD_TURNS.put(UnitType.BORDER_EXPANDER,3);
    }
}
