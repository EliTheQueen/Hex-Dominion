package model.tribe.mission;

import model.*;

public final class BuildingNearCampObjective implements TribeMissionObjective {
    private final Player player;
    private final HexCoordinate camp;
    private final Constants.BuildingType type;
    public BuildingNearCampObjective(Player player, HexCoordinate camp, Constants.BuildingType type) {
        this.player = player; this.camp = camp; this.type = type;
    }
    @Override public boolean isCompleted() {
        for (Building building : player.getBuildings())
            if (building.isActive() && building.getType() == type
                    && building.getPosition().distanceTo(camp) <= 2) return true;
        return false;
    }
    @Override public String getDescription() { return "Build a " + type.name().replace('_', ' ') + " near the camp"; }
}
