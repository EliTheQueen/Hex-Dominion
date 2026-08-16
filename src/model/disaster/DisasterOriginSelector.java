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

    private List<HexCoordinate> findEligibleCoordinates(
            DisasterType disasterType,
            GameMap map
    ) {
        switch (disasterType) {

            case EARTHQUAKE:
                return findLandCoordinates(map);

            case FLOOD:
                return findFloodCoordinates(map);

            case BEAR_ATTACK:
                return findTerrainCoordinates(
                        map,
                        Constants.TerrainType.FOREST
                );

            case TSUNAMI:
                return findCoastalCoordinates(map);

            case VOLCANIC_ERUPTION:
                return findTerrainCoordinates(
                        map,
                        Constants.TerrainType.VOLCANO
                );

            case TORNADO:
                return findTornadoCoordinates(map);

            case AVALANCHE:
                return findAvalancheCoordinates(map);

            case SEA_STORM:
                return findTerrainCoordinates(
                        map,
                        Constants.TerrainType.SEA
                );

            default:
                throw new IllegalArgumentException(
                        "origin selection is not implemented for " + disasterType
                );
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
        return hex.getTerrainType() != Constants.TerrainType.SEA
                && hex.getTerrainType() != Constants.TerrainType.MOUNTAIN_RANGE;
    }

    private List<HexCoordinate> findTerrainCoordinates(
            GameMap map,
            Constants.TerrainType terrainType
    ) {
        List<HexCoordinate> coordinates = new ArrayList<>();

        for (Hex hex : map.getAllHexes()) {
            if (hex.getTerrainType() == terrainType) {
                coordinates.add(hex.getCoordinate());
            }
        }

        return coordinates;
    }

    private List<HexCoordinate> findCoastalCoordinates(GameMap map) {
        List<HexCoordinate> result = new ArrayList<>();

        for (Hex hex : map.getAllHexes()) {
            if (map.isCoastal(hex.getCoordinate())) {
                result.add(hex.getCoordinate());
            }
        }

        return result;
    }

    private List<HexCoordinate> findFloodCoordinates(GameMap map) {
        List<HexCoordinate> result = new ArrayList<>();

        for (Hex hex : map.getAllHexes()) {

            Constants.TerrainType terrain = hex.getTerrainType();

            boolean validTerrain =
                    terrain == Constants.TerrainType.PLAIN
                            || terrain == Constants.TerrainType.GRASSLAND
                            || terrain == Constants.TerrainType.FOREST;

            if (!validTerrain) {
                continue;
            }

            HexCoordinate coordinate = hex.getCoordinate();

            if (map.isNextToRiver(coordinate) || map.isCoastal(coordinate)) {
                result.add(coordinate);
            }
        }

        return result;
    }

    private List<HexCoordinate> findTornadoCoordinates(GameMap map) {
        List<HexCoordinate> result = new ArrayList<>();

        for (Hex hex : map.getAllHexes()) {
            if (hex.getTerrainType() == Constants.TerrainType.PLAIN
                    || hex.getTerrainType() == Constants.TerrainType.GRASSLAND) {
                result.add(hex.getCoordinate());
            }
        }

        return result;
    }

    private List<HexCoordinate> findAvalancheCoordinates(GameMap map) {
        List<HexCoordinate> result = new ArrayList<>();

        for (Hex hex : map.getAllHexes()) {
            if (hex.getTerrainType() == Constants.TerrainType.MOUNTAIN
                    || hex.getTerrainType() == Constants.TerrainType.MOUNTAIN_RANGE) {
                result.add(hex.getCoordinate());
            }
        }

        return result;
    }
}