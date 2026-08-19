package model;

public class InfrastructureService implements java.io.Serializable {

    private static final int ACTION_AP_COST = 1;

    private final GameMap map;
    private final Player player;

    public InfrastructureService(GameMap map, Player player) {
        if (map == null || player == null) {
            throw new IllegalArgumentException("map and player must not be null");
        }

        this.map = map;
        this.player = player;
    }

    public boolean buildRoad(Builder builder, HexCoordinate coordinate) {
        if (!canUseBuilder(builder) || !canReach(builder, coordinate)) {
            return false;
        }

        Hex hex = map.getHex(coordinate);

        if (hex == null || !hex.getIsExplored() || !player.isInTerritory(coordinate)
                || hex.getTerrainType() == Constants.TerrainType.SEA
                || hex.getTerrainType() == Constants.TerrainType.MOUNTAIN_RANGE) {
            return false;
        }

        if (hex.hasRoad()) {
            return false;
        }

        if (!builder.spendAP(ACTION_AP_COST)) {
            return false;
        }

        map.buildRoad(coordinate);
        return true;
    }

    public boolean demolishRoad(Builder builder, HexCoordinate coordinate) {
        if (!canUseBuilder(builder) || !canReach(builder, coordinate)) {
            return false;
        }

        if (!map.hasRoad(coordinate)) {
            return false;
        }

        if (!builder.spendAP(ACTION_AP_COST)) {
            return false;
        }

        map.removeRoad(coordinate);
        return true;
    }

    public boolean buildWall(Builder builder, HexCoordinate first, HexCoordinate second) {
        if (!canBuildWall(builder, first, second)) return false;

        int apBefore = builder.getCurrentAP();
        if (!builder.spendAP(ACTION_AP_COST)) {
            return false;
        }
        if (!player.spend(Constants.WALL_COST)) {
            builder.setCurrentAP(apBefore);
            return false;
        }

        map.buildWall(first, second);
        return true;
    }

    public boolean canBuildWall(Builder builder, HexCoordinate first, HexCoordinate second) {
        return canUseBuilder(builder)
                && isValidEdge(first, second)
                && builderTouchesEdge(builder, first, second)
                && (player.isInTerritory(first) || player.isInTerritory(second))
                && validWallTerrain(first) && validWallTerrain(second)
                && map.getHex(first).getIsExplored() && map.getHex(second).getIsExplored()
                && !map.hasWallBetween(first, second)
                && player.canAfford(Constants.WALL_COST);
    }

    public ResourceAmount getWallCost() { return Constants.WALL_COST.copy(); }

    /** Builds a free wall for a completed technology effect, without consuming a builder action. */
    public boolean buildAutomaticWall(HexCoordinate first, HexCoordinate second) {
        if (!isValidEdge(first, second) || map.hasWallBetween(first, second)) return false;
        if (!player.isInTerritory(first) && !player.isInTerritory(second)) return false;
        if (!validWallTerrain(first) || !validWallTerrain(second)) return false;
        map.buildWall(first, second);
        return true;
    }

    public boolean demolishWall(Builder builder, HexCoordinate first, HexCoordinate second) {
        if (!canDemolishWall(builder, first, second)) return false;

        if (!builder.spendAP(ACTION_AP_COST)) {
            return false;
        }

        map.removeWall(first, second);
        return true;
    }

    public boolean canDemolishWall(Builder builder, HexCoordinate first, HexCoordinate second) {
        return canUseBuilder(builder) && isValidEdge(first, second)
                && builderTouchesEdge(builder, first, second)
                && map.hasWallBetween(first, second);
    }

    public boolean buildBridge(Builder builder, HexCoordinate first, HexCoordinate second) {
        if (!canUseBuilder(builder) || !isValidEdge(first, second)) {
            return false;
        }

        if (!builderTouchesEdge(builder, first, second)) {
            return false;
        }

        if (!map.hasRiverBetween(first, second) || map.hasBridgeBetween(first, second)) {
            return false;
        }

        if (!builder.spendAP(ACTION_AP_COST)) {
            return false;
        }

        map.buildBridge(first, second);
        return true;
    }

    public boolean demolishBuilding(Builder builder, HexCoordinate coordinate) {
        if (!canDemolishBuilding(builder, coordinate)) return false;
        Building building = player.getBuildingAt(coordinate);
        if (!builder.spendAP(ACTION_AP_COST)) return false;
        if (player.destroyBuilding(map, building)) return true;
        builder.setCurrentAP(builder.getCurrentAP() + ACTION_AP_COST);
        return false;
    }

    public boolean canDemolishBuilding(Builder builder, HexCoordinate coordinate) {
        if (!canUseBuilder(builder) || !canReach(builder, coordinate)) return false;
        Building building = player.getBuildingAt(coordinate);
        return building != null && building.getType() != Constants.BuildingType.TOWN_HALL;
    }

    public boolean demolishBridge(Builder builder, HexCoordinate first, HexCoordinate second) {
        if (!canUseBuilder(builder) || !isValidEdge(first, second)) {
            return false;
        }

        if (!builderTouchesEdge(builder, first, second)) {
            return false;
        }

        if (!map.hasBridgeBetween(first, second)) {
            return false;
        }

        if (!builder.spendAP(ACTION_AP_COST)) {
            return false;
        }

        map.removeBridge(first, second);
        return true;
    }

    private boolean canUseBuilder(Builder builder) {
        return builder != null && builder.isAlive() && builder.getCurrentAP() >= ACTION_AP_COST;
    }

    private boolean canReach(Builder builder, HexCoordinate coordinate) {
        if (coordinate == null) {
            return false;
        }

        return builder.getPosition().equals(coordinate)
                || builder.getPosition().distanceTo(coordinate) == 1;
    }

    private boolean isValidEdge(HexCoordinate first, HexCoordinate second) {
        if (first == null || second == null) {
            return false;
        }

        if (!map.containsCoordinate(first) || !map.containsCoordinate(second)) {
            return false;
        }

        return first.findNeighbours().contains(second);
    }

    private boolean builderTouchesEdge(Builder builder, HexCoordinate first, HexCoordinate second) {
        return builder.getPosition().equals(first) || builder.getPosition().equals(second);
    }

    private boolean validWallTerrain(HexCoordinate coordinate) {
        Hex hex = map.getHex(coordinate);
        return hex != null && hex.getTerrainType() != Constants.TerrainType.SEA
                && hex.getTerrainType() != Constants.TerrainType.MOUNTAIN_RANGE;
    }
}
