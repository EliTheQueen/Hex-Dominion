package model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

public class GameMap implements java.io.Serializable {

    private Map<HexCoordinate, Hex> hexes;
    private final Set<HexEdge> riverEdges;
    private final Set<HexEdge> wallEdges;
    private final Set<HexCoordinate> tradingPosts;
    private final Map<HexCoordinate, Integer> tradingPostLastTradeTurns;

    public GameMap(Map<HexCoordinate, Hex> hexes) {

        this.hexes = hexes;
        this.riverEdges = new HashSet<>();
        this.wallEdges = new HashSet<>();
        this.tradingPosts = new HashSet<>();
        this.tradingPostLastTradeTurns = new java.util.HashMap<>();

    }

    public Hex getHex(HexCoordinate coordinate) {
        return hexes.get(coordinate);
    }

    public boolean containsCoordinate(HexCoordinate coordinate) {
        return hexes.containsKey(coordinate);
    }

    public List<Hex> getNeighboursOf(HexCoordinate coordinate) {
        List<Hex> neighbours = new ArrayList<>();

        for (HexCoordinate neighborCoordinate : coordinate.findNeighbours()) {
            Hex neighborHex = getHex(neighborCoordinate);

            if (neighborHex != null) {
                neighbours.add(neighborHex);
            }
        }

        return neighbours;
    }

    public List<Hex> getHexesInRadius(HexCoordinate center, int radius) {
        List<Hex> hexesInRadius = new ArrayList<>();
        for (HexCoordinate hexCoordinate : hexes.keySet()) {
            if (center.distanceTo(hexCoordinate) <= radius) {
                hexesInRadius.add(hexes.get(hexCoordinate));
            }
        }
        return hexesInRadius;
    }

    public List<Hex> getAllHexes() {
        List<Hex> result = new ArrayList<>();
        for (HexCoordinate hexCoordinate : hexes.keySet()) {
            result.add(hexes.get(hexCoordinate));
        }
        return result;
    }

    public  List<HexCoordinate> getHexCoordinatesInRadius(HexCoordinate center, int radius) {

        List<HexCoordinate> result = new ArrayList<>();

        for (Hex hex : getHexesInRadius(center, radius)) {
            result.add(hex.getCoordinate());
        }

        return result;
    }

    public void addRiver(HexCoordinate first, HexCoordinate second) {
        if (!containsCoordinate(first) || !containsCoordinate(second)) {
            throw new IllegalArgumentException("river coordinates must be inside map");
        }

        riverEdges.add(new HexEdge(first, second));
    }

    public boolean hasRiverBetween(HexCoordinate first, HexCoordinate second) {
        return findRiverEdge(first, second) != null;
    }

    public boolean isNextToRiver(HexCoordinate coordinate) {
        for (HexEdge edge : riverEdges) {
            if (edge.getFirst().equals(coordinate) || edge.getSecond().equals(coordinate)) {
                return true;
            }
        }

        return false;
    }

    public boolean isCoastal(HexCoordinate coordinate) {
        Hex hex = getHex(coordinate);

        if (hex == null || hex.getTerrainType() == Constants.TerrainType.SEA) {
            return false;
        }

        for (Hex neighbour : getNeighboursOf(coordinate)) {
            if (neighbour.getTerrainType() == Constants.TerrainType.SEA) {
                return true;
            }
        }

        return false;
    }

    public Set<HexEdge> getRiverEdges() {
        return new HashSet<>(riverEdges);
    }

    public void advanceBlockedHexes() {
        for (Hex hex : getAllHexes()) {
            hex.advanceBlockedTurn();
        }
    }

    public void buildRoad(HexCoordinate coordinate) {
        Hex hex = getHex(coordinate);

        if (hex == null) {
            throw new IllegalArgumentException("coordinate is not in map");
        }

        hex.buildRoad();
    }

    public void removeRoad(HexCoordinate coordinate) {
        Hex hex = getHex(coordinate);

        if (hex == null) {
            throw new IllegalArgumentException("coordinate is not in map");
        }

        hex.removeRoad();
    }

    /** Removes every road touched by a map effect such as a disaster. */
    public void removeRoads(Iterable<HexCoordinate> coordinates) {
        if (coordinates == null) {
            throw new IllegalArgumentException("coordinates must not be null");
        }

        for (HexCoordinate coordinate : coordinates) {
            Hex hex = getHex(coordinate);
            if (hex != null) {
                hex.removeRoad();
            }
        }
    }

    public boolean hasRoad(HexCoordinate coordinate) {
        Hex hex = getHex(coordinate);
        return hex != null && hex.hasRoad();
    }

    public void buildWall(
            HexCoordinate first,
            HexCoordinate second
    ) {
        if (!containsCoordinate(first) || !containsCoordinate(second)) {
            throw new IllegalArgumentException("wall coordinates must be inside map");
        }

        HexEdge edge = findWallEdge(first, second);

        if (edge == null) {
            edge = new HexEdge(first, second);
            wallEdges.add(edge);
        }

        edge.buildWall(100);
    }

    public boolean hasWallBetween(
            HexCoordinate first,
            HexCoordinate second
    ) {
        HexEdge edge = findWallEdge(first, second);
        return edge != null && edge.hasWall();
    }

    public Wall getWallBetween(
            HexCoordinate first,
            HexCoordinate second
    ) {
        HexEdge edge = findWallEdge(first, second);

        if (edge == null) {
            return null;
        }

        return edge.getWall();
    }

    public void removeWall(
            HexCoordinate first,
            HexCoordinate second
    ) {
        wallEdges.removeIf(edge -> edge.connects(first, second));
    }

    private HexEdge findWallEdge(
            HexCoordinate first,
            HexCoordinate second
    ) {
        for (HexEdge edge : wallEdges) {
            if (edge.connects(first, second)) {
                return edge;
            }
        }

        return null;
    }

    public Set<HexEdge> getWallEdges() {
        return new HashSet<>(wallEdges);
    }

    private HexEdge findRiverEdge(HexCoordinate first, HexCoordinate second) {
        for (HexEdge edge : riverEdges) {
            if (edge.connects(first, second)) {
                return edge;
            }
        }

        return null;
    }

    public void buildBridge(HexCoordinate first, HexCoordinate second) {
        HexEdge edge = findRiverEdge(first, second);

        if (edge == null) {
            throw new IllegalArgumentException("bridge can only be built on a river");
        }

        edge.buildBridge();
    }

    public boolean hasBridgeBetween(HexCoordinate first, HexCoordinate second) {
        HexEdge edge = findRiverEdge(first, second);
        return edge != null && edge.hasBridge();
    }

    public void removeBridge(HexCoordinate first, HexCoordinate second) {
        HexEdge edge = findRiverEdge(first, second);

        if (edge != null) {
            edge.removeBridge();
        }
    }

    public void addTradingPost(HexCoordinate coordinate) {
        Hex hex = getHex(coordinate);
        if (hex == null || hex.getTerrainType() == Constants.TerrainType.SEA
                || hex.getTerrainType() == Constants.TerrainType.MOUNTAIN_RANGE)
            throw new IllegalArgumentException("Trading Post requires passable land");
        tradingPosts.add(coordinate);
    }
    public boolean hasTradingPost(HexCoordinate coordinate) { return tradingPosts.contains(coordinate); }
    public Set<HexCoordinate> getTradingPosts() { return new HashSet<>(tradingPosts); }
    public boolean canTradeAtPost(HexCoordinate coordinate, int turn) {
        return hasTradingPost(coordinate) && tradingPostLastTradeTurns.getOrDefault(coordinate, -1) != turn;
    }
    public void markTradingPostUsed(HexCoordinate coordinate, int turn) {
        if (!hasTradingPost(coordinate)) throw new IllegalArgumentException("No Trading Post");
        tradingPostLastTradeTurns.put(coordinate, turn);
    }

}
