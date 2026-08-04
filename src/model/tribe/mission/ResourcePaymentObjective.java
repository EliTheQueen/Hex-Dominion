package model.tribe.mission;

import model.Player;
import model.ResourceAmount;

public class ResourcePaymentObjective implements TribeMissionObjective {

    private final Player player;
    private final ResourceAmount requiredResources;

    public ResourcePaymentObjective(Player player, ResourceAmount requiredResources) {
        if (player == null || requiredResources == null) {
            throw new IllegalArgumentException("arguments must not be null");
        }

        this.player = player;
        this.requiredResources = requiredResources.copy();
    }

    @Override
    public boolean isCompleted() {
        return player.canAfford(requiredResources);
    }

    @Override
    public String getDescription() {
        return "Provide the required resources";
    }

    public ResourceAmount getRequiredResources() {
        return requiredResources.copy();
    }
}