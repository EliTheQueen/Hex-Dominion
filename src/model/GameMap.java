package model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GameMap {

    private Map<HexCoordinate, Hex> hexes;

    public GameMap(Map<HexCoordinate, Hex> hexes) {
        this.hexes = hexes;
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
}
