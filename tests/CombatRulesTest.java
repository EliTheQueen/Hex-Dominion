import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import model.Building;
import model.Constants;
import model.GameMap;
import model.Hex;
import model.HexCoordinate;
import model.Player;
import model.Wall;
import model.Worker;
import model.combat.CombatReport;
import model.combat.CombatRequest;
import model.combat.CombatService;
import model.combat.DiceResult;
import model.combat.DiceRoller;
import model.disaster.Bear;
import model.disaster.BearAttackEvent;
import model.military.Archer;
import model.military.Catapult;
import model.military.Cavalry;
import model.military.MilitaryDamageHandler;
import model.military.MilitaryHex;
import model.military.MilitaryUnit;
import model.military.Swordsman;

public final class CombatRulesTest {
    public static void main(String[] args) {
        distinctTypesWallValuesAndDamagePriority();
        rangeTwoIsArcherOnlyAndExactlyOneDie();
        structuresAggregateAndUseDestructionLifecycle();
        defendersBlockStructuresAtomically();
        adjacentEmptyHexCaptureMovesTheForce();
        bearOffenseUsesTheSameDiceAndWallRules();
        bearEventDelegatesItsRealAttackToCombatEngine();
        System.out.println("CombatRulesTest passed");
    }

    private static void distinctTypesWallValuesAndDamagePriority() {
        Fixture fixture = new Fixture();
        MilitaryHex attackers = fixture.hexAt(fixture.a);
        Swordsman sword1 = fixture.add(attackers, new Swordsman(fixture.a));
        Swordsman sword2 = fixture.add(attackers, new Swordsman(fixture.a));
        Archer archer = fixture.add(attackers, new Archer(fixture.a));
        Cavalry cavalry = fixture.add(attackers, new Cavalry(fixture.a));
        Catapult catapult = fixture.add(attackers, new Catapult(fixture.a));
        MilitaryHex defenders = fixture.hexAt(fixture.b);
        fixture.add(defenders, new Swordsman(fixture.b));
        fixture.add(defenders, new Archer(fixture.b));
        fixture.map.buildWall(fixture.a, fixture.b);

        ScriptedRoller roller = new ScriptedRoller(Arrays.asList(6, 4, 2), Arrays.asList(3, 5));
        CombatReport report = fixture.service(roller).resolve(
                CombatRequest.military(archer, attackers, defenders));

        require(roller.counts.equals(Arrays.asList(3, 2)),
                "dice are counted by distinct type and Catapult contributes no anti-unit die");
        require(report.getDefenderRolls().equals(Arrays.asList(6, 5)),
                "wall adds two to each defense die, caps at six, and keeps die count");
        require(report.isWallModifierApplied(), "report records wall context");
        require(report.getParticipatingAttackers() == 5, "full adjacent force participates");
        require(!sword1.isAlive() && !sword2.isAlive(),
                "defender wins apply damage to Swordsmen first");
        require(archer.getCurrentAP() == 1 && cavalry.getCurrentAP() == 3
                        && catapult.getCurrentAP() == 1,
                "every surviving participant spends exactly one AP");
        require(!fixture.player.getUnits().contains(sword1)
                        && !fixture.player.getUnits().contains(sword2),
                "dead units are removed immediately");
    }

    private static void rangeTwoIsArcherOnlyAndExactlyOneDie() {
        Fixture fixture = new Fixture();
        MilitaryHex attackers = fixture.hexAt(fixture.a);
        Archer first = fixture.add(attackers, new Archer(fixture.a));
        Archer second = fixture.add(attackers, new Archer(fixture.a));
        Catapult catapult = fixture.add(attackers, new Catapult(fixture.a));
        MilitaryHex defenders = fixture.hexAt(fixture.c);
        fixture.add(defenders, new Swordsman(fixture.c));
        ScriptedRoller roller = new ScriptedRoller(Arrays.asList(6), Arrays.asList(1));

        CombatReport report = fixture.service(roller).resolve(
                CombatRequest.military(first, attackers, defenders));
        require(roller.counts.equals(Arrays.asList(1, 1)), "range two rolls exactly one Archer die");
        require(report.getParticipatingAttackers() == 2, "all eligible Archers participate");
        require(first.getCurrentAP() == 1 && second.getCurrentAP() == 1,
                "every participating Archer spends AP");
        require(catapult.getCurrentAP() == catapult.getMaxAP(),
                "Catapult does not participate in range-two anti-unit combat");
    }

    private static void structuresAggregateAndUseDestructionLifecycle() {
        Fixture fixture = new Fixture();
        MilitaryHex attackers = fixture.hexAt(fixture.a);
        MilitaryUnit sword = fixture.add(attackers, new Swordsman(fixture.a));
        MilitaryUnit archer = fixture.add(attackers, new Archer(fixture.a));
        MilitaryUnit cavalry = fixture.add(attackers, new Cavalry(fixture.a));
        MilitaryUnit catapult = fixture.add(attackers, new Catapult(fixture.a));
        Building farm = new Building(fixture.b, Constants.BuildingType.FARM);
        farm.synchronizeHealth(44, 100);
        fixture.player.addBuilding(farm);
        fixture.map.getHex(fixture.b).setHasBuilding(true);
        Worker worker = new Worker(fixture.b);
        fixture.player.addUnit(worker);
        require(worker.station(farm), "worker stations before combat");

        CombatReport report = fixture.service(new ScriptedRoller()).resolve(
                CombatRequest.building(sword, attackers, farm, null));
        require(report.getStructureDamage() == 44, "structure damage sums Sword+Archer+Cavalry+Catapult");
        require(report.getAttackerRolls().isEmpty() && report.getDefenderRolls().isEmpty(),
                "structures do not roll dice");
        require(fixture.player.getBuildingAt(fixture.b) == null
                        && !fixture.map.getHex(fixture.b).getHasBuilding(),
                "destroyed structure is removed and frees its hex");
        require(!worker.isStationed(), "structure destruction releases workers");
        require(sword.getCurrentAP() == 1 && archer.getCurrentAP() == 1
                        && cavalry.getCurrentAP() == 3 && catapult.getCurrentAP() == 1,
                "all structure attackers spend one AP");
    }

    private static void defendersBlockStructuresAtomically() {
        Fixture fixture = new Fixture();
        MilitaryHex attackers = fixture.hexAt(fixture.a);
        Swordsman attacker = fixture.add(attackers, new Swordsman(fixture.a));
        MilitaryHex defenders = fixture.hexAt(fixture.b);
        fixture.add(defenders, new Archer(fixture.b));
        Building farm = new Building(fixture.b, Constants.BuildingType.FARM);
        fixture.player.addBuilding(farm);
        int apBefore = attacker.getCurrentAP();
        int hpBefore = farm.getCurrentHp();
        expectIllegal(() -> fixture.service(new ScriptedRoller()).resolve(
                CombatRequest.building(attacker, attackers, farm, defenders)));
        require(attacker.getCurrentAP() == apBefore && farm.getCurrentHp() == hpBefore,
                "blocked structure attack changes neither AP nor HP");
    }

    private static void adjacentEmptyHexCaptureMovesTheForce() {
        Fixture fixture = new Fixture();
        MilitaryHex attackers = fixture.hexAt(fixture.a);
        Swordsman sword = fixture.add(attackers, new Swordsman(fixture.a));
        Archer archer = fixture.add(attackers, new Archer(fixture.a));
        CombatReport report = fixture.service(new ScriptedRoller()).resolve(
                CombatRequest.emptyHex(sword, attackers, fixture.b));
        require(report.isCaptured(), "capture is represented in the combat result");
        require(sword.getPosition().equals(fixture.b) && archer.getPosition().equals(fixture.b),
                "adjacent empty capture moves the participating force");
        require(sword.getCurrentAP() == 1 && archer.getCurrentAP() == 1,
                "capture consumes one AP from every participant");
        require(fixture.player.isInTerritory(fixture.b), "captured hex transfers to player territory");
    }

    private static void bearOffenseUsesTheSameDiceAndWallRules() {
        Fixture fixture = new Fixture();
        Bear bear = new Bear(fixture.a, fixture.a);
        MilitaryHex defenders = fixture.hexAt(fixture.b);
        Swordsman sword = fixture.add(defenders, new Swordsman(fixture.b));
        fixture.map.buildWall(fixture.a, fixture.b);
        ScriptedRoller roller = new ScriptedRoller(Arrays.asList(6), Arrays.asList(3, 2));
        CombatReport report = fixture.service(roller).resolve(
                CombatRequest.bearAttack(bear, sword, defenders));
        require(roller.counts.equals(Arrays.asList(1, 2)),
                "bear rolls one die and player defense rolls exactly two");
        require(report.getDefenderRolls().equals(Arrays.asList(5, 4))
                        && report.isWallModifierApplied(),
                "wall modifies military defense against a bear by value");
        require(!sword.isAlive() && !fixture.player.getUnits().contains(sword),
                "bear applies its 35 damage and dead military is cleaned immediately");
        require(bear.getCurrentAP() == bear.getMaxAP() - 1,
                "bear combat consumes one AP through the engine");
    }

    private static void bearEventDelegatesItsRealAttackToCombatEngine() {
        HexCoordinate den = new HexCoordinate(0, 0);
        HexCoordinate targetPosition = new HexCoordinate(1, 0);
        Map<HexCoordinate, Hex> cells = new HashMap<>();
        cells.put(den, new Hex(den, Constants.TerrainType.FOREST));
        cells.put(targetPosition, new Hex(targetPosition, Constants.TerrainType.PLAIN));
        GameMap map = new GameMap(cells);
        Player player = new Player("bear-event");
        Worker worker = new Worker(targetPosition);
        player.addUnit(worker);
        ScriptedRoller roller = new ScriptedRoller(Arrays.asList(6), Arrays.asList(1, 1));
        CombatService engine = new CombatService(map, player, roller,
                new MilitaryDamageHandler());
        BearAttackEvent event = new BearAttackEvent(den, map, player, engine);
        event.start();
        event.processTurn();
        require(worker.getCurrentHp() == 65, "bear event applies the specified 35 damage");
        require(roller.counts.equals(Arrays.asList(1, 2)),
                "civilian defense also uses the shared one-versus-two dice rule");
        require(event.getLastCombatReport() != null
                        && event.getLastCombatReport().getTargetType()
                        == model.combat.CombatTargetType.WILD_ANIMAL_ATTACK,
                "bear event exposes the common engine report");
    }

    private static void expectIllegal(Runnable action) {
        try {
            action.run();
            throw new AssertionError("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class Fixture {
        private final HexCoordinate a = new HexCoordinate(0, 0);
        private final HexCoordinate b = new HexCoordinate(1, 0);
        private final HexCoordinate c = new HexCoordinate(2, 0);
        private final GameMap map;
        private final Player player = new Player("combat-test");

        private Fixture() {
            Map<HexCoordinate, Hex> cells = new HashMap<>();
            cells.put(a, new Hex(a, Constants.TerrainType.PLAIN));
            cells.put(b, new Hex(b, Constants.TerrainType.PLAIN));
            cells.put(c, new Hex(c, Constants.TerrainType.PLAIN));
            map = new GameMap(cells);
        }

        private MilitaryHex hexAt(HexCoordinate coordinate) {
            return new MilitaryHex(map.getHex(coordinate));
        }

        private <T extends MilitaryUnit> T add(MilitaryHex hex, T unit) {
            hex.addUnit(unit);
            player.addUnit(unit);
            return unit;
        }

        private CombatService service(DiceRoller roller) {
            return new CombatService(map, player, roller, new MilitaryDamageHandler());
        }
    }

    private static final class ScriptedRoller extends DiceRoller {
        private final List<List<Integer>> scripts = new ArrayList<>();
        private final List<Integer> counts = new ArrayList<>();
        private int next;

        @SafeVarargs
        private ScriptedRoller(List<Integer>... scripts) {
            this.scripts.addAll(Arrays.asList(scripts));
        }

        @Override
        public DiceResult rollD6(int count) {
            counts.add(count);
            if (next >= scripts.size()) {
                List<Integer> values = new ArrayList<>();
                for (int i = 0; i < count; i++) values.add(1);
                return new DiceResult(values);
            }
            List<Integer> values = scripts.get(next++);
            require(values.size() == count, "scripted roll count mismatch");
            return new DiceResult(values);
        }
    }
}
