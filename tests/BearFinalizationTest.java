import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import model.Constants;
import model.GameMap;
import model.GameState;
import model.Hex;
import model.HexCoordinate;
import model.Player;
import model.Unit;
import model.Worker;
import model.combat.CombatService;
import model.combat.DiceResult;
import model.combat.DiceRoller;
import model.disaster.Bear;
import model.disaster.BearActivity;
import model.disaster.BearAttackEvent;
import model.disaster.DisasterEvent;
import model.disaster.DisasterGenerator;
import model.disaster.DisasterOccurrencePolicy;
import model.disaster.DisasterOriginSelector;
import model.disaster.DisasterSelector;
import model.disaster.DisasterStatus;
import model.military.MilitaryDamageHandler;
import model.military.Swordsman;
import model.season.Season;

public final class BearFinalizationTest {
    public static void main(String[] args) {
        preservesStatsAndCivilianFirstSingleAttack();
        returnsToDenBeforeCleanupAndExposesAnimationState();
        cooldownIsFiveTurnsAndAreaSpecific();
        System.out.println("BearFinalizationTest passed");
    }

    private static void preservesStatsAndCivilianFirstSingleAttack() {
        HexCoordinate den = new HexCoordinate(0, 0);
        HexCoordinate adjacent = new HexCoordinate(1, 0);
        GameMap map = lineMap(2);
        map.getHex(den).setTerrainType(Constants.TerrainType.FOREST);
        Player player = new Player("bear-priority");
        Worker civilian = new Worker(adjacent);
        Swordsman military = new Swordsman(adjacent);
        int militaryHpBefore = military.getCurrentHp();
        player.addUnit(civilian);
        player.addUnit(military);
        CountingRoller roller = new CountingRoller(
                Arrays.asList(6), Arrays.asList(1, 1));
        BearAttackEvent event = new BearAttackEvent(den, map, player,
                new CombatService(map, player, roller, new MilitaryDamageHandler()));

        event.start();
        Bear bear = event.getBears().get(0);
        require(bear.getMaxHp() == 120 && bear.getCurrentHp() == 120,
                "bear HP must remain 120");
        require(bear.getAttackDamage() == 35 && bear.getAttackRange() == 1
                        && bear.getMaxAP() == 2,
                "bear damage/range/AP must remain 35/1/2");

        event.processTurn();
        require(civilian.getCurrentHp() == 65
                        && military.getCurrentHp() == militaryHpBefore,
                "civilian is targeted before military even on the same hex");
        require(roller.calls.equals(Arrays.asList(1, 2))
                        && bear.getCurrentAP() == 1,
                "one shared combat resolution and at most one attack occur per turn");
    }

    private static void returnsToDenBeforeCleanupAndExposesAnimationState() {
        HexCoordinate den = new HexCoordinate(0, 0);
        HexCoordinate targetPosition = new HexCoordinate(3, 0);
        GameMap map = lineMap(4);
        map.getHex(den).setTerrainType(Constants.TerrainType.FOREST);
        Player player = new Player("bear-return");
        Worker target = new Worker(targetPosition);
        player.addUnit(target);
        CountingRoller roller = new CountingRoller(
                Arrays.asList(6), Arrays.asList(1, 1));
        BearAttackEvent event = new BearAttackEvent(den, map, player,
                new CombatService(map, player, roller, new MilitaryDamageHandler()));
        event.start();
        Bear bear = event.getBears().get(0);

        event.processTurn();
        require(bear.getPosition().equals(new HexCoordinate(2, 0))
                        && target.getCurrentHp() == 100,
                "bear spends its two AP moving and does not gain a free attack");
        event.processTurn();
        require(target.getCurrentHp() == 65 && bear.getActivity() == BearActivity.ATTACKING,
                "attack state remains visible after common combat resolves");

        player.removeUnit(target);
        event.processTurn();
        require(bear.getPosition().equals(den) && bear.hasReturnedToDen()
                        && !event.shouldEnd(),
                "event remains active for a visible arrival at the den");
        event.processTurn();
        require(event.shouldEnd(), "event becomes completable after the return frame");
        event.complete();
        require(event.getBears().isEmpty() && event.getLastCombatReport() == null,
                "completion releases bears and combat references");

        Bear damaged = new Bear(den, den);
        damaged.takeDamage(20);
        require(damaged.getActivity() == BearActivity.DAMAGED
                        && damaged.getDamageRevision() == 1 && damaged.getCurrentHp() == 100,
                "damage exposes a monotonic animation revision and state");
    }

    private static void cooldownIsFiveTurnsAndAreaSpecific() {
        SequencedBearGenerator generator = new SequencedBearGenerator();
        GameState state = new GameState(15, 13, new Random(21), generator);

        state.endTurn();
        HexCoordinate firstDen = generator.firstDen;
        HexCoordinate remoteDen = generator.remoteDen;
        require(state.getActiveBearAttack() != null,
                "first bear event starts normally");
        require(!state.isBearAttackAreaAvailable(firstDen)
                        && !state.isBearAttackAreaAvailable(new HexCoordinate(1, 0)),
                "the den and its local hunt area enter cooldown");
        require(state.isBearAttackAreaAvailable(remoteDen),
                "a remote forest area is not globally disabled");

        state.endTurn();
        require(state.getActiveBearAttack() == null
                        && generator.firstEvent.getStatus() == DisasterStatus.COMPLETED
                        && generator.firstEvent.getBears().isEmpty(),
                "returned event completes and cleans itself up");

        for (int expectedTurn = 3; expectedTurn <= 6; expectedTurn++) {
            require(state.getCurrentTurn() == expectedTurn,
                    "test turn sequence must stay deterministic");
            require(!state.isBearAttackAreaAvailable(firstDen),
                    "same area remains unavailable through five subsequent turns");
            state.endTurn();
        }
        require(state.getCurrentTurn() == 7 && state.isBearAttackAreaAvailable(firstDen),
                "same area becomes available only after five complete turns");
    }

    private static GameMap lineMap(int length) {
        Map<HexCoordinate, Hex> cells = new HashMap<>();
        for (int q = 0; q < length; q++) {
            HexCoordinate coordinate = new HexCoordinate(q, 0);
            cells.put(coordinate, new Hex(coordinate, Constants.TerrainType.PLAIN));
        }
        return new GameMap(cells);
    }

    private static final class CountingRoller extends DiceRoller {
        private final List<List<Integer>> results = new ArrayList<>();
        private final List<Integer> calls = new ArrayList<>();
        private int next;

        @SafeVarargs
        private CountingRoller(List<Integer>... results) {
            this.results.addAll(Arrays.asList(results));
        }

        @Override
        public DiceResult rollD6(int count) {
            calls.add(count);
            if (next >= results.size()) {
                List<Integer> fallback = new ArrayList<>();
                for (int i = 0; i < count; i++) fallback.add(1);
                return new DiceResult(fallback);
            }
            List<Integer> result = results.get(next++);
            require(result.size() == count, "scripted dice count must match request");
            return new DiceResult(result);
        }
    }

    private static final class SequencedBearGenerator extends DisasterGenerator {
        private final HexCoordinate firstDen = new HexCoordinate(0, 0);
        private final HexCoordinate remoteDen = new HexCoordinate(14, 12);
        private BearAttackEvent firstEvent;
        private boolean generated;

        private SequencedBearGenerator() {
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
            if (generated) return null;
            generated = true;
            map.getHex(firstDen).setTerrainType(Constants.TerrainType.FOREST);
            map.getHex(remoteDen).setTerrainType(Constants.TerrainType.FOREST);
            firstEvent = new BearAttackEvent(firstDen, map, player, combatService);
            return firstEvent;
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
