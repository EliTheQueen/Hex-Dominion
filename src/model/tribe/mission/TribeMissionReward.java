package model.tribe.mission;

import model.ResourceAmount;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public class TribeMissionReward implements java.io.Serializable {

    private final ResourceAmount resources;
    private final int relationReward;
    private final EnumSet<TribeMissionRewardEffect> effects;

    public TribeMissionReward(ResourceAmount resources, int relationReward) {
        this(resources, relationReward, EnumSet.noneOf(TribeMissionRewardEffect.class));
    }

    public TribeMissionReward(ResourceAmount resources, int relationReward,
                              Set<TribeMissionRewardEffect> effects) {
        if (resources == null) {
            throw new IllegalArgumentException("resources must not be null");
        }

        if (relationReward < 0) {
            throw new IllegalArgumentException("relationReward must not be negative");
        }

        this.resources = resources.copy();
        this.relationReward = relationReward;
        this.effects = effects == null || effects.isEmpty()
                ? EnumSet.noneOf(TribeMissionRewardEffect.class) : EnumSet.copyOf(effects);
    }

    public ResourceAmount getResources() {
        return resources.copy();
    }

    public int getRelationReward() {
        return relationReward;
    }

    public Set<TribeMissionRewardEffect> getEffects() {
        return Collections.unmodifiableSet(EnumSet.copyOf(effects));
    }

    public boolean hasEffect(TribeMissionRewardEffect effect) { return effects.contains(effect); }
}
