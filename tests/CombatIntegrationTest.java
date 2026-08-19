import java.util.Arrays;
import model.*;
import model.combat.*;
import model.military.*;
import model.tribe.*;

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
        System.out.println("CombatIntegrationTest passed");
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
