import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import model.Constants;
import model.GameState;
import model.Hex;
import model.HexCoordinate;
import model.Unit;

/** Many-seed safety and reproducibility properties for complete game generation. */
public final class MapGenerationSafetyPropertyTest {
    private static final int SEED_COUNT = 500;

    public static void main(String[] args) {
        for (long seed = 0; seed < SEED_COUNT; seed++) assertSafe(seed);
        require(signature(new GameState(15, 13, 947L)).equals(
                        signature(new GameState(15, 13, 947L))),
                "the same seed must generate the same complete initial map");
        require(!signature(new GameState(15, 13, 947L)).equals(
                        signature(new GameState(15, 13, 948L))),
                "different seeds should generate different initial maps");
        System.out.println("MapGenerationSafetyPropertyTest passed (" + SEED_COUNT + " seeds)");
    }

    private static void assertSafe(long seed) {
        GameState state = new GameState(15, 13, seed);
        HexCoordinate townHall = state.getTownHallPos();
        Hex center = state.getMap().getHex(townHall);
        require(center != null, seed, "Town Hall must be inside the map");
        require(passable(center), seed, "Town Hall must be on passable terrain");
        require(state.getPlayer().getBuildingAt(townHall) != null, seed,
                "Town Hall projection must occupy its authoritative position");

        int safeExits = 0;
        for (HexCoordinate neighbour : townHall.findNeighbours()) {
            Hex hex = state.getMap().getHex(neighbour);
            if (hex != null && passable(hex)) safeExits++;
            require(hex == null || (hex.getTerrainType() != Constants.TerrainType.SEA
                            && hex.getTerrainType() != Constants.TerrainType.MOUNTAIN_RANGE),
                    seed, "Town Hall cannot be encircled by sea or Mountain Range");
        }
        require(safeExits >= 4, seed, "Town Hall needs several independent starting exits");

        for (Unit unit : state.getPlayer().getUnits()) {
            Hex spawn = state.getMap().getHex(unit.getPosition());
            require(spawn != null && passable(spawn), seed,
                    "every starting unit must spawn on passable terrain");
        }
        for (Hex hex : state.getMap().getAllHexes()) {
            if (hex.getTerrainType() != Constants.TerrainType.MOUNTAIN_RANGE) continue;
            require(!hex.hasNaturalResource(), seed,
                    "Mountain Range must never contain natural resources");
            require(!hex.getHasBuilding() && state.getPlayer().getBuildingAt(hex.getCoordinate()) == null,
                    seed, "Mountain Range must never contain a building");
            require(!state.getMap().hasTradingPost(hex.getCoordinate()), seed,
                    "Mountain Range must never contain a Trading Post");
        }
        require(state.getTribes().size() == 5, seed,
                "safe generation must retain one camp for each tribe type");
    }

    private static boolean passable(Hex hex) {
        return hex.getTerrainType() != Constants.TerrainType.SEA
                && hex.getTerrainType() != Constants.TerrainType.MOUNTAIN_RANGE;
    }

    private static String signature(GameState state) {
        List<Hex> hexes = new ArrayList<>(state.getMap().getAllHexes());
        hexes.sort(Comparator.comparingInt((Hex h) -> h.getCoordinate().getQ())
                .thenComparingInt(h -> h.getCoordinate().getR()));
        StringBuilder result = new StringBuilder();
        for (Hex hex : hexes) {
            result.append(hex.getCoordinate().getQ()).append(',')
                    .append(hex.getCoordinate().getR()).append(':')
                    .append(hex.getTerrainType()).append(':')
                    .append(hex.getNaturalResources()).append(':')
                    .append(state.getMap().hasTradingPost(hex.getCoordinate())).append(';');
        }
        return result.toString();
    }

    private static void require(boolean condition, long seed, String message) {
        if (!condition) throw new AssertionError("seed " + seed + ": " + message);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
