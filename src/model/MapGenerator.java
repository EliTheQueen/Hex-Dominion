package model;


import java.util.*;
import model.Constants;

public class MapGenerator {

    private int mapWidth;
    private int mapHeight;
    private Map<HexCoordinate, Hex> hexes;
    private HexCoordinate center;



    public GameMap generateMap(int mapWidth, int mapHeight) {
        this.mapWidth = mapWidth;
        this.mapHeight = mapHeight;
        this.center = new HexCoordinate(mapWidth/2, mapHeight/2);
        this.hexes = new HashMap<>();

        createBaseMap(hexes);
        createTownHallNeighbours();
        createNearbyForest(hexes, center);
        createMountainChain(hexes, mapWidth, mapHeight, center);
        placeNaturalResources();

        return new GameMap(hexes);
    }

    private void placeNaturalResources() {
        for (Hex hex : hexes.values()) {
            Constants.TerrainType terrainType = hex.getTerrainType();
            if (terrainType == Constants.TerrainType.MOUNTAIN) {
                hex.addNaturalResource(Constants.NaturalResourceType.STONE, 120);

                double r = Math.random();
                if (r < 0.33) {
                    hex.addNaturalResource(Constants.NaturalResourceType.IRON, 60);
                }
            }
            else  if (terrainType == Constants.TerrainType.FOREST) {
                hex.addNaturalResource(Constants.NaturalResourceType.TREE, 100);
            }
            else if (terrainType == Constants.TerrainType.GRASSLAND) {
                double r = Math.random();
                if (r < 0.33) {

                    hex.addNaturalResource(Constants.NaturalResourceType.COW, 80);
                } else if (0.33 <= r && r < 0.66) {
                    hex.addNaturalResource(Constants.NaturalResourceType.SHEEP, 80);
                }
            } else if (terrainType == Constants.TerrainType.PLAIN) {
                double r = Math.random();
                if (r < 0.33) {
                    hex.addNaturalResource(Constants.NaturalResourceType.WHEAT, 100);
                } else if (0.33 <= r && r < 0.66) {
                    hex.addNaturalResource(Constants.NaturalResourceType.RICE, 100);
                }
            }
        }
    }

    private void createMountainChain(Map<HexCoordinate, Hex> hexes, int mapWidth, int mapHeight, HexCoordinate center) {
        int r = mapHeight / 3;

        for (int q = 0; q < mapWidth; q++) {
            if (q % 3 == 0 && r + 1 < mapHeight) {
                r++;
            } else if (q % 3 == 1 && r - 1 >= 0) {
                r--;
            }

            HexCoordinate coordinate = new HexCoordinate(q, r);

            if (!hexes.containsKey(coordinate)) {
                continue;
            }

            if (center.distanceTo(coordinate) <= 2) {
                continue;
            }

            Hex hex = hexes.get(coordinate);
            hex.setTerrainType(Constants.TerrainType.MOUNTAIN);
        }
    }

    private void createNearbyForest(Map<HexCoordinate, Hex> hexes, HexCoordinate center) {
        for( HexCoordinate hex : hexes.keySet() ) {
            if (hex.distanceTo(center) == 2) {
                if ((hex.getQ() + hex.getR()) % 2 == 0) {

                    hexes.get(hex).setTerrainType(Constants.TerrainType.FOREST);
                }
            }
        }

    }

    private void createBaseMap(Map<HexCoordinate, Hex> hexes) {
        for (int q=0; q<mapWidth; q++) {
            for (int r=0; r<mapHeight; r++) {
                HexCoordinate coordinate = new HexCoordinate(q, r);

                Constants.TerrainType terrainType = chooseBaseTerrain();

                Hex hex = new Hex(coordinate, terrainType);
                hexes.put(coordinate, hex);
            }
        }
    }

    private Constants.TerrainType chooseBaseTerrain() {
        double random = Math.random();
        if (random < 0.5) {
            return Constants.TerrainType.PLAIN;
        }
        return Constants.TerrainType.GRASSLAND;
    }

    private void createTownHallNeighbours() {

        for (Hex hex : hexes.values()) {
            if (center.distanceTo(hex.getCoordinate()) <= 1) {
                hex.setTerrainType(chooseBaseTerrain());
            }
        }


    }
}
