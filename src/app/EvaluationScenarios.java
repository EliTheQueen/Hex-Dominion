package app;

import java.nio.file.Files;
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
import model.Unit;
import model.Worker;
import model.combat.CombatReport;
import model.combat.CombatRequest;
import model.combat.CombatService;
import model.combat.DiceRoller;
import model.disaster.BearAttackEvent;
import model.disaster.DisasterGenerator;
import model.disaster.DisasterOccurrencePolicy;
import model.disaster.DisasterOriginSelector;
import model.disaster.DisasterSelector;
import model.disaster.DisasterTargetCollector;
import model.disaster.EarthquakeEvent;
import model.disaster.FloodEvent;
import model.disaster.area.RadiusDisasterAreaCalculator;
import model.happiness.HappinessEventType;
import model.happiness.HappinessLevel;
import model.military.Archer;
import model.military.MilitaryDamageHandler;
import model.military.MilitaryHex;
import model.military.Swordsman;
import model.save.SaveManager;
import model.save.SaveSlot;
import model.tribe.DiplomacyResult;
import model.tribe.Tribe;
import model.tribe.TribeType;
import model.tribe.mission.MissionActionResult;

/** Deterministic, command-line evaluator entry points for mandatory gameplay paths. */
public final class EvaluationScenarios {
    private static final long SEED = 20260819L;

    private EvaluationScenarios() {}

    public static void main(String[] args) throws Exception {
        List<String> scenarios = args.length == 0
                ? List.of("revolt", "mandatory-disasters", "tribe-war", "tribe-camp-defeat",
                        "mission-completion", "combat", "save-load")
                : List.of(args);
        for (String scenario : scenarios) run(scenario.toLowerCase());
        System.out.println("EVALUATION_SCENARIOS_OK count=" + scenarios.size());
    }

    private static void run(String scenario) throws Exception {
        switch (scenario) {
            case "revolt" -> revolt();
            case "mandatory-disasters", "disasters" -> mandatoryDisasters();
            case "tribe-war" -> tribeWar();
            case "tribe-camp-defeat" -> tribeCampDefeat();
            case "mission-completion" -> missionCompletion();
            case "combat" -> combat();
            case "save-load" -> saveLoad();
            default -> throw new IllegalArgumentException("Unknown scenario: " + scenario);
        }
    }

    private static void revolt() {
        GameState state = stableState(SEED);
        state.getHappinessService().applyEvent(HappinessEventType.MISSION_FAILED);
        require(state.getHappinessLevel() == HappinessLevel.REVOLT, "revolt threshold was not reached");
        state.endTurn();
        Worker worker = state.getPlayer().getUnits().stream()
                .filter(Worker.class::isInstance).map(Worker.class::cast).findFirst().orElseThrow();
        require(worker.getCurrentAP() < worker.getMaxAP(), "revolt did not penalize Worker AP");
        System.out.println("SCENARIO_OK revolt happiness=" + state.getHappinessScore()
                + " workerAP=" + worker.getCurrentAP());
    }

    private static void mandatoryDisasters() {
        GameState earthquakeState = stableState(SEED + 1);
        Building hall = earthquakeState.getPlayer().getBuildingAt(earthquakeState.getTownHallPos());
        int hpBefore = hall.getCurrentHp();
        new EarthquakeEvent(earthquakeState.getTownHallPos(), earthquakeState.getMap(),
                earthquakeState.getPlayer(), new RadiusDisasterAreaCalculator(2),
                new DisasterTargetCollector()).start();
        require(hall.getCurrentHp() == hpBefore - 50, "Earthquake Town Hall damage mismatch");

        Fixture flood = new Fixture();
        Building farm = flood.addFarm();
        new FloodEvent(flood.origin, flood.map, flood.player, new DisasterTargetCollector()).start();
        require(!flood.player.getBuildings().contains(farm)
                        && !flood.map.getHex(flood.origin).getHasBuilding(),
                "Flood did not complete building cleanup");

        Fixture bear = new Fixture();
        bear.map.getHex(bear.origin).setTerrainType(Constants.TerrainType.FOREST);
        Worker target = new Worker(bear.adjacent);
        bear.player.addUnit(target);
        CombatService combat = new CombatService(bear.map, bear.player,
                new DiceRoller(new Random(SEED + 2)), new MilitaryDamageHandler());
        BearAttackEvent attack = new BearAttackEvent(bear.origin, bear.map, bear.player, combat);
        attack.start();
        attack.processTurn();
        require(attack.getLastCombatReport() != null, "Bear did not use the combat engine");
        System.out.println("SCENARIO_OK mandatory-disasters earthquakeHP=" + hall.getCurrentHp()
                + " floodFarmRemoved=true bearCombat=true");
    }

    private static void tribeWar() {
        GameState state = stableState(SEED + 3);
        Tribe tribe = state.getTribes().get(0);
        tribe.discover();
        require(state.declareWar(tribe) == DiplomacyResult.SUCCESS
                        && tribe.getRelation().isEnemy(),
                "tribe war transition failed");
        state.endTurn();
        System.out.println("SCENARIO_OK tribe-war tribe=" + tribe.getName()
                + " relation=" + tribe.getRelation().getScore());
    }

    private static void tribeCampDefeat() {
        GameState state = stableState(SEED + 4);
        Tribe tribe = state.getTribes().get(0);
        tribe.discover();
        tribe.removeGuards(99);
        tribe.takeDamage(tribe.getCurrentHp() - 1);
        HexCoordinate attackerPosition = validNeighbour(state, tribe.getCampCoordinate());
        Swordsman attacker = new Swordsman(attackerPosition);
        state.getPlayer().addUnit(attacker);
        CombatReport report = state.attackTribeCamp(attacker, tribe);
        require(report != null && tribe.isOutpost()
                        && state.getPlayer().isInTerritory(tribe.getCampCoordinate()),
                "camp defeat did not create a player Outpost");
        System.out.println("SCENARIO_OK tribe-camp-defeat outpost=" + tribe.isOutpost()
                + " damage=" + report.getStructureDamage());
    }

    private static void missionCompletion() {
        GameState state = stableState(SEED + 5);
        Tribe farmer = find(state, TribeType.FARMER);
        farmer.discover();
        state.getMap().getHex(farmer.getCampCoordinate()).setVisible(true);
        farmer.getRelation().setScore(20);
        require(state.requestMission(farmer) == MissionActionResult.SUCCESS,
                "Farmer mission was not accepted");
        state.getMission(farmer).refreshCompletionState();
        require(state.turnInMission(farmer) == MissionActionResult.SUCCESS,
                "completed Farmer mission was not turned in");
        System.out.println("SCENARIO_OK mission-completion relation="
                + farmer.getRelation().getScore() + " cooldown=true");
    }

    private static void combat() {
        Fixture fixture = new Fixture();
        Swordsman sword = new Swordsman(fixture.origin);
        Archer archer = new Archer(fixture.origin);
        Swordsman defender = new Swordsman(fixture.adjacent);
        fixture.player.addUnit(sword);
        fixture.player.addUnit(archer);
        fixture.player.addUnit(defender);
        MilitaryHex attackers = new MilitaryHex(fixture.map.getHex(fixture.origin));
        attackers.addUnit(sword);
        attackers.addUnit(archer);
        MilitaryHex defenders = new MilitaryHex(fixture.map.getHex(fixture.adjacent));
        defenders.addUnit(defender);
        CombatReport report = new CombatService(fixture.map, fixture.player,
                new DiceRoller(new Random(SEED + 6)), new MilitaryDamageHandler())
                .resolve(CombatRequest.military(sword, attackers, defenders));
        require(report.getAttackerRolls().size() == 2
                        && sword.getCurrentAP() == sword.getMaxAP() - 1
                        && archer.getCurrentAP() == archer.getMaxAP() - 1,
                "combat composition or participant AP mismatch");
        System.out.println("SCENARIO_OK combat attackDice=" + report.getAttackerRolls()
                + " defenseDice=" + report.getDefenderRolls());
    }

    private static void saveLoad() throws Exception {
        GameState state = stableState(SEED + 7);
        state.endTurn();
        SaveManager manager = new SaveManager(Files.createTempDirectory("hex-evaluation-save"));
        require(manager.save(SaveSlot.MANUAL_1, "Evaluation", state), "evaluation save failed");
        GameState loaded = manager.load(SaveSlot.MANUAL_1).getGameState();
        require(loaded != null
                        && loaded.getCurrentTurn() == state.getCurrentTurn()
                        && loaded.getSeasonCycle().getCurrentSeason()
                        == state.getSeasonCycle().getCurrentSeason()
                        && loaded.getTownHall().getLevel() == state.getTownHall().getLevel()
                        && loaded.getTownHall().getCurrentHp() == state.getTownHall().getCurrentHp()
                        && loaded.getPlayer().getUnitCount() == state.getPlayer().getUnitCount()
                        && loaded.getPlayer().getBuildingCount() == state.getPlayer().getBuildingCount(),
                "save/load authoritative state mismatch");
        for (Constants.ResourceType type : Constants.ResourceType.values()) {
            require(loaded.getPlayer().getResources().get(type)
                            == state.getPlayer().getResources().get(type)
                            && loaded.getPlayer().getResources().getCap(type)
                            == state.getPlayer().getResources().getCap(type),
                    "save/load resource mismatch for " + type);
        }
        System.out.println("SCENARIO_OK save-load turn=" + loaded.getCurrentTurn()
                + " verified=true");
    }

    private static GameState stableState(long seed) {
        return new GameState(15, 13, new Random(seed),
                new DisasterGenerator(new DisasterOccurrencePolicy(0),
                        new DisasterSelector(), new DisasterOriginSelector()));
    }

    private static HexCoordinate validNeighbour(GameState state, HexCoordinate center) {
        for (HexCoordinate coordinate : center.findNeighbours()) {
            Hex hex = state.getMap().getHex(coordinate);
            if (hex != null && hex.getTerrainType() != Constants.TerrainType.SEA
                    && hex.getTerrainType() != Constants.TerrainType.MOUNTAIN_RANGE) return coordinate;
        }
        throw new IllegalStateException("scenario has no valid neighbour");
    }

    private static Tribe find(GameState state, TribeType type) {
        return state.getTribes().stream().filter(tribe -> tribe.getType() == type)
                .findFirst().orElseThrow();
    }

    private static final class Fixture {
        private final HexCoordinate origin = new HexCoordinate(0, 0);
        private final HexCoordinate adjacent = new HexCoordinate(1, 0);
        private final GameMap map;
        private final Player player = new Player("evaluation");

        private Fixture() {
            Map<HexCoordinate, Hex> cells = new HashMap<>();
            cells.put(origin, new Hex(origin, Constants.TerrainType.PLAIN));
            cells.put(adjacent, new Hex(adjacent, Constants.TerrainType.PLAIN));
            map = new GameMap(cells);
        }

        private Building addFarm() {
            Building farm = new Building(origin, Constants.BuildingType.FARM);
            player.addBuilding(farm);
            map.getHex(origin).setHasBuilding(true);
            return farm;
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
