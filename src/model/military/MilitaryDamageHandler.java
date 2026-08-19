package model.military;

public class MilitaryDamageHandler implements java.io.Serializable {

    public void applyDamage(MilitaryHex targetHex, int damageCount) {
        if (targetHex == null) {
            throw new IllegalArgumentException("Target Hex is null");
        }

        if (damageCount < 0) {
            throw new IllegalArgumentException("Damage count is negative");
        }

        for (int i = 0; i < damageCount; i++) {
            MilitaryUnit target = targetHex.getNextDamageTarget();

            if (target == null) {
                break;
            }

            target.takeDamage(1);

            if (target.isDead()) {
                targetHex.removeDeadUnits();
            }
        }
    }

    /** Applies one raw hit to the normal priority target (used by high-damage wildlife). */
    public void applyHitPointDamage(MilitaryHex targetHex, int hitPointDamage) {
        if (targetHex == null || hitPointDamage < 0) {
            throw new IllegalArgumentException("invalid military damage target or amount");
        }
        MilitaryUnit target = targetHex.getNextDamageTarget();
        if (target == null || hitPointDamage == 0) return;
        target.takeDamage(hitPointDamage);
        targetHex.removeDeadUnits();
    }
}
