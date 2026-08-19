package model.disaster;

public final class DisasterDamage {

    private DisasterDamage() {
    }

    public static void apply(DamageableDisasterTarget target, int damage) {
        if (target == null) {
            throw new IllegalArgumentException("target must not be null");
        }

        if (damage < 0) {
            throw new IllegalArgumentException("damage must not be negative");
        }

        if (damage == 0 || target.isDestroyed()) {
            return;
        }

        target.takeDisasterDamage(damage);
    }

    public static void applyAndClearActionPoints(
            DamageableDisasterTarget damageTarget,
            ActionPointDisasterTarget actionPointTarget,
            int damage
    ) {
        if (actionPointTarget == null) {
            throw new IllegalArgumentException("actionPointTarget must not be null");
        }

        apply(damageTarget, damage);
        actionPointTarget.clearActionPoints();
    }
}