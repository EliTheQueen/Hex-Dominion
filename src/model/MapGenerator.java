package model;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class MapGenerator {

    private int mapWidth;
    private int mapHeight;
    private Map<HexCoordinate, Hex> hexes;
    private HexCoordinate center;
    private final Random random = new Random();

    public GameMap generateMap(int mapWidth, int mapHeight) {
        this.mapWidth = mapWidth;
        this.mapHeight = mapHeight;
        this.center = new HexCoordinate(mapWidth / 2, mapHeight / 2);
        this.hexes = new HashMap<>();

        createBaseMap();
        createSeaArea();
        createMountainChain();
        createMountainRange();
        createVolcanoes();
        createNearbyForest();
        createTownHallSafeArea();
        placeNaturalResources();

        GameMap map = new GameMap(hexes);
        createRiver(map);
        placeTradingPosts(map);

        return map;
    }

    private void placeTradingPosts(GameMap map) {
        int placed = 0;
        for (Hex hex : map.getAllHexes()) {
            if (placed >= 2) break;
            HexCoordinate coordinate = hex.getCoordinate();
            if (center.distanceTo(coordinate) < 4) continue;
            if (hex.getTerrainType() == Constants.TerrainType.PLAIN
                    || hex.getTerrainType() == Constants.TerrainType.GRASSLAND) {
                map.addTradingPost(coordinate);
                placed++;
            }
        }
    }

    private void createBaseMap() {
        for (int q = 0; q < mapWidth; q++) {
            for (int r = 0; r < mapHeight; r++) {
                HexCoordinate coordinate = new HexCoordinate(q, r);
                hexes.put(coordinate, new Hex(coordinate, chooseBaseTerrain()));
            }
        }
    }

    private Constants.TerrainType chooseBaseTerrain() {
        if (random.nextDouble() < 0.5) {
            return Constants.TerrainType.PLAIN;
        }

        return Constants.TerrainType.GRASSLAND;
    }

    private void createSeaArea() {
        for (Hex hex : hexes.values()) {
            HexCoordinate coordinate = hex.getCoordinate();

            if (center.distanceTo(coordinate) <= 3) {
                continue;
            }

            if (coordinate.getQ() == 0) {
                hex.setTerrainType(Constants.TerrainType.SEA);
            } else if (coordinate.getQ() == 1 && coordinate.getR() % 3 != 0) {
                hex.setTerrainType(Constants.TerrainType.SEA);
            }
        }
    }

    private void createMountainChain() {
        int r = mapHeight / 3;

        for (int q = 0; q < mapWidth; q++) {
            if (q % 3 == 0 && r + 1 < mapHeight) {
                r++;
            } else if (q % 3 == 1 && r - 1 >= 0) {
                r--;
            }

            HexCoordinate coordinate = new HexCoordinate(q, r);
            Hex hex = hexes.get(coordinate);

            if (hex == null || center.distanceTo(coordinate) <= 3) {
                continue;
            }

            if (hex.getTerrainType() != Constants.TerrainType.SEA) {
                hex.setTerrainType(Constants.TerrainType.MOUNTAIN);
            }
        }
    }

    private void createMountainRange() {
        int q = Math.max(2, (mapWidth * 3) / 4);
        int gap = mapHeight / 2;

        for (int r = 1; r < mapHeight - 1; r++) {
            if (Math.abs(r - gap) <= 1) {
                continue;
            }

            HexCoordinate coordinate = new HexCoordinate(q, r);
            Hex hex = hexes.get(coordinate);

            if (hex == null || center.distanceTo(coordinate) <= 3) {
                continue;
            }

            if (hex.getTerrainType() != Constants.TerrainType.SEA) {
                hex.setTerrainType(Constants.TerrainType.MOUNTAIN_RANGE);
            }
        }
    }

    private void createVolcanoes() {
        for (Hex hex : hexes.values()) {
            if (hex.getTerrainType() != Constants.TerrainType.MOUNTAIN) {
                continue;
            }

            if (center.distanceTo(hex.getCoordinate()) <= 4) {
                continue;
            }

            if (random.nextDouble() < 0.15) {
                hex.setTerrainType(Constants.TerrainType.VOLCANO);
            }
        }
    }

    private void createNearbyForest() {
        for (HexCoordinate coordinate : hexes.keySet()) {
            if (coordinate.distanceTo(center) != 2) {
                continue;
            }

            Hex hex = hexes.get(coordinate);

            if (hex.getTerrainType() == Constants.TerrainType.PLAIN
                    || hex.getTerrainType() == Constants.TerrainType.GRASSLAND) {

                if ((coordinate.getQ() + coordinate.getR()) % 2 == 0) {
                    hex.setTerrainType(Constants.TerrainType.FOREST);
                }
            }
        }
    }

    private void createTownHallSafeArea() {
        for (Hex hex : hexes.values()) {
            if (center.distanceTo(hex.getCoordinate()) <= 1) {
                hex.setTerrainType(chooseBaseTerrain());
            }
        }
    }

    private void createRiver(GameMap map) {
        int q = Math.max(2, mapWidth / 3);

        for (int r = 1; r < mapHeight - 2; r++) {
            HexCoordinate first = new HexCoordinate(q, r);
            HexCoordinate second = new HexCoordinate(q, r + 1);

            Hex firstHex = map.getHex(first);
            Hex secondHex = map.getHex(second);

            if (!canRiverPass(firstHex) || !canRiverPass(secondHex)) {
                continue;
            }

            map.addRiver(first, second);
        }
    }

    private boolean canRiverPass(Hex hex) {
        if (hex == null) {
            return false;
        }

        return hex.getTerrainType() != Constants.TerrainType.SEA
                && hex.getTerrainType() != Constants.TerrainType.MOUNTAIN_RANGE;
    }

    private void placeNaturalResources() {
        for (Hex hex : hexes.values()) {
            Constants.TerrainType terrainType = hex.getTerrainType();

            if (terrainType == Constants.TerrainType.MOUNTAIN) {
                hex.addNaturalResource(Constants.NaturalResourceType.STONE, 120);

                if (random.nextDouble() < 0.33) {
                    hex.addNaturalResource(Constants.NaturalResourceType.IRON, 60);
                }
            } else if (terrainType == Constants.TerrainType.VOLCANO) {
                hex.addNaturalResource(Constants.NaturalResourceType.STONE, 100);

                if (random.nextDouble() < 0.5) {
                    hex.addNaturalResource(Constants.NaturalResourceType.IRON, 50);
                }
            } else if (terrainType == Constants.TerrainType.FOREST) {
                hex.addNaturalResource(Constants.NaturalResourceType.WOOD, 100);
            } else if (terrainType == Constants.TerrainType.PLAIN) {
                double value = random.nextDouble();

                if (value < 0.33) {
                    hex.addNaturalResource(Constants.NaturalResourceType.COW, 80);
                } else if (value < 0.66) {
                    hex.addNaturalResource(Constants.NaturalResourceType.SHEEP, 80);
                }
            } else if (terrainType == Constants.TerrainType.GRASSLAND) {
                double value = random.nextDouble();

                if (value < 0.33) {
                    hex.addNaturalResource(Constants.NaturalResourceType.WHEAT, 100);
                } else if (value < 0.66) {
                    hex.addNaturalResource(Constants.NaturalResourceType.RICE, 100);
                }
            } else if (terrainType == Constants.TerrainType.SEA && random.nextDouble() < 0.4) {
                hex.addNaturalResource(Constants.NaturalResourceType.FISH, 100);
            }
        }
    }
}
