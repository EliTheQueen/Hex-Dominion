package model.combat;

import java.util.List;
import model.Building;
import model.Wall;
import model.military.MilitaryUnit;

/** Fixed-damage, no-dice attacks against structures. */
public final class StructureCombatService {
    public int attackBuilding(MilitaryUnit attacker, Building target, List<MilitaryUnit> defenders) {
        if (!canAttack(attacker, defenders) || target == null || !target.isActive()) return 0;
        attacker.spendAttackAP(); int damage = attacker.getStructureDamage(); target.takeDamage(damage); return damage;
    }
    public int attackWall(MilitaryUnit attacker, Wall target, List<MilitaryUnit> defenders) {
        if (!canAttack(attacker, defenders) || target == null || target.isDestroyed()) return 0;
        attacker.spendAttackAP(); int damage = attacker.getStructureDamage(); target.takeDamage(damage); return damage;
    }
    private boolean canAttack(MilitaryUnit attacker, List<MilitaryUnit> defenders) {
        if (attacker == null || !attacker.canAttack()) return false;
        if (defenders != null) for (MilitaryUnit defender : defenders) if (defender != null && defender.isAlive()) return false;
        return true;
    }
}
