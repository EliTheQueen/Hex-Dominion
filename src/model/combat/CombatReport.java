package model.combat;

import java.util.*;

/** Immutable controller-to-view result; combat rules remain Swing-independent. */
public final class CombatReport {
    private final String attacker;
    private final String defender;
    private final List<Integer> attackerRolls;
    private final List<Integer> defenderRolls;
    private final int attackerWins;
    private final int defenderWins;
    private final String casualty;
    private final int structureDamage;
    public CombatReport(String attacker, String defender, List<Integer> attackerRolls,
                        List<Integer> defenderRolls, int attackerWins, int defenderWins,
                        String casualty, int structureDamage) {
        this.attacker = attacker; this.defender = defender;
        this.attackerRolls = Collections.unmodifiableList(new ArrayList<>(attackerRolls));
        this.defenderRolls = Collections.unmodifiableList(new ArrayList<>(defenderRolls));
        this.attackerWins = attackerWins; this.defenderWins = defenderWins;
        this.casualty = casualty; this.structureDamage = structureDamage;
    }
    public String getAttacker() { return attacker; }
    public String getDefender() { return defender; }
    public List<Integer> getAttackerRolls() { return attackerRolls; }
    public List<Integer> getDefenderRolls() { return defenderRolls; }
    public int getAttackerWins() { return attackerWins; }
    public int getDefenderWins() { return defenderWins; }
    public String getCasualty() { return casualty; }
    public int getStructureDamage() { return structureDamage; }
    public int getApConsumed() { return 1; }
    public boolean isStructureAttack() { return structureDamage > 0; }
}
