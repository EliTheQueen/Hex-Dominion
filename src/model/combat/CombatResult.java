package model.combat;

public class CombatResult {

    private final int attackerWins;
    private final int defenderWins;

    public CombatResult(int attackerWins, int defenderWins) {
        this.attackerWins = attackerWins;
        this.defenderWins = defenderWins;
    }

    public int getAttackerWins() {
        return attackerWins;
    }

    public int getDefenderWins() {
        return defenderWins;
    }

    public boolean attackerWon() {
        return attackerWins > defenderWins;
    }

    public boolean defenderWon() {
        return defenderWins >= attackerWins;
    }
}