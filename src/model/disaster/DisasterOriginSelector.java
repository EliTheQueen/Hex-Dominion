package model.disaster;

import model.Constants;
import model.GameMap;
import model.Hex;
import model.HexCoordinate;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DisasterOriginSelector {

    public HexCoordinate select(DisasterType disasterType, GameMap map, Random random) {
        if (disasterType == null || map == null || random == null) {
            throw new IllegalArgumentException("arguments must not be null");
        }

        List<HexCoordinate> eligibleCoordinates = findEligibleCoordinates(disasterType, map);

        if (eligibleCoordinates.isEmpty()) {
            return null;
        }

        int randomIndex = random.nextInt(eligibleCoordinates.size());

        return eligibleCoordinates.get(randomIndex);
    }

    private List<HexCoordinate> findEligibleCoordinates(DisasterType disasterType, GameMap map) {
        switch (disasterType) {
            case EARTHQUAKE: return findLandCoordinates(map);

            default:
                throw new IllegalArgumentException("origin selection is not implemented for " + disasterType);
        }
    }

    private List<HexCoordinate> findLandCoordinates(GameMap map) {
        List<HexCoordinate> coordinates = new ArrayList<>();

        for (Hex hex : map.getAllHexes()) {
            if (isLand(hex)) {
                coordinates.add(hex.getCoordinate());
            }
        }

        return coordinates;
    }

    private boolean isLand(Hex hex) {
        return hex.getTerrainType() != Constants.TerrainType.SEA;
    }
}