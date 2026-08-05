package model.disaster;

public interface DamageableDisasterTarget {

    void takeDisasterDamage(int damage);

    boolean isDestroyed();
}