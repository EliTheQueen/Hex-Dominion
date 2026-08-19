package view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import model.combat.CombatReport;

/** Deterministic, testable sequence consumed by the animated Swing overlay. */
public final class CombatPresentationModel {
    private final CombatReport report;
    private final List<String> pairComparisons;
    private final List<String> resultSequence;

    public CombatPresentationModel(CombatReport report) {
        if (report == null) throw new IllegalArgumentException("combat report is required");
        this.report = report;
        this.pairComparisons = buildPairComparisons(report);
        this.resultSequence = buildResultSequence(report);
    }

    private List<String> buildPairComparisons(CombatReport report) {
        List<String> comparisons = new ArrayList<>();
        int pairs = Math.min(report.getAttackerRolls().size(), report.getDefenderRolls().size());
        for (int i = 0; i < pairs; i++) {
            int attack = report.getAttackerRolls().get(i);
            int defense = report.getDefenderRolls().get(i);
            comparisons.add("Pair " + (i + 1) + ": " + attack
                    + (attack > defense ? " > " : attack == defense ? " = " : " < ")
                    + defense + " — " + (attack > defense ? "attacker wins"
                    : attack == defense ? "tie favors defender" : "defender wins"));
        }
        return Collections.unmodifiableList(comparisons);
    }

    private List<String> buildResultSequence(CombatReport report) {
        List<String> results = new ArrayList<>();
        if (report.isWallModifierApplied()) {
            results.add("Wall defense: +2 applied to defender dice (maximum 6).");
        }
        int paired = Math.min(report.getAttackerRolls().size(), report.getDefenderRolls().size());
        if (report.getAttackerRolls().size() > paired) {
            results.add("Unpaired attacker dice: "
                    + report.getAttackerRolls().subList(paired, report.getAttackerRolls().size()));
        }
        if (report.getDefenderRolls().size() > paired) {
            results.add("Unpaired defender dice: "
                    + report.getDefenderRolls().subList(paired, report.getDefenderRolls().size()));
        }
        results.add("Casualties: " + report.getCasualty());
        if (report.isStructureAttack()) {
            results.add("Structure damage: " + report.getStructureDamage());
        }
        if (report.isCaptured()) {
            results.add("CAPTURE: the defender hex joined your territory.");
        }
        results.add("Action cost: " + report.getApConsumed() + " AP across "
                + report.getParticipatingAttackers() + " participating attacker"
                + (report.getParticipatingAttackers() == 1 ? "." : "s."));
        return Collections.unmodifiableList(results);
    }

    public String getAttackerLabel() { return report.getAttacker(); }
    public String getDefenderLabel() { return report.getDefender(); }
    public List<Integer> getAttackerDice() { return report.getAttackerRolls(); }
    public List<Integer> getDefenderDice() { return report.getDefenderRolls(); }
    public List<String> getPairComparisons() { return pairComparisons; }
    public List<String> getResultSequence() { return resultSequence; }
    public boolean hasWallModifier() { return report.isWallModifierApplied(); }
}
