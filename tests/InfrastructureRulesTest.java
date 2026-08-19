import java.util.HashMap;
import java.util.Map;

import model.*;

public final class InfrastructureRulesTest {
    public static void main(String[] args) {
        HexCoordinate a = new HexCoordinate(0, 0), b = new HexCoordinate(1, 0), c = new HexCoordinate(0, 1);
        Map<HexCoordinate, Hex> cells = new HashMap<>();
        cells.put(a, new Hex(a, Constants.TerrainType.PLAIN));
        cells.put(b, new Hex(b, Constants.TerrainType.SEA));
        cells.put(c, new Hex(c, Constants.TerrainType.PLAIN));
        cells.get(a).explore(true); cells.get(b).explore(true); cells.get(c).explore(true);
        GameMap map = new GameMap(cells);
        Player player = new Player("test"); player.expandTerritory(a); player.expandTerritory(c);
        Builder builder = new Builder(a); player.addUnit(builder);
        InfrastructureService service = new InfrastructureService(map, player);
        require(service.buildRoad(builder, a), "land road");
        builder.resetAP(); builder.setPosition(b);
        require(!service.buildRoad(builder, b), "no sea road");
        builder.setPosition(a);
        require(!service.buildWall(builder, a, b), "no wall on sea boundary");

        player.spend(ResourceAmount.of(0, 0, 10, 0));
        builder.resetAP();
        int apBefore = builder.getCurrentAP();
        int woodBefore = player.getResources().get(Constants.ResourceType.WOOD);
        require(!service.buildWall(builder, a, c), "wall requires both Wood and Stone");
        require(builder.getCurrentAP() == apBefore
                        && player.getResources().get(Constants.ResourceType.WOOD) == woodBefore,
                "failed wall validation spends nothing");
        player.addResources(ResourceAmount.of(0, 0, 10, 0));
        int stoneBefore = player.getResources().get(Constants.ResourceType.STONE);
        require(service.buildWall(builder, a, c), "valid wall builds");
        require(builder.getCurrentAP() == apBefore - 1
                        && player.getResources().get(Constants.ResourceType.WOOD) == woodBefore - 10
                        && player.getResources().get(Constants.ResourceType.STONE) == stoneBefore - 10,
                "wall atomically consumes AP, Wood and Stone");
        require(map.getWallEdges().size() == 1, "built wall owns one edge");
        builder.resetAP();
        require(service.demolishWall(builder, a, c), "wall edge demolition");
        require(!map.hasWallBetween(a, c) && map.getWallEdges().isEmpty(),
                "wall demolition removes stale edge state");

        Building farm = new Building(a, Constants.BuildingType.FARM);
        player.addBuilding(farm); cells.get(a).setHasBuilding(true);
        Worker worker = new Worker(a); player.addUnit(worker); worker.station(farm);
        builder.setPosition(c);
        builder.resetAP();
        require(service.demolishBuilding(builder, a), "adjacent demolition");
        require(!worker.isStationed() && player.getBuildingAt(a) == null, "workers released");

        verifyMonumentTerrainRule();
        System.out.println("InfrastructureRulesTest passed");
    }

    private static void verifyMonumentTerrainRule() {
        GameState state = new GameState(15, 13);
        Hex candidate = null;
        for (HexCoordinate coordinate : state.getPlayer().getTerritory()) {
            Hex hex = state.getMap().getHex(coordinate);
            if (hex != null && state.getPlayer().getBuildingAt(coordinate) == null
                    && !hex.everHadResource() && hex.getIsExplored()) {
                candidate = hex;
                break;
            }
        }
        require(candidate != null, "test map has an empty explored territory hex");
        candidate.setTerrainType(Constants.TerrainType.GRASSLAND);
        require(!state.canBuildAt(candidate.getCoordinate(), Constants.BuildingType.MONUMENT),
                "Monument is forbidden on Grassland");
        candidate.setTerrainType(Constants.TerrainType.PLAIN);
        require(state.canBuildAt(candidate.getCoordinate(), Constants.BuildingType.MONUMENT),
                "Monument is allowed on resource-free Plain");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
