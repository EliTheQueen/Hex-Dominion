import java.util.HashMap;
import java.util.Map;

import model.*;
import model.season.Season;

public final class MovementPolicyTest {
    public static void main(String[] args) {
        HexCoordinate a = new HexCoordinate(0, 0);
        HexCoordinate b = new HexCoordinate(1, 0);
        Map<HexCoordinate, Hex> cells = new HashMap<>();
        cells.put(a, new Hex(a, Constants.TerrainType.PLAIN));
        cells.put(b, new Hex(b, Constants.TerrainType.PLAIN));
        GameMap map = new GameMap(cells);

        require(new MovementPolicy(false, Season.SUMMER).edgeCost(map, a, b) == 1, "plain");
        map.addRiver(a, b);
        require(new MovementPolicy(false, Season.SUMMER).edgeCost(map, a, b) == 2, "river");
        map.buildBridge(a, b);
        require(new MovementPolicy(false, Season.SUMMER).edgeCost(map, a, b) == 1, "bridge");
        require(new MovementPolicy(false, Season.WINTER).edgeCost(map, a, b) == 2, "winter land");

        cells.get(b).setTerrainType(Constants.TerrainType.SEA);
        require(!new MovementPolicy(false, Season.SUMMER).canEnter(cells.get(b)), "sea locked");
        MovementPolicy sailing = new MovementPolicy(true, Season.AUTUMN);
        require(sailing.canEnter(cells.get(b)), "sailing unlocks sea");
        require(sailing.edgeCost(map, a, b) == 2, "autumn water");

        Explorer explorer = new Explorer(a);
        require(explorer.moveTo(map, b, new MovementPolicy(true, Season.SUMMER)), "embark");
        require(explorer.getCurrentAP() == 0, "embarking ends movement");
        System.out.println("MovementPolicyTest passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
