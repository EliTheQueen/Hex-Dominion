package model.combat;

import java.util.*;

/** Immutable controller-to-view result; combat rules remain Swing-independent. */
public final class CombatReport implements java.io.Serializable {
    private final String attacker;
    private final String defender;
    private final List<Integer> attackerRolls;
    private final List<Integer> defenderRolls;
    private final int attackerWins;
    private final int defenderWins;
    private final String casualty;
    private final int structureDamage;
    private final int participatingAttackers;
    private final CombatTargetType targetType;
    private final boolean wallModifierApplied;
    private final boolean captured;
    public CombatReport(String attacker, String defender, List<Integer> attackerRolls,
                        List<Integer> defenderRolls, int attackerWins, int defenderWins,
                        String casualty, int structureDamage, int participatingAttackers,
                        CombatTargetType targetType, boolean wallModifierApplied, boolean captured) {
        this.attacker = attacker; this.defender = defender;
        this.attackerRolls = Collections.unmodifiableList(new ArrayList<>(attackerRolls));
        this.defenderRolls = Collections.unmodifiableList(new ArrayList<>(defenderRolls));
        this.attackerWins = attackerWins; this.defenderWins = defenderWins;
        this.casualty = casualty; this.structureDamage = structureDamage;
        this.participatingAttackers = participatingAttackers;
        this.targetType = Objects.requireNonNull(targetType);
        this.wallModifierApplied = wallModifierApplied;
        this.captured = captured;
    }
    public String getAttacker() { return attacker; }
    public String getDefender() { return defender; }
    public List<Integer> getAttackerRolls() { return attackerRolls; }
    public List<Integer> getDefenderRolls() { return defenderRolls; }
    public int getAttackerWins() { return attackerWins; }
    public int getDefenderWins() { return defenderWins; }
    public String getCasualty() { return casualty; }
    public int getStructureDamage() { return structureDamage; }
    public int getApConsumed() { return participatingAttackers; }
    public int getParticipatingAttackers() { return participatingAttackers; }
    public CombatTargetType getTargetType() { return targetType; }
    public boolean isWallModifierApplied() { return wallModifierApplied; }
    public boolean isCaptured() { return captured; }
    public boolean isStructureAttack() { return structureDamage > 0; }
}
