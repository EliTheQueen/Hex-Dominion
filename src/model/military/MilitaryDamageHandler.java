package model.military;

public class MilitaryDamageHandler {

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
                throw new IllegalArgumentException("Target Hex is null");
            }

            target.decreaseHp(1);

            if (target.isDead()) {
                targetHex.removeDeadUnits();
            }
        }
    }
}
