package model.tribe;

import model.ResourceAmount;

/** Recurring gameplay reward supplied for as long as an alliance remains active. */
public enum TribeAllianceBenefit {
    FARMER("Agricultural tribute: +2 Food each turn", ResourceAmount.of(2, 0, 0, 0)),
    WARRIOR("Armorer tribute: +1 Iron each turn", ResourceAmount.of(0, 0, 0, 1)),
    MERCHANT("Caravan tribute: +1 Wood and +1 Stone each turn", ResourceAmount.of(0, 1, 1, 0)),
    MOUNTAIN("Quarry tribute: +2 Stone each turn", ResourceAmount.of(0, 0, 2, 0)),
    COASTAL("Harbor tribute: +1 Food and +1 Wood each turn", ResourceAmount.of(1, 1, 0, 0));

    private final String description;
    private final ResourceAmount turnIncome;

    TribeAllianceBenefit(String description, ResourceAmount turnIncome) {
        this.description = description;
        this.turnIncome = turnIncome;
    }

    public String getDescription() { return description; }
    public ResourceAmount getTurnIncome() { return turnIncome.copy(); }

    public static TribeAllianceBenefit forType(TribeType type) {
        if (type == null) throw new IllegalArgumentException("tribe type must not be null");
        return valueOf(type.name());
    }
}
