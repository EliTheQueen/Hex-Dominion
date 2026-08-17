package model.disaster;

import model.Constants;
import model.GameMap;
import model.Hex;
import model.HexCoordinate;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DisasterPathBuilder {

    public List<HexCoordinate> buildRandomPath(
            HexCoordinate origin,
            GameMap map,
            int length,
            Random random,
            boolean allowMountain
    ) {
        if (origin == null || map == null || random == null) {
            throw new IllegalArgumentException("arguments must not be null");
        }

        if (length <= 0) {
            throw new IllegalArgumentException("length must be positive");
        }

        List<HexCoordinate> path = new ArrayList<>();
        path.add(origin);

        HexCoordinate current = origin;

        while (path.size() < length) {

            List<HexCoordinate> possible = new ArrayList<>();

            for (Hex neighbour : map.getNeighboursOf(current)) {

                HexCoordinate coordinate = neighbour.getCoordinate();

                if (path.contains(coordinate)) {
                    continue;
                }

                if (canEnter(neighbour, allowMountain)) {
                    possible.add(coordinate);
                }
            }

            if (possible.isEmpty()) {
                break;
            }

            current = possible.get(random.nextInt(possible.size()));

            path.add(current);
        }

        return path;
    }

    private boolean canEnter(
            Hex hex,
            boolean allowMountain
    ) {
        Constants.TerrainType terrain = hex.getTerrainType();

        if (terrain == Constants.TerrainType.SEA
                || terrain == Constants.TerrainType.MOUNTAIN_RANGE) {
            return false;
        }

        if (!allowMountain && terrain == Constants.TerrainType.MOUNTAIN) {
            return false;
        }

        return true;
    }
}