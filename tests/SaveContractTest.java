import java.io.BufferedOutputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import controller.GameController;
import model.Building;
import model.Constants;
import model.GameMap;
import model.GameState;
import model.Hex;
import model.HexCoordinate;
import model.Player;
import model.ResourceAmount;
import model.Unit;
import model.Worker;
import model.combat.CombatReport;
import model.combat.CombatService;
import model.disaster.Bear;
import model.disaster.DisasterEvent;
import model.disaster.DisasterGenerator;
import model.disaster.DisasterOccurrencePolicy;
import model.disaster.DisasterOriginSelector;
import model.disaster.DisasterSelector;
import model.military.Swordsman;
import model.save.SaveAvailability;
import model.save.SaveLoadResult;
import model.save.SaveManager;
import model.save.SaveSlot;
import model.save.SaveSnapshot;
import model.save.SaveWriteResult;
import model.season.Season;
import model.townhall.ProductionCommand;
import model.tribe.DiplomacyResult;
import model.tribe.Tribe;
import model.tribe.TribeType;
import model.tribe.mission.MissionActionResult;
import model.tribe.mission.TribeMission;

public final class SaveContractTest {
    public static void main(String[] args) throws Exception {
        stableGuardAndAutosavePreservation();
        autosaveFailureIsSurfacedByController();
        migrationAndDeepValidationPrecedeReplacement();
        deepRoundTripAndRngContinuation();
        System.out.println("SaveContractTest passed");
    }

    private static void stableGuardAndAutosavePreservation() throws Exception {
        Path directory = Files.createTempDirectory("hex-save-stability");
        SaveManager manager = new SaveManager(directory);
        GameState state = new GameState(15, 13);
        SaveAvailability stable = state.getSaveAvailability();
        require(stable.isEnabled() && !stable.getReason().isBlank(),
                "stable state exposes an enabled reason");
        require(manager.save(SaveSlot.AUTOSAVE, "Healthy", state),
                "healthy autosave must be created");
        Path autosave = directory.resolve(SaveSlot.AUTOSAVE.getFileName());
        byte[] healthyBytes = Files.readAllBytes(autosave);

        state.beginCombatPresentation();
        SaveAvailability combat = state.getSaveAvailability();
        SaveWriteResult rejected = manager.saveWithResult(SaveSlot.AUTOSAVE, "Blocked", state);
        require(!combat.isEnabled() && combat.getReason().toLowerCase().contains("combat")
                        && !rejected.isSuccessful(),
                "combat presentation must disable saving with an exact reason");
        require(java.util.Arrays.equals(healthyBytes, Files.readAllBytes(autosave)),
                "rejected autosave must retain prior healthy bytes");
        state.endCombatPresentation();

        GameController progressController = new GameController(manager);
        progressController.startNewGame();
        require(progressController.hasUnsavedProgress(),
                "a new in-game session must warn before load");
        require(progressController.saveGame(SaveSlot.MANUAL_1, "Progress baseline")
                        && !progressController.hasUnsavedProgress(),
                "successful save establishes the unsaved-progress baseline");
        require(progressController.getGameState().getPlayer().spend(
                        ResourceAmount.of(1, 0, 0, 0))
                        && progressController.hasUnsavedProgress(),
                "later model mutation re-enables the in-game load warning");

        EndTurnProbeGenerator generator = new EndTurnProbeGenerator(manager);
        GameState probed = new GameState(15, 13, new Random(31), generator);
        generator.state = probed;
        probed.endTurn();
        require(generator.availability != null && !generator.availability.isEnabled()
                        && generator.availability.getReason().toLowerCase().contains("end-of-turn")
                        && generator.saveResult != null && !generator.saveResult.isSuccessful(),
                "end-turn transaction must reject a concurrent save before file I/O");
    }

    private static void autosaveFailureIsSurfacedByController() throws Exception {
        Path root = Files.createTempDirectory("hex-autosave-failure");
        Path notDirectory = root.resolve("not-a-directory");
        Files.writeString(notDirectory, "occupied");
        GameController controller = new GameController(new SaveManager(notDirectory));
        controller.startNewGame();
        controller.onEndTurnClicked();
        require(controller.getStatusMessage().startsWith("Autosave failed:")
                        && controller.getStatusMessage().contains("preserved"),
                "autosave failure must be visible to the player and mention preservation");
    }

    private static void migrationAndDeepValidationPrecedeReplacement() throws Exception {
        Path directory = Files.createTempDirectory("hex-save-migration");
        SaveManager manager = new SaveManager(directory);
        GameState legacy = new GameState(15, 13);
        writeEnvelope(directory.resolve(SaveSlot.MANUAL_1.getFileName()), legacy, 2);
        SaveLoadResult migrated = manager.load(SaveSlot.MANUAL_1);
        require(migrated.isSuccessful()
                        && migrated.getGameState().getSaveAvailability().isEnabled(),
                "compatible version-2 envelope must run through the explicit migration");

        GameState invalid = new GameState(15, 13);
        HexCoordinate badPosition = firstFreeHex(invalid);
        invalid.getPlayer().addBuilding(
                new Building(badPosition, Constants.BuildingType.FARM));
        // Deliberately omit the Hex occupancy flag: shallow validation would miss this.
        writeEnvelope(directory.resolve(SaveSlot.MANUAL_2.getFileName()), invalid, 2);

        GameController controller = new GameController(manager);
        controller.startNewGame();
        GameState live = controller.getGameState();
        SaveLoadResult corrupt = controller.loadGame(SaveSlot.MANUAL_2);
        require(!corrupt.isSuccessful() && controller.getGameState() == live,
                "deep validation must reject the detached candidate before live replacement");
        require(manager.preview(SaveSlot.MANUAL_2).isCorrupted(),
                "deeply invalid graph must be marked corrupted in preview");
    }

    private static void deepRoundTripAndRngContinuation() throws Exception {
        Path directory = Files.createTempDirectory("hex-save-deep");
        SaveManager manager = new SaveManager(directory);
        GameState original = new GameState(15, 13, new Random(947),
                new DisasterGenerator(new DisasterOccurrencePolicy(0),
                        new DisasterSelector(), new DisasterOriginSelector()));
        Player player = original.getPlayer();
        player.addResources(ResourceAmount.of(60, 70, 90, 80));

        HexCoordinate hall = original.getTownHallPos();
        HexCoordinate farmPosition = hall.findNeighbours().stream()
                .filter(original.getMap()::containsCoordinate).findFirst().orElseThrow();
        Building farm = new Building(farmPosition, Constants.BuildingType.FARM);
        player.addBuilding(farm);
        original.getMap().getHex(farmPosition).setHasBuilding(true);
        original.getMap().buildRoad(hall);
        original.getMap().buildRoad(farmPosition);
        original.getMap().buildWall(hall, farmPosition);
        Worker worker = player.getUnits().stream().filter(Worker.class::isInstance)
                .map(Worker.class::cast).findFirst().orElseThrow();
        worker.setPosition(farmPosition);
        require(worker.station(farm), "worker stations for reference round trip");
        Swordsman hunter = new Swordsman(hall);
        player.addUnit(hunter);

        Tribe cooldownTribe = findTribe(original, TribeType.FARMER);
        cooldownTribe.discover();
        cooldownTribe.getRelation().setScore(20);
        require(original.requestMission(cooldownTribe) == MissionActionResult.SUCCESS
                        && original.cancelMission(cooldownTribe) == MissionActionResult.SUCCESS,
                "mission cooldown fixture");
        Tribe activeTribe = findTribe(original, TribeType.MERCHANT);
        activeTribe.discover();
        activeTribe.getRelation().setScore(70);
        require(original.requestMission(activeTribe) == MissionActionResult.SUCCESS
                        && original.requestAlliance(activeTribe) == DiplomacyResult.SUCCESS,
                "mission/alliance fixture");
        firstFreeHex(original).findNeighbours();
        original.getMap().getHex(firstFreeHex(original)).blockForTurns(2);
        require(original.startTownHallUpgrade() != null, "Town Hall timer fixture");
        original.prepareAfterLoad();
        original.validatePersistentState();

        String before = canonicalState(original);
        require(manager.save(SaveSlot.MANUAL_1, "Deep state", original),
                "deep graph saves");
        GameState loadedA = manager.load(SaveSlot.MANUAL_1).getGameState();
        GameState loadedB = manager.load(SaveSlot.MANUAL_1).getGameState();
        require(before.equals(canonicalState(loadedA)),
                "turn, season, economy, entities, fog, infrastructure, diplomacy, missions and timers round-trip");
        require(SaveSnapshot.fingerprint(loadedA).equals(SaveSnapshot.fingerprint(loadedB)),
                "loading the same snapshot twice is semantically reproducible");

        Swordsman loadedHunterA = findSwordsmanAt(loadedA, hall);
        Swordsman loadedHunterB = findSwordsmanAt(loadedB, hall);
        HexCoordinate bearPosition = hall.findNeighbours().stream()
                .filter(loadedA.getMap()::containsCoordinate)
                .filter(coordinate -> !coordinate.equals(farmPosition))
                .findFirst().orElseThrow();
        CombatReport reportA = loadedA.attackBear(loadedHunterA,
                new Bear(bearPosition, bearPosition));
        CombatReport reportB = loadedB.attackBear(loadedHunterB,
                new Bear(bearPosition, bearPosition));
        require(reportA != null && reportB != null
                        && reportA.getAttackerRolls().equals(reportB.getAttackerRolls())
                        && reportA.getDefenderRolls().equals(reportB.getDefenderRolls()),
                "RNG continuation after load must be deterministic and must not reroll during restoration");
    }

    private static String canonicalState(GameState state) throws Exception {
        StringBuilder out = new StringBuilder();
        out.append(state.getCurrentTurn()).append('|')
                .append(state.getSeasonCycle().getCurrentTurn()).append('|')
                .append(state.getSeasonCycle().getCurrentSeason()).append('|')
                .append(state.getTownHall().getLevel()).append('|')
                .append(state.getTownHall().getCurrentHp()).append('|')
                .append(state.getTownHall().getCommandSlot().getActiveCommand() == null ? "none"
                        : commandText(state.getTownHall().getCommandSlot().getActiveCommand()));
        for (Constants.ResourceType type : Constants.ResourceType.values()) {
            out.append('|').append(type).append(':')
                    .append(state.getPlayer().getResources().get(type)).append('/')
                    .append(state.getPlayer().getResources().getCap(type));
        }

        List<String> hexes = new ArrayList<>();
        for (Hex hex : state.getMap().getAllHexes()) {
            hexes.add(coordinate(hex.getCoordinate()) + ':' + hex.getTerrainType() + ':'
                    + hex.getIsExplored() + ':' + hex.isVisible() + ':' + hex.hasRoad()
                    + ':' + hex.getBlockedTurns() + ':' + hex.getHasBuilding());
        }
        hexes.sort(String::compareTo);
        out.append("|HEX=").append(hexes);

        List<String> units = new ArrayList<>();
        for (Unit unit : state.getPlayer().getUnits()) {
            String stationed = unit instanceof Worker && ((Worker) unit).isStationed()
                    ? coordinate(((Worker) unit).getStationedAt().getPosition()) : "-";
            units.add(unit.getClass().getSimpleName() + ':' + coordinate(unit.getPosition())
                    + ':' + unit.getCurrentHp() + '/' + unit.getMaxHp() + ':'
                    + unit.getCurrentAP() + '/' + unit.getMaxAP() + ':' + unit.getState()
                    + ':' + stationed);
        }
        units.sort(String::compareTo);
        out.append("|UNIT=").append(units);

        List<String> buildings = new ArrayList<>();
        for (Building building : state.getPlayer().getBuildings()) {
            buildings.add(building.getType() + ":" + coordinate(building.getPosition())
                    + ':' + building.getCurrentHp() + '/' + building.getMaxHp()
                    + ":W" + building.getWorkerCount() + ":B" + building.isProductionBlocked());
        }
        buildings.sort(String::compareTo);
        out.append("|BUILD=").append(buildings);

        List<String> walls = new ArrayList<>();
        state.getMap().getWallEdges().forEach(edge -> walls.add(
                normalizedEdge(edge.getFirst(), edge.getSecond()) + ':'
                        + edge.getWall().getCurrentHp()));
        walls.sort(String::compareTo);
        out.append("|WALL=").append(walls);

        List<String> tribes = new ArrayList<>();
        for (Tribe tribe : state.getTribes()) {
            TribeMission mission = state.getMission(tribe);
            tribes.add(tribe.getId() + ':' + tribe.getType() + ':' + coordinate(tribe.getCampCoordinate())
                    + ':' + tribe.getCurrentHp() + ':' + tribe.isDiscovered() + ':'
                    + tribe.getRelation().getScore() + ':' + state.isAllied(tribe) + ':'
                    + tribe.getGuardCount() + ':' + (mission == null ? "none"
                    : mission.getId() + '/' + mission.getStatus() + '/' + mission.getRemainingTurns()));
        }
        tribes.sort(String::compareTo);
        out.append("|TRIBE=").append(tribes);
        out.append("|MCOOL=").append(sortedPrivateMap(state, "tribeMissionCooldownUntil"));
        out.append("|MFAIL=").append(sortedPrivateMap(state, "tribeMissionFailureTurn"));
        out.append("|FORBID=").append(sortedPrivateMap(state, "tribeForbiddenZoneTurns"));
        out.append("|TRADE=").append(sortedPrivateMap(state, "tribeTradeOfferTurn"));
        out.append("|BEAR=").append(sortedPrivateMap(state, "bearAreaCooldownUntil"));
        return out.toString();
    }

    private static String commandText(ProductionCommand command) {
        return command.getClass().getSimpleName() + ':' + command.getRemainingTurns()
                + '/' + command.getTotalTurns();
    }

    private static List<String> sortedPrivateMap(GameState state, String name) throws Exception {
        Field field = GameState.class.getDeclaredField(name);
        field.setAccessible(true);
        Map<?, ?> map = (Map<?, ?>) field.get(state);
        List<String> values = new ArrayList<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Object key = entry.getKey();
            values.add((key instanceof HexCoordinate ? coordinate((HexCoordinate) key) : key)
                    + "=" + entry.getValue());
        }
        values.sort(String::compareTo);
        return values;
    }

    private static void writeEnvelope(Path path, GameState state, int version) throws Exception {
        Class<?> type = Class.forName("model.save.SaveManager$SaveEnvelope");
        Constructor<?> constructor = type.getDeclaredConstructor(String.class, GameState.class, int.class);
        constructor.setAccessible(true);
        Object envelope = constructor.newInstance("Legacy fixture", state, version);
        try (ObjectOutputStream output = new ObjectOutputStream(
                new BufferedOutputStream(Files.newOutputStream(path)))) {
            output.writeObject(envelope);
        }
    }

    private static HexCoordinate firstFreeHex(GameState state) {
        return state.getMap().getAllHexes().stream()
                .map(Hex::getCoordinate)
                .filter(coordinate -> state.getPlayer().getBuildingAt(coordinate) == null)
                .min(Comparator.comparingInt(HexCoordinate::getQ)
                        .thenComparingInt(HexCoordinate::getR)).orElseThrow();
    }

    private static Tribe findTribe(GameState state, TribeType type) {
        return state.getTribes().stream().filter(tribe -> tribe.getType() == type)
                .findFirst().orElseThrow();
    }

    private static Swordsman findSwordsmanAt(GameState state, HexCoordinate coordinate) {
        return state.getPlayer().getUnits().stream().filter(Swordsman.class::isInstance)
                .map(Swordsman.class::cast).filter(unit -> unit.getPosition().equals(coordinate))
                .findFirst().orElseThrow();
    }

    private static String normalizedEdge(HexCoordinate first, HexCoordinate second) {
        String a = coordinate(first), b = coordinate(second);
        return a.compareTo(b) <= 0 ? a + '-' + b : b + '-' + a;
    }

    private static String coordinate(HexCoordinate coordinate) {
        return coordinate.getQ() + "," + coordinate.getR();
    }

    private static final class EndTurnProbeGenerator extends DisasterGenerator {
        private final SaveManager manager;
        private GameState state;
        private SaveAvailability availability;
        private SaveWriteResult saveResult;

        private EndTurnProbeGenerator(SaveManager manager) {
            super(new DisasterOccurrencePolicy(0), new DisasterSelector(),
                    new DisasterOriginSelector());
            this.manager = manager;
        }

        @Override
        public DisasterEvent generate(Season season, boolean navalSystemEnabled,
                                      boolean bearAttackAllowed, GameMap map, Player player,
                                      Random random, CombatService combatService) {
            availability = state.getSaveAvailability();
            saveResult = manager.saveWithResult(SaveSlot.MANUAL_3, "During turn", state);
            return null;
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
