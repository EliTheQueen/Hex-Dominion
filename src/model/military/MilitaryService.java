package model.military;

import model.combat.CombatResult;
import model.combat.CombatReport;
import model.combat.CombatRequest;
import model.combat.CombatService;

import java.util.List;

public class MilitaryService {

    private final CombatService combatService;

    public MilitaryService(CombatService combatService) {
        if (combatService == null) {
            throw new IllegalArgumentException(
                    "CombatService cannot be null"
            );
        }

        this.combatService = combatService;
    }

    public CombatResult attack(
            MilitaryUnit attacker,
            MilitaryHex attackerHex,
            MilitaryHex defenderHex
    ) {

        CombatReport report = combatService.resolve(
                CombatRequest.military(attacker, attackerHex, defenderHex));
        return new CombatResult(report.getAttackerWins(), report.getDefenderWins());
    }

    public void recruit(MilitaryHex militaryHex, MilitaryUnit militaryUnit) {
        if (militaryHex == null) {
            throw new IllegalArgumentException(
                    "Military hex cannot be null"
            );
        }

        if (militaryUnit == null) {
            throw new IllegalArgumentException(
                    "Military unit cannot be null"
            );
        }

        militaryHex.addUnit(militaryUnit);
    }

    public void removeDeadUnits(MilitaryHex militaryHex) {
        if (militaryHex == null) {
            throw new IllegalArgumentException(
                    "Military hex cannot be null"
            );
        }

        militaryHex.removeDeadUnits();
    }

    public void endTurn(List<MilitaryHex> militaryHexes) {
        if (militaryHexes == null) {
            throw new IllegalArgumentException(
                    "Military hexes cannot be null"
            );
        }

        for (MilitaryHex militaryHex : militaryHexes) {
            if (militaryHex == null) {
                continue;
            }

            militaryHex.removeDeadUnits();

            for (MilitaryUnit militaryUnit : militaryHex.getAliveUnits()) {

                militaryUnit.resetAP();
            }
        }
    }
}
