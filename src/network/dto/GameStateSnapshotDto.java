package network.dto;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Pure-data JSON representation of the state needed by the network client UI. */
public final class GameStateSnapshotDto {
    private final int width;
    private final int height;
    private final int currentTurn;
    private final int seasonTurn;
    private final boolean gameOver;
    private final boolean starving;
    private final String gameOverReason;
    private final int finalScore;
    private final CoordinateDto townHall;
    private final PlayerDto player;
    private final TownHallDto townHallState;
    private final List<HexDto> hexes;
    private final List<EdgeDto> rivers;
    private final List<EdgeDto> walls;
    private final List<CoordinateDto> tradingPosts;
    private final List<String> completedPhaseTwoTechnologies;
    private final List<String> queuedPhaseTwoTechnologies;
    private final boolean sailing;
    private final boolean steelTools;
    private final boolean defensiveArchitecture;
    private final List<String> lastTurnEvents;

    public GameStateSnapshotDto(int width, int height, int currentTurn, int seasonTurn,
            boolean gameOver, boolean starving, String gameOverReason, int finalScore,
            CoordinateDto townHall, PlayerDto player, TownHallDto townHallState,
            List<HexDto> hexes, List<EdgeDto> rivers, List<EdgeDto> walls,
            List<CoordinateDto> tradingPosts, List<String> completedPhaseTwoTechnologies,
            List<String> queuedPhaseTwoTechnologies, boolean sailing, boolean steelTools,
            boolean defensiveArchitecture, List<String> lastTurnEvents) {
        this.width = width;
        this.height = height;
        this.currentTurn = currentTurn;
        this.seasonTurn = seasonTurn;
        this.gameOver = gameOver;
        this.starving = starving;
        this.gameOverReason = gameOverReason;
        this.finalScore = finalScore;
        this.townHall = townHall;
        this.player = player;
        this.townHallState = townHallState;
        this.hexes = hexes;
        this.rivers = rivers;
        this.walls = walls;
        this.tradingPosts = tradingPosts;
        this.completedPhaseTwoTechnologies = completedPhaseTwoTechnologies;
        this.queuedPhaseTwoTechnologies = queuedPhaseTwoTechnologies;
        this.sailing = sailing;
        this.steelTools = steelTools;
        this.defensiveArchitecture = defensiveArchitecture;
        this.lastTurnEvents = lastTurnEvents;
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getCurrentTurn() { return currentTurn; }
    public int getSeasonTurn() { return seasonTurn; }
    public boolean isGameOver() { return gameOver; }
    public boolean isStarving() { return starving; }
    public String getGameOverReason() { return gameOverReason; }
    public int getFinalScore() { return finalScore; }
    public CoordinateDto getTownHall() { return townHall; }
    public PlayerDto getPlayer() { return player; }
    public TownHallDto getTownHallState() { return townHallState; }
    public List<HexDto> getHexes() { return safe(hexes); }
    public List<EdgeDto> getRivers() { return safe(rivers); }
    public List<EdgeDto> getWalls() { return safe(walls); }
    public List<CoordinateDto> getTradingPosts() { return safe(tradingPosts); }
    public List<String> getCompletedPhaseTwoTechnologies() { return safe(completedPhaseTwoTechnologies); }
    public List<String> getQueuedPhaseTwoTechnologies() { return safe(queuedPhaseTwoTechnologies); }
    public boolean hasSailing() { return sailing; }
    public boolean hasSteelTools() { return steelTools; }
    public boolean hasDefensiveArchitecture() { return defensiveArchitecture; }
    public List<String> getLastTurnEvents() { return safe(lastTurnEvents); }

    private static <T> List<T> safe(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }

    public static final class CoordinateDto {
        private final int q;
        private final int r;
        public CoordinateDto(int q, int r) { this.q = q; this.r = r; }
        public int getQ() { return q; }
        public int getR() { return r; }
    }

    public static final class PlayerDto {
        private final String name;
        private final Map<String, Integer> resources;
        private final Map<String, Integer> capacities;
        private final List<String> legacyTechnologies;
        private final List<CoordinateDto> territory;
        private final List<UnitDto> units;
        private final List<BuildingDto> buildings;

        public PlayerDto(String name, Map<String, Integer> resources,
                Map<String, Integer> capacities, List<String> legacyTechnologies,
                List<CoordinateDto> territory, List<UnitDto> units,
                List<BuildingDto> buildings) {
            this.name = name;
            this.resources = resources;
            this.capacities = capacities;
            this.legacyTechnologies = legacyTechnologies;
            this.territory = territory;
            this.units = units;
            this.buildings = buildings;
        }

        public String getName() { return name; }
        public Map<String, Integer> getResources() { return resources == null ? Collections.emptyMap() : resources; }
        public Map<String, Integer> getCapacities() { return capacities == null ? Collections.emptyMap() : capacities; }
        public List<String> getLegacyTechnologies() { return safe(legacyTechnologies); }
        public List<CoordinateDto> getTerritory() { return safe(territory); }
        public List<UnitDto> getUnits() { return safe(units); }
        public List<BuildingDto> getBuildings() { return safe(buildings); }
    }

    public static final class UnitDto {
        private final String type;
        private final String militaryType;
        private final CoordinateDto position;
        private final int currentAp;
        private final int currentHp;
        private final boolean alive;
        private final String state;
        private final int builderCharges;
        private final boolean autoExplore;
        private final int stationedBuildingIndex;

        public UnitDto(String type, String militaryType, CoordinateDto position, int currentAp,
                int currentHp, boolean alive, String state, int builderCharges,
                boolean autoExplore, int stationedBuildingIndex) {
            this.type = type;
            this.militaryType = militaryType;
            this.position = position;
            this.currentAp = currentAp;
            this.currentHp = currentHp;
            this.alive = alive;
            this.state = state;
            this.builderCharges = builderCharges;
            this.autoExplore = autoExplore;
            this.stationedBuildingIndex = stationedBuildingIndex;
        }

        public String getType() { return type; }
        public String getMilitaryType() { return militaryType; }
        public CoordinateDto getPosition() { return position; }
        public int getCurrentAp() { return currentAp; }
        public int getCurrentHp() { return currentHp; }
        public boolean isAlive() { return alive; }
        public String getState() { return state; }
        public int getBuilderCharges() { return builderCharges; }
        public boolean isAutoExplore() { return autoExplore; }
        public int getStationedBuildingIndex() { return stationedBuildingIndex; }
    }

    public static final class BuildingDto {
        private final CoordinateDto position;
        private final String type;
        private final int currentHp;
        private final int maxHp;

        public BuildingDto(CoordinateDto position, String type, int currentHp, int maxHp) {
            this.position = position;
            this.type = type;
            this.currentHp = currentHp;
            this.maxHp = maxHp;
        }

        public CoordinateDto getPosition() { return position; }
        public String getType() { return type; }
        public int getCurrentHp() { return currentHp; }
        public int getMaxHp() { return maxHp; }
    }

    public static final class HexDto {
        private final CoordinateDto coordinate;
        private final String terrain;
        private final Map<String, Integer> naturalResources;
        private final boolean explored;
        private final boolean visible;
        private final boolean expanded;
        private final boolean hasBuilding;
        private final boolean everHadResource;
        private final int blockedTurns;
        private final boolean road;

        public HexDto(CoordinateDto coordinate, String terrain,
                Map<String, Integer> naturalResources, boolean explored, boolean visible,
                boolean expanded, boolean hasBuilding, boolean everHadResource,
                int blockedTurns, boolean road) {
            this.coordinate = coordinate;
            this.terrain = terrain;
            this.naturalResources = naturalResources;
            this.explored = explored;
            this.visible = visible;
            this.expanded = expanded;
            this.hasBuilding = hasBuilding;
            this.everHadResource = everHadResource;
            this.blockedTurns = blockedTurns;
            this.road = road;
        }

        public CoordinateDto getCoordinate() { return coordinate; }
        public String getTerrain() { return terrain; }
        public Map<String, Integer> getNaturalResources() {
            return naturalResources == null ? Collections.emptyMap() : naturalResources;
        }
        public boolean isExplored() { return explored; }
        public boolean isVisible() { return visible; }
        public boolean isExpanded() { return expanded; }
        public boolean hasBuilding() { return hasBuilding; }
        public boolean hasEverHadResource() { return everHadResource; }
        public int getBlockedTurns() { return blockedTurns; }
        public boolean hasRoad() { return road; }
    }

    public static final class EdgeDto {
        private final CoordinateDto first;
        private final CoordinateDto second;
        private final boolean bridge;
        private final int currentHp;
        private final int maxHp;

        public EdgeDto(CoordinateDto first, CoordinateDto second, boolean bridge,
                int currentHp, int maxHp) {
            this.first = first;
            this.second = second;
            this.bridge = bridge;
            this.currentHp = currentHp;
            this.maxHp = maxHp;
        }

        public CoordinateDto getFirst() { return first; }
        public CoordinateDto getSecond() { return second; }
        public boolean hasBridge() { return bridge; }
        public int getCurrentHp() { return currentHp; }
        public int getMaxHp() { return maxHp; }
    }

    public static final class TownHallDto {
        private final String level;
        private final int currentHp;
        private final boolean defensiveArchitecture;

        public TownHallDto(String level, int currentHp, boolean defensiveArchitecture) {
            this.level = level;
            this.currentHp = currentHp;
            this.defensiveArchitecture = defensiveArchitecture;
        }

        public String getLevel() { return level; }
        public int getCurrentHp() { return currentHp; }
        public boolean hasDefensiveArchitecture() { return defensiveArchitecture; }
    }
}
