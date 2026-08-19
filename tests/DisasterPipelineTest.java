import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import model.Building;
import model.Constants;
import model.GameMap;
import model.GameState;
import model.Hex;
import model.HexCoordinate;
import model.Player;
import model.combat.CombatService;
import model.disaster.AvalancheEvent;
import model.disaster.DisasterEvent;
import model.disaster.DisasterGenerator;
import model.disaster.DisasterOccurrencePolicy;
import model.disaster.DisasterOriginSelector;
import model.disaster.DisasterPathBuilder;
import model.disaster.DisasterSelector;
import model.disaster.DisasterTargetCollector;
import model.disaster.DisasterType;
import model.disaster.TornadoEvent;
import model.disaster.VolcanicEruptionEvent;
import model.season.Season;

public final class DisasterPipelineTest {
    public static void main(String[] args) {
        beginningOfTurnUsesPreAdvanceSeasonAndKeepsFullBlockDuration();
        optionalPathDisastersDestroyRoadsAndUseCommonBuildingCleanup();
        System.out.println("DisasterPipelineTest passed");
    }

    private static void beginningOfTurnUsesPreAdvanceSeasonAndKeepsFullBlockDuration() {
        RecordingGenerator generator = new RecordingGenerator();
        GameState state = new GameState(15, 13, new Random(17), generator);
        state.getSeasonCycle().restoreTurn(10);

        int foodBefore = state.getPlayer().getResources().get(Constants.ResourceType.FOOD);
        state.endTurn();

        require(generator.seenSeason == Season.SPRING,
                "disaster eligibility must use the season at turn start");
        require(generator.foodWhenRolled == foodBefore,
                "disaster must roll before production and consumption");
        require(state.getSeasonCycle().getCurrentSeason() == Season.SUMMER,
                "season still advances after turn processing");
        require(generator.origin != null
                        && state.getMap().getHex(generator.origin).getBlockedTurns() == 3,
                "new disaster block must not lose a turn immediately");
        require(state.getLastTurnEvents().stream()
                        .anyMatch(message -> message.startsWith("Distant disaster reported:")
                                && !message.contains(generator.origin.toString())),
                "hidden disasters need a useful alert without leaking their location");

        state.endTurn();
        require(state.getMap().getHex(generator.origin).getBlockedTurns() == 2,
                "three-turn block must decrement once on the following turn");
        state.endTurn();
        require(state.getMap().getHex(generator.origin).getBlockedTurns() == 1,
                "three-turn block must persist for its third player turn");
        state.endTurn();
        require(!state.getMap().getHex(generator.origin).isBlocked(),
                "block must expire exactly after three full turns");
    }

    private static void optionalPathDisastersDestroyRoadsAndUseCommonBuildingCleanup() {
        PathFixture tornado = new PathFixture();
        Building tornadoFarm = tornado.addDamagedFarm(30);
        new TornadoEvent(tornado.path.get(0), tornado.map, tornado.player,
                new Random(1), tornado.builder, new DisasterTargetCollector()).start();
        tornado.assertRoadsRemoved("tornado");
        tornado.assertBuildingReleased(tornadoFarm, "tornado");

        PathFixture avalanche = new PathFixture();
        Building avalancheFarm = avalanche.addDamagedFarm(40);
        new AvalancheEvent(avalanche.path.get(0), avalanche.map, avalanche.player,
                new Random(2), avalanche.builder, new DisasterTargetCollector()).start();
        avalanche.assertRoadsRemoved("avalanche");
        avalanche.assertBuildingReleased(avalancheFarm, "avalanche");
        for (HexCoordinate coordinate : avalanche.path) {
            require(avalanche.map.getHex(coordinate).getBlockedTurns() == 1,
                    "avalanche must block its complete path for one turn");
        }

        PathFixture volcano = new PathFixture();
        Building volcanoFarm = volcano.addDamagedFarm(100);
        new VolcanicEruptionEvent(volcano.path.get(0), volcano.map, volcano.player,
                new Random(3), volcano.builder, new DisasterTargetCollector()).start();
        volcano.assertRoadsRemoved("volcano");
        volcano.assertBuildingReleased(volcanoFarm, "volcano");
        for (HexCoordinate coordinate : volcano.path) {
            require(volcano.map.getHex(coordinate).getBlockedTurns() == 3,
                    "volcano must block its complete lava path for three turns");
        }
    }

    private static final class RecordingGenerator extends DisasterGenerator {
        private Season seenSeason;
        private int foodWhenRolled;
        private HexCoordinate origin;
        private boolean generated;

        private RecordingGenerator() {
            super(new DisasterOccurrencePolicy(0), new DisasterSelector(),
                    new DisasterOriginSelector());
        }

        @Override
        public DisasterEvent generate(
                Season season,
                boolean navalSystemEnabled,
                boolean bearAttackAllowed,
                GameMap map,
                Player player,
                Random random,
                CombatService combatService
        ) {
            if (generated) {
                return null;
            }
            generated = true;
            seenSeason = season;
            foodWhenRolled = player.getResources().get(Constants.ResourceType.FOOD);
            origin = map.getAllHexes().stream()
                    .filter(hex -> !hex.isVisible())
                    .map(Hex::getCoordinate)
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("test map needs a hidden hex"));
            return new BlockingEvent(origin, map);
        }
    }

    private static final class BlockingEvent extends DisasterEvent {
        private final GameMap map;

        private BlockingEvent(HexCoordinate origin, GameMap map) {
            super(DisasterType.TORNADO, origin);
            this.map = map;
        }

        @Override
        protected void onStarted() {
            map.getHex(getOrigin()).blockForTurns(3);
        }
    }

    private static final class PathFixture {
        private final List<HexCoordinate> path = Arrays.asList(
                new HexCoordinate(0, 0),
                new HexCoordinate(1, 0),
                new HexCoordinate(2, 0)
        );
        private final GameMap map;
        private final Player player = new Player("disaster-test");
        private final DisasterPathBuilder builder;

        private PathFixture() {
            Map<HexCoordinate, Hex> cells = new HashMap<>();
            for (HexCoordinate coordinate : path) {
                cells.put(coordinate, new Hex(coordinate, Constants.TerrainType.PLAIN));
            }
            map = new GameMap(cells);
            for (HexCoordinate coordinate : path) {
                map.buildRoad(coordinate);
            }
            builder = new FixedPathBuilder(path);
        }

        private Building addDamagedFarm(int hitPoints) {
            HexCoordinate coordinate = path.get(1);
            Building building = new Building(coordinate, Constants.BuildingType.FARM);
            building.synchronizeHealth(hitPoints, 100);
            player.addBuilding(building);
            map.getHex(coordinate).setHasBuilding(true);
            return building;
        }

        private void assertRoadsRemoved(String disaster) {
            for (HexCoordinate coordinate : path) {
                require(!map.hasRoad(coordinate),
                        disaster + " must destroy every road on its path");
            }
        }

        private void assertBuildingReleased(Building building, String disaster) {
            require(building.isRuined()
                            && !player.getBuildings().contains(building)
                            && player.getBuildingAt(building.getPosition()) == null
                            && !map.getHex(building.getPosition()).getHasBuilding(),
                    disaster + " must use the authoritative building destruction lifecycle");
        }
    }

    private static final class FixedPathBuilder extends DisasterPathBuilder {
        private final List<HexCoordinate> path;

        private FixedPathBuilder(List<HexCoordinate> path) {
            this.path = path;
        }

        @Override
        public List<HexCoordinate> buildRandomPath(
                HexCoordinate origin,
                GameMap map,
                int length,
                Random random,
                boolean allowMountain
        ) {
            return path;
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
