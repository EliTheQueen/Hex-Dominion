package model;

import model.season.MovementDomain;
import model.season.Season;
import model.season.SeasonMovementModifiers;

/** The single rules object used by pathfinding, previews, and actual movement. */
public final class MovementPolicy {
    private final boolean sailing;
    private final Season season;

    public MovementPolicy(boolean sailing, Season season) {
        this.sailing = sailing;
        this.season = season == null ? Season.SUMMER : season;
    }

    public static MovementPolicy basic() { return new MovementPolicy(false, Season.SUMMER); }

    public boolean canEnter(Hex hex) {
        if (hex == null || hex.isBlocked()) return false;
        if (hex.getTerrainType() == Constants.TerrainType.MOUNTAIN_RANGE) return false;
        return hex.getTerrainType() != Constants.TerrainType.SEA || sailing;
    }

    public int edgeCost(GameMap map, HexCoordinate from, HexCoordinate to) {
        Hex destination = map.getHex(to);
        if (!canEnter(destination)) return Integer.MAX_VALUE;
        boolean water = destination.getTerrainType() == Constants.TerrainType.SEA;
        int cost;
        if (!water && map.hasRoad(from) && map.hasRoad(to)) {
            cost = 1;
        } else {
            cost = Constants.MOVE_COST.getOrDefault(destination.getTerrainType(), 1);
        }
        if (map.hasRiverBetween(from, to) && !map.hasBridgeBetween(from, to)) cost++;
        return SeasonMovementModifiers.calculateFinalCost(cost, season,
                water ? MovementDomain.WATER : MovementDomain.LAND);
    }

    public boolean enteringWater(GameMap map, HexCoordinate from, HexCoordinate to) {
        Hex a = map.getHex(from);
        Hex b = map.getHex(to);
        return a != null && b != null
                && a.getTerrainType() != Constants.TerrainType.SEA
                && b.getTerrainType() == Constants.TerrainType.SEA;
    }
}
