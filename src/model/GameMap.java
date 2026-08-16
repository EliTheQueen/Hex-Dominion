package model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

public class GameMap{

    private Map<HexCoordinate, Hex> hexes;
    private final Set<HexEdge> riverEdges;

    public GameMap(Map<HexCoordinate, Hex> hexes) {

        this.hexes = hexes;
        this.riverEdges = new HashSet<>();

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
        for (HexEdge edge : riverEdges) {
            if (edge.connects(first, second)) {
                return true;
            }
        }

        return false;
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
}
