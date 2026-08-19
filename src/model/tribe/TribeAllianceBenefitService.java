package model.tribe;

import model.Player;
import model.ResourceAmount;

/** Derives active benefits from the alliance registry, so no parallel state can become stale. */
public final class TribeAllianceBenefitService implements java.io.Serializable {
    private final TribeAllianceRegistry allianceRegistry;

    public TribeAllianceBenefitService(TribeAllianceRegistry allianceRegistry) {
        if (allianceRegistry == null) throw new IllegalArgumentException("allianceRegistry must not be null");
        this.allianceRegistry = allianceRegistry;
    }

    public TribeAllianceBenefit getBenefit(Tribe tribe) {
        if (tribe == null) throw new IllegalArgumentException("tribe must not be null");
        return TribeAllianceBenefit.forType(tribe.getType());
    }

    public boolean isBenefitActive(Tribe tribe) {
        return tribe != null && allianceRegistry.isAlliedWith(tribe);
    }

    public ResourceAmount getActiveTurnIncome() {
        ResourceAmount total = ResourceAmount.zero();
        for (Tribe tribe : allianceRegistry.getAlliedTribes()) {
            total = total.add(TribeAllianceBenefit.forType(tribe.getType()).getTurnIncome());
        }
        return total;
    }

    public void applyTurnBenefits(Player player) {
        if (player == null) throw new IllegalArgumentException("player must not be null");
        player.addResources(getActiveTurnIncome());
    }

    public String getDisplayText(Tribe tribe) {
        TribeAllianceBenefit benefit = getBenefit(tribe);
        return (isBenefitActive(tribe) ? "ACTIVE — " : "Inactive — ") + benefit.getDescription();
    }
}
