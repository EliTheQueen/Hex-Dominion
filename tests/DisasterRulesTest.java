import java.util.HashMap;
import java.util.Map;
import model.*;
import model.disaster.*;
import model.disaster.area.RadiusDisasterAreaCalculator;

public final class DisasterRulesTest {
    public static void main(String[] args) {
        HexCoordinate center = new HexCoordinate(0, 0), next = new HexCoordinate(1, 0);
        Map<HexCoordinate, Hex> cells = new HashMap<>();
        cells.put(center, new Hex(center, Constants.TerrainType.PLAIN));
        cells.put(next, new Hex(next, Constants.TerrainType.PLAIN));
        GameMap map = new GameMap(cells); Player player = new Player("test");
        Building hall = new Building(center, Constants.BuildingType.TOWN_HALL); player.addBuilding(hall);
        Explorer explorer = new Explorer(next); player.addUnit(explorer);
        EarthquakeEvent quake = new EarthquakeEvent(center, map, player,
                new RadiusDisasterAreaCalculator(2), new DisasterTargetCollector());
        quake.start();
        require(hall.getCurrentHp() == 150, "earthquake town hall damage");
        require(explorer.getCurrentHp() == 90, "earthquake unit damage");

        Building farm = new Building(next, Constants.BuildingType.FARM); player.addBuilding(farm);
        map.buildRoad(next); explorer.resetAP();
        FloodEvent flood = new FloodEvent(center, map, player, new DisasterTargetCollector()); flood.start();
        require(farm.isRuined() && !map.hasRoad(next), "flood destroys farm and road");
        require(explorer.getCurrentAP() == 0 && explorer.getCurrentHp() == 70, "flood damage and AP");
        System.out.println("DisasterRulesTest passed");
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
