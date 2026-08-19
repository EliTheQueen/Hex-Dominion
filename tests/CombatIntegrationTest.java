import model.GameState;
import model.HexCoordinate;
import model.combat.CombatReport;
import model.combat.CombatResolver;
import model.combat.CombatResult;
import model.combat.DiceResult;
import model.disaster.Bear;
import model.military.Archer;
import model.military.Swordsman;
import model.tribe.Tribe;

import java.util.Arrays;

/** Verifies that real GameState combat paths use the common engine. */
public final class CombatIntegrationTest {
    public static void main(String[] args) {
        CombatResult tie = new CombatResolver(new DiceResult(Arrays.asList(4)),
                new DiceResult(Arrays.asList(4))).resolve();
        require(tie.getDefenderWins() == 1, "tie favors defender");

        GameState state = new GameState(15, 13, 111L);
        Tribe tribe = state.getTribes().get(0);
        tribe.discover();
        tribe.getRelation().setScore(20);
        HexCoordinate attackFrom = validNeighbour(state, tribe.getCampCoordinate(), 0);
        Swordsman sword = new Swordsman(attackFrom);
        state.getPlayer().addUnit(sword);
        int campHp = tribe.getCurrentHp();
        CombatReport guardFight = state.attackTribeCamp(sword, tribe);
        require(guardFight != null && guardFight.getParticipatingAttackers() == 1,
                "tribe guard combat uses the common engine");
        require(tribe.getCurrentHp() == campHp, "guards block camp targeting");

        tribe.removeGuards(99);
        HexCoordinate structureFrom = validNeighbour(state, tribe.getCampCoordinate(), 1);
        Swordsman structureSword = new Swordsman(structureFrom);
        Archer structureArcher = new Archer(structureFrom);
        state.getPlayer().addUnit(structureSword);
        state.getPlayer().addUnit(structureArcher);
        CombatReport structure = state.attackTribeCamp(structureSword, tribe);
        require(structure != null && structure.getStructureDamage() == 16,
                "camp damage sums the full attacking force");
        require(structure.getParticipatingAttackers() == 2
                        && structureSword.getCurrentAP() == 1 && structureArcher.getCurrentAP() == 1,
                "all camp attackers spend one AP");

        Swordsman hunter = new Swordsman(structureFrom);
        state.getPlayer().addUnit(hunter);
        Bear bear = new Bear(tribe.getCampCoordinate(), tribe.getCampCoordinate());
        CombatReport bearReport = state.attackBear(hunter, bear);
        require(bearReport != null && bearReport.getDefender().equals("BEAR"),
                "bear combat uses the common engine");
        System.out.println("CombatIntegrationTest passed");
    }

    private static HexCoordinate validNeighbour(GameState state, HexCoordinate center, int skip) {
        int found = 0;
        for (HexCoordinate coordinate : center.findNeighbours()) {
            if (state.getMap().containsCoordinate(coordinate)) {
                if (found++ >= skip) return coordinate;
            }
        }
        throw new AssertionError("camp has no valid neighbour");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
