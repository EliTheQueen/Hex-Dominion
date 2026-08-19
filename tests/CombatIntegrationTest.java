import java.util.Arrays;
import model.*;
import model.combat.*;
import model.military.*;
import model.tribe.*;
import model.disaster.Bear;
import java.util.ArrayList;

public final class CombatIntegrationTest {
    public static void main(String[] args) {
        CombatResult tie = new CombatResolver(new DiceResult(Arrays.asList(4)),
                new DiceResult(Arrays.asList(4))).resolve();
        require(tie.getDefenderWins() == 1, "tie favors defender");

        GameState state = new GameState(15, 13);
        Tribe tribe = state.getTribes().get(0); tribe.discover(); tribe.getRelation().setScore(20);
        HexCoordinate attackFrom = tribe.getCampCoordinate().findNeighbours().get(0);
        if (!state.getMap().containsCoordinate(attackFrom)) attackFrom = tribe.getCampCoordinate().findNeighbours().get(1);
        Swordsman sword = new Swordsman(attackFrom); state.getPlayer().addUnit(sword);
        int campHp = tribe.getCurrentHp();
        CombatReport guardFight = state.attackTribeCamp(sword, tribe);
        require(guardFight != null && guardFight.getApConsumed() == 1, "attack consumes one AP");
        require(tribe.getCurrentHp() == campHp, "guards block camp targeting");

        tribe.removeGuards(99);
        Swordsman structureAttacker = new Swordsman(attackFrom); state.getPlayer().addUnit(structureAttacker);
        CombatReport structure = state.attackTribeCamp(structureAttacker, tribe);
        require(structure.getStructureDamage() == 10 && tribe.getCurrentHp() == campHp - 10,
                "fixed no-dice structure damage");

        Building building = new Building(tribe.getCampCoordinate(), Constants.BuildingType.FARM);
        StructureCombatService structures = new StructureCombatService();
        Swordsman structureSword = new Swordsman(attackFrom);
        require(structures.attackBuilding(structureSword, building, Arrays.asList(new Archer(tribe.getCampCoordinate()))) == 0,
                "defenders block structure target");
        require(structures.attackBuilding(structureSword, building, new ArrayList<>()) == 10,
                "structure combat is fixed damage");

        CapturingRoller roller = new CapturingRoller();
        MilitaryHex attackingHex = new MilitaryHex(state.getMap().getHex(attackFrom));
        MilitaryHex defendingHex = new MilitaryHex(state.getMap().getHex(tribe.getCampCoordinate()));
        attackingHex.addUnit(new Swordsman(attackFrom)); defendingHex.addUnit(new Archer(tribe.getCampCoordinate()));
        new CombatService(roller, new MilitaryDamageHandler()).attack(attackingHex.getAliveUnits().get(0),
                attackingHex, defendingHex, new Wall(100));
        require(roller.counts.get(1) == 3, "wall adds two defender dice capped by service");

        Swordsman hunter = new Swordsman(attackFrom);
        Bear bear = new Bear(tribe.getCampCoordinate(), tribe.getCampCoordinate());
        CombatReport bearReport = state.attackBear(hunter, bear);
        require(bearReport != null && bearReport.getApConsumed() == 1, "bear is attackable");
        System.out.println("CombatIntegrationTest passed");
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class CapturingRoller extends DiceRoller {
        private final java.util.List<Integer> counts = new java.util.ArrayList<>();
        @Override public DiceResult rollD6(int count) {
            counts.add(count); java.util.List<Integer> values = new java.util.ArrayList<>();
            for (int i = 0; i < count; i++) values.add(counts.size() == 1 ? 6 : 1);
            return new DiceResult(values);
        }
    }
}
