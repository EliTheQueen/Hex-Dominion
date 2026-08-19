import java.util.HashMap;
import java.util.Map;

import model.*;

public final class InfrastructureRulesTest {
    public static void main(String[] args) {
        HexCoordinate a = new HexCoordinate(0, 0), b = new HexCoordinate(1, 0);
        Map<HexCoordinate, Hex> cells = new HashMap<>();
        cells.put(a, new Hex(a, Constants.TerrainType.PLAIN));
        cells.put(b, new Hex(b, Constants.TerrainType.SEA));
        cells.get(a).explore(true); cells.get(b).explore(true);
        GameMap map = new GameMap(cells);
        Player player = new Player("test"); player.expandTerritory(a);
        Builder builder = new Builder(a); player.addUnit(builder);
        InfrastructureService service = new InfrastructureService(map, player);
        require(service.buildRoad(builder, a), "land road");
        builder.resetAP(); builder.setPosition(b);
        require(!service.buildRoad(builder, b), "no sea road");
        builder.setPosition(a);
        require(!service.buildWall(builder, a, b), "no wall on sea boundary");

        Building farm = new Building(a, Constants.BuildingType.FARM);
        player.addBuilding(farm); cells.get(a).setHasBuilding(true);
        Worker worker = new Worker(a); player.addUnit(worker); worker.station(farm);
        builder.resetAP();
        require(service.demolishBuilding(builder, a), "demolition");
        require(!worker.isStationed() && player.getBuildingAt(a) == null, "workers released");
        System.out.println("InfrastructureRulesTest passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
