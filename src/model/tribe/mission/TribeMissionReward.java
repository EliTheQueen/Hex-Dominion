package model.tribe.mission;

import model.ResourceAmount;

public class TribeMissionReward {

    private final ResourceAmount resources;
    private final int relationReward;

    public TribeMissionReward(ResourceAmount resources, int relationReward) {
        if (resources == null) {
            throw new IllegalArgumentException("resources must not be null");
        }

        if (relationReward < 0) {
            throw new IllegalArgumentException("relationReward must not be negative");
        }

        this.resources = resources.copy();
        this.relationReward = relationReward;
    }

    public ResourceAmount getResources() {
        return resources.copy();
    }

    public int getRelationReward() {
        return relationReward;
    }
}