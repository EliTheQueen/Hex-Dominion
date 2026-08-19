package model.combat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CombatResolver {

    private final DiceResult attackerDiceResult;
    private final DiceResult defenderDiceResult;

    public CombatResolver(DiceResult attacker, DiceResult defender) {
        if (attacker == null || defender == null) {
            throw new IllegalArgumentException("Dice results cannot be null");
        }

        this.attackerDiceResult = sortDescending(attacker);
        this.defenderDiceResult = sortDescending(defender);
    }

    public DiceResult getAttackerDiceResult() {
        return attackerDiceResult;
    }

    public DiceResult getDefenderDiceResult() {
        return defenderDiceResult;
    }

    private DiceResult sortDescending(DiceResult diceResult) {
        List<Integer> sortedRolls = new ArrayList<>(diceResult.getRolls());

        sortedRolls.sort(Collections.reverseOrder());

        return new DiceResult(sortedRolls);
    }

    public CombatResult resolve() {

        int attackerWins = 0;
        int defenderWins = 0;

        List<Integer> attackerRolls = attackerDiceResult.getRolls();
        List<Integer> defenderRolls = defenderDiceResult.getRolls();

        int pairs = Math.min(attackerRolls.size(), defenderRolls.size());

        for (int i = 0; i < pairs; i++) {

            int attackerDice = attackerRolls.get(i);
            int defenderDice = defenderRolls.get(i);

            if (attackerDice > defenderDice) {
                attackerWins++;
            } else {
                defenderWins++;
            }
        }

        return new CombatResult(attackerWins, defenderWins);
    }
}