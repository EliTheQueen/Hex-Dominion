import java.util.HashMap;
import java.util.Map;

import model.*;
import model.season.*;

public final class SeasonAndAdjacencyTest {
    public static void main(String[] args) {
        require(new SeasonCycle(1).getCurrentSeason() == Season.SPRING, "turn 1");
        require(new SeasonCycle(10).getCurrentSeason() == Season.SPRING, "turn 10");
        require(new SeasonCycle(11).getCurrentSeason() == Season.SUMMER, "turn 11");
        require(new SeasonCycle(20).getCurrentSeason() == Season.SUMMER, "turn 20");
        require(new SeasonCycle(21).getCurrentSeason() == Season.AUTUMN, "turn 21");
        require(new SeasonCycle(30).getCurrentSeason() == Season.AUTUMN, "turn 30");
        require(new SeasonCycle(31).getCurrentSeason() == Season.WINTER, "turn 31");
        require(new SeasonCycle(40).getCurrentSeason() == Season.WINTER, "turn 40");
        require(new SeasonCycle(41).getCurrentSeason() == Season.SPRING, "turn 41");
        require(SeasonProductionModifiers.apply(6, Season.SPRING, Constants.BuildingType.FARM) == 7,
                "spring farm");
        require(SeasonProductionModifiers.apply(6, Season.WINTER, Constants.BuildingType.FARM) == 5,
                "winter farm");

        HexCoordinate a = new HexCoordinate(0, 0);
        HexCoordinate b = new HexCoordinate(1, 0);
        Map<HexCoordinate, Hex> cells = new HashMap<>();
        cells.put(a, new Hex(a, Constants.TerrainType.PLAIN));
        cells.put(b, new Hex(b, Constants.TerrainType.PLAIN));
        GameMap map = new GameMap(cells);
        Player player = new Player("test");
        Building first = new Building(a, Constants.BuildingType.FARM);
        Building second = new Building(b, Constants.BuildingType.FARM);
        player.addBuilding(first);
        player.addBuilding(second);
        AdjacencyBonusService service = new AdjacencyBonusService();
        int total = service.calculateBonus(first, player, map) + service.calculateBonus(second, player, map);
        require(total == 1, "shared farm edge counted once");
        System.out.println("SeasonAndAdjacencyTest passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
