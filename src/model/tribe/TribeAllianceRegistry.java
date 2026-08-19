package model.tribe;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

//فقط وضعیت نگه می‌دارد.
//هیچ تصمیمی نمی‌گیرد.
public class TribeAllianceRegistry implements java.io.Serializable {

    private final Set<Tribe> alliedTribes = new HashSet<Tribe>();

    public boolean canAllyWith(Tribe tribe){
        if (tribe == null){
            return false;
        }

        if (tribe.getType() == TribeType.WARRIOR) {
            return alliedTribes.isEmpty();
        }

        for (Tribe t : alliedTribes){
            if (t.getType() == TribeType.WARRIOR){
                return false;
            }

            if (isFarmerMountainConflict(t.getType(), tribe.getType())){
                return false;
            }
        }
        return true;
    }

    public void addAlliance(Tribe tribe) {
        if (tribe == null) {
            throw new IllegalArgumentException("tribe must not be null");
        }

        if (!canAllyWith(tribe)) {
            throw new IllegalStateException("alliance conflicts with existing alliances");
        }

        alliedTribes.add(tribe);
    }

    public void removeAlliance(Tribe tribe) {
        if (tribe == null) {
            return;
        }

        alliedTribes.remove(tribe);
    }

    public boolean isAlliedWith(Tribe tribe) {
        if (tribe == null) {
            throw new IllegalArgumentException("tribe must not be null");
        }

        return alliedTribes.contains(tribe);
    }

    public Set<Tribe> getAlliedTribes() {
        return Collections.unmodifiableSet(new HashSet<>(alliedTribes));
    }

    private boolean isFarmerMountainConflict(TribeType first, TribeType second){
        return first == TribeType.FARMER
                && second == TribeType.MOUNTAIN
                || first == TribeType.MOUNTAIN
                && second == TribeType.FARMER;
    }
}
