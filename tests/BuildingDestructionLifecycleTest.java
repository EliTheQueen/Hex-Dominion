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
import model.ResourceAmount;
import model.Worker;
import model.combat.CombatRequest;
import model.combat.CombatService;
import model.combat.DiceRoller;
import model.disaster.DisasterGenerator;
import model.disaster.DisasterOccurrencePolicy;
import model.disaster.DisasterOriginSelector;
import model.disaster.DisasterPathBuilder;
import model.disaster.DisasterSelector;
import model.disaster.DisasterTargetCollector;
import model.disaster.FloodEvent;
import model.disaster.TsunamiEvent;
import model.disaster.VolcanicEruptionEvent;
import model.military.MilitaryDamageHandler;
import model.military.MilitaryHex;
import model.military.Swordsman;

/** Every destructive subsystem must converge on the same complete cleanup. */
public final class BuildingDestructionLifecycleTest {
    public static void main(String[] args) {
        floodCleanup();
        tsunamiCleanup();
        volcanoCleanup();
        militaryCleanup();
        unpaidUpkeepCleanupAndRebuildability();
        System.out.println("BuildingDestructionLifecycleTest passed");
    }

    private static void floodCleanup() {
        Fixture fixture = new Fixture();
        Building farm = fixture.addFarmWithWorker(100);
        new FloodEvent(fixture.site, fixture.map, fixture.player,
                new DisasterTargetCollector()).start();
        fixture.assertReleased(farm, "Flood");
    }

    private static void tsunamiCleanup() {
        Fixture fixture = new Fixture();
        Building farm = fixture.addFarmWithWorker(100);
        new TsunamiEvent(fixture.site, fixture.map, fixture.player,
                new DisasterTargetCollector(), new Random(12)).start();
        fixture.assertReleased(farm, "Tsunami");
    }

    private static void volcanoCleanup() {
        Fixture fixture = new Fixture();
        Building farm = fixture.addFarmWithWorker(100);
        DisasterPathBuilder fixedPath = new DisasterPathBuilder() {
            @Override
            public List<HexCoordinate> buildRandomPath(HexCoordinate origin, GameMap map,
                                                       int length, Random random,
                                                       boolean allowMountain) {
                return List.of(fixture.site);
            }
        };
        new VolcanicEruptionEvent(fixture.site, fixture.map, fixture.player,
                new Random(13), fixedPath, new DisasterTargetCollector()).start();
        fixture.assertReleased(farm, "Volcano");
    }

    private static void militaryCleanup() {
        Fixture fixture = new Fixture();
        Building farm = fixture.addFarmWithWorker(10);
        Swordsman sword = new Swordsman(fixture.attacker);
        fixture.player.addUnit(sword);
        MilitaryHex attackers = new MilitaryHex(fixture.map.getHex(fixture.attacker));
        attackers.addUnit(sword);
        new CombatService(fixture.map, fixture.player, new DiceRoller(new Random(14)),
                new MilitaryDamageHandler()).resolve(
                        CombatRequest.building(sword, attackers, farm, null));
        fixture.assertReleased(farm, "Military attack");
    }

    private static void unpaidUpkeepCleanupAndRebuildability() {
        DisasterGenerator none = new DisasterGenerator(new DisasterOccurrencePolicy(0),
                new DisasterSelector(), new DisasterOriginSelector());
        GameState state = new GameState(15, 13, new Random(15), none);
        HexCoordinate site = state.getTownHallPos().findNeighbours().get(0);
        Hex hex = state.getMap().getHex(site);
        hex.setTerrainType(Constants.TerrainType.PLAIN);
        hex.addNaturalResource(Constants.NaturalResourceType.STONE, 100);
        state.getPlayer().expandTerritory(site);
        state.getPlayer().applyTech(Constants.TechnologyType.STONE_MINING);
        Building mine = new Building(site, Constants.BuildingType.STONE_MINE);
        state.getPlayer().addBuilding(mine);
        hex.setHasBuilding(true);
        Worker worker = new Worker(site);
        state.getPlayer().addUnit(worker);
        require(worker.station(mine), "worker must station before unpaid-upkeep cleanup");
        mine.blockProductionForTurns(Constants.UPKEEP_GRACE_TURNS + 1);
        int stone = state.getPlayer().getResources().get(Constants.ResourceType.STONE);
        require(state.getPlayer().spend(ResourceAmount.of(0, 0, stone, 0)),
                "fixture must exhaust Stone upkeep resource");

        for (int i = 0; i < Constants.UPKEEP_GRACE_TURNS; i++) state.endTurn();
        require(!state.getPlayer().getBuildings().contains(mine)
                        && !hex.getHasBuilding() && !worker.isStationed(),
                "unpaid upkeep must remove the building, free its hex, and release its Worker");
        require(state.canBuildAt(site, Constants.BuildingType.STONE_MINE),
                "the exact site must be buildable again after upkeep destruction");
    }

    private static final class Fixture {
        private final HexCoordinate site = new HexCoordinate(0, 0);
        private final HexCoordinate attacker = new HexCoordinate(1, 0);
        private final GameMap map;
        private final Player player = new Player("destruction-test");
        private Worker worker;

        private Fixture() {
            Map<HexCoordinate, Hex> cells = new HashMap<>();
            cells.put(site, new Hex(site, Constants.TerrainType.PLAIN));
            cells.put(attacker, new Hex(attacker, Constants.TerrainType.PLAIN));
            map = new GameMap(cells);
        }

        private Building addFarmWithWorker(int hp) {
            Building building = new Building(site, Constants.BuildingType.FARM);
            building.synchronizeHealth(hp, 100);
            player.addBuilding(building);
            map.getHex(site).setHasBuilding(true);
            worker = new Worker(site);
            player.addUnit(worker);
            require(worker.station(building), "worker must station before destruction");
            return building;
        }

        private void assertReleased(Building building, String cause) {
            require(!player.getBuildings().contains(building)
                            && player.getBuildingAt(site) == null,
                    cause + " must remove the building from Player state");
            require(!map.getHex(site).getHasBuilding() && map.getHex(site).canHoldBuilding(),
                    cause + " must free map occupancy");
            require(worker != null && !worker.isStationed() && building.getWorkers().isEmpty(),
                    cause + " must release every stationed Worker");
            Building replacement = new Building(site, Constants.BuildingType.FARM);
            player.addBuilding(replacement);
            map.getHex(site).setHasBuilding(true);
            require(player.getBuildingAt(site) == replacement,
                    cause + " must allow a replacement building on the same hex");
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
