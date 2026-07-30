package model.combat;

import model.military.MilitaryDamageHandler;
import model.military.MilitaryHex;
import model.military.MilitaryUnit;

public class CombatService {
    private final DiceRoller diceRoller;
    private final MilitaryDamageHandler damageHandler;

    public CombatService(DiceRoller diceRoller, MilitaryDamageHandler damageHandler) {
        if (diceRoller == null) throw new IllegalArgumentException("DiceRoller cannot be null");
        if (damageHandler == null) throw new IllegalArgumentException("MilitaryDamageHandler cannot be null");

        this.diceRoller = diceRoller;
        this.damageHandler = damageHandler;
    }

    public CombatResult attack(
            MilitaryUnit attacker,
            MilitaryHex attackerHex,
            MilitaryHex defenderHex
    ) {
        validateAttack(attacker, attackerHex, defenderHex);

        attacker.spendAttackAP();

        int attackerDiceCount = attackerHex.getAliveUnits().size();
        int defenderDiceCount = defenderHex.getAliveUnits().size();

        DiceResult attackerRolls = diceRoller.rollD6(attackerDiceCount);
        DiceResult defenderRolls = diceRoller.rollD6(defenderDiceCount);

        CombatResolver resolver = new CombatResolver(
                attackerRolls,
                defenderRolls
        );

        CombatResult result = resolver.resolve();

        damageHandler.applyDamage(defenderHex, result.getAttackerWins());

        damageHandler.applyDamage(attackerHex, result.getDefenderWins());

        return result;
    }

    private void validateAttack(
            MilitaryUnit attacker,
            MilitaryHex attackerHex,
            MilitaryHex defenderHex
    ) {
        if (attacker == null) throw new IllegalArgumentException("attacker cannot be null");
        if (attackerHex == null) throw new IllegalArgumentException("attackerHex cannot be null");
        if (defenderHex == null) throw new IllegalArgumentException("defenderHex cannot be null");

        if (attackerHex == defenderHex) {
            throw new IllegalArgumentException("attackerHex and defenderHex cannot be the same");
        }

        if (!attackerHex.getUnits().contains(attacker)) {
            throw new IllegalArgumentException("attacker not in attackerHex");
        }

        if (!attacker.canAttack()) {
            throw new IllegalArgumentException("attacker can not attack");
        }

        if (defenderHex.getAliveUnits().isEmpty()) {
            throw new IllegalArgumentException("defenderHex has no alive units");
        }

        int distance = attacker.getPosition().distanceTo(defenderHex.getCoordinate());

        if (distance > attacker.getRange()) {
            throw new IllegalArgumentException("Target hex is not in range");
        }
    }

}
