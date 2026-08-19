import java.util.Arrays;
import java.util.Collections;

import model.combat.CombatReport;
import model.combat.CombatTargetType;
import view.CombatPresentationModel;

public final class CombatPresentationTest {
    public static void main(String[] args) {
        CombatReport diceReport = new CombatReport(
                "[SWORDSMAN, ARCHER]", "WARRIOR GUARDS",
                Arrays.asList(6, 4, 2), Arrays.asList(6, 3),
                1, 1, "1 guard and 1 Swordsman lost", 0, 3,
                CombatTargetType.TRIBE_GUARDS, true, false);
        CombatPresentationModel dice = new CombatPresentationModel(diceReport);
        require(dice.getAttackerLabel().contains("SWORDSMAN")
                        && dice.getDefenderLabel().equals("WARRIOR GUARDS"),
                "attacker and defender labels are explicit");
        require(dice.getPairComparisons().equals(Arrays.asList(
                        "Pair 1: 6 = 6 — tie favors defender",
                        "Pair 2: 4 > 3 — attacker wins")),
                "sorted dice are presented one pair at a time with tie semantics");
        require(dice.getResultSequence().stream().anyMatch(line -> line.contains("+2")
                        && line.contains("maximum 6")),
                "wall modifier is visibly explained");
        require(dice.getResultSequence().stream().anyMatch(line -> line.startsWith("Casualties:"))
                        && dice.getResultSequence().stream().anyMatch(line -> line.contains("Unpaired attacker"))
                        && dice.getResultSequence().stream().anyMatch(line -> line.contains("3 AP")),
                "casualties, unpaired dice, and aggregate AP are sequenced");

        CombatReport structureReport = new CombatReport(
                "[CATAPULT]", "FARM", Collections.emptyList(), Collections.emptyList(),
                0, 0, "Structure destroyed", 20, 1,
                CombatTargetType.BUILDING, false, false);
        CombatPresentationModel structure = new CombatPresentationModel(structureReport);
        require(structure.getResultSequence().contains("Structure damage: 20"),
                "structure damage has a dedicated result step");

        CombatReport captureReport = new CombatReport(
                "[CAVALRY]", "EMPTY HEX", Collections.emptyList(), Collections.emptyList(),
                0, 0, "Hex secured", 0, 1,
                CombatTargetType.EMPTY_HEX, false, true);
        CombatPresentationModel capture = new CombatPresentationModel(captureReport);
        require(capture.getResultSequence().stream().anyMatch(line -> line.startsWith("CAPTURE:")),
                "territory acquisition has a dedicated notification");
        System.out.println("CombatPresentationTest passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
