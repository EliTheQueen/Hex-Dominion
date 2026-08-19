import java.util.HashMap;
import java.util.Map;
import model.*;

public final class CoastalProductionTest {
    public static void main(String[] args) {
        HexCoordinate land = new HexCoordinate(0, 0), sea = new HexCoordinate(1, 0);
        Map<HexCoordinate, Hex> cells = new HashMap<>();
        cells.put(land, new Hex(land, Constants.TerrainType.PLAIN));
        cells.put(sea, new Hex(sea, Constants.TerrainType.SEA));
        cells.get(sea).addNaturalResource(Constants.NaturalResourceType.FISH, 20);
        GameMap map = new GameMap(cells); Player player = new Player("test");
        Building dock = new Building(land, Constants.BuildingType.DOCK); player.addBuilding(dock);
        dock.getWorkers().add(new Worker(land));
        require(dock.produce(false).get(Constants.ResourceType.FOOD) == 2, "dock base catch");
        require(new AdjacencyBonusService().calculateBonus(dock, player, map) == 2, "adjacent fish bonus");

        Building stable = new Building(land, Constants.BuildingType.STABLE);
        stable.getWorkers().add(new Worker(land));
        require(stable.produce(false).get(Constants.ResourceType.FOOD) == 5,
                "plain stable has meaningful production");
        System.out.println("CoastalProductionTest passed");
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
