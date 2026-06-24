package model;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public final class Constants {

    public enum TerrainType { PLAINS, FOREST, MOUNTAIN, GRASSLAND }
    public enum ResourceType { FOOD, WOOD, STONE, IRON }
    public enum NaturalResourceType { NONE, WOOD, STONE, IRON, WHEAT, RICE, COW, SHEEP }
    public enum UnitType { EXPLORER, BUILDER, WORKER, BORDER_EXPANDER }
    public enum BuildingType { TOWN_HALL, LUMBER_MILL, STONE_MINE, IRON_MINE, FARM, STABLE, TOWNSHIP }
    public enum TechnologyType { STORAGE_I, STORAGE_II, STONE_MINING, IRON_MINING, PROFESSIONAL_TOOLS, TOWNSHIP }
    public enum UnitState { IDLE, MOVING, STATIONED, AUTO_EXPLORE }

    private Constants() {
    }

    public static final int MAP_RADIUS = 7;
    public static final int INITIAL_CAP = 10;
    public static final int INITIAL_STORAGE = 100;
    public static final int INITIAL_FOOD = 40, INITIAL_WOOD = 30, INITIAL_IRON = 0;
    public static final int BUILDER_CHARGES = 3;
    public static final int WORKER_STATION_AP_COST = 1;
    public static final int FOOD_PER_UNIT = 1;

    public static final Map<TerrainType,Integer> MOVE_COST = new EnumMap<>(TerrainType.class);
    public static final Map<UnitType,Integer> UNIT_AP = new EnumMap<>(UnitType.class);
    public static final Map<UnitType,Integer> UNIT_VISION = new EnumMap<>(UnitType.class);
    public static final Map<BuildingType,Integer> BUILD_AP = new EnumMap<>(BuildingType.class);
    public static final Map<BuildingType,Integer> WORKER_CAP = new EnumMap<>(BuildingType.class);
    public static final Map<BuildingType,ResourceType> PRODUCES = new EnumMap<>(BuildingType.class);
    public static final Map<BuildingType,Integer> BASE_RATE = new EnumMap<>(BuildingType.class);
    public static final Map<BuildingType,ResourceAmount> BUILD_COST = new EnumMap<>(BuildingType.class);
    public static final Map<BuildingType,ResourceAmount> UPKEEP = new EnumMap<>(BuildingType.class);
    public static final Map<UnitType,ResourceAmount> UNIT_COST = new EnumMap<>(UnitType.class);}
