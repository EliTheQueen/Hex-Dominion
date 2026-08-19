package model.tribe.mission;

import model.*;

public final class BuildingNearCampObjective implements TribeMissionObjective {
    private final Player player;
    private final HexCoordinate camp;
    private final Constants.BuildingType type;
    private final int maximumDistance;
    public BuildingNearCampObjective(Player player, HexCoordinate camp, Constants.BuildingType type) {
        this(player, camp, type, 2);
    }
    public BuildingNearCampObjective(Player player, HexCoordinate camp, Constants.BuildingType type,
                                     int maximumDistance) {
        if (player == null || camp == null || type == null || maximumDistance < 0)
            throw new IllegalArgumentException("invalid building mission objective");
        this.player = player; this.camp = camp; this.type = type;
        this.maximumDistance = maximumDistance;
    }
    @Override public boolean isCompleted() {
        for (Building building : player.getBuildings())
            if (building.isActive() && building.getType() == type
                    && building.getPosition().distanceTo(camp) <= maximumDistance) return true;
        return false;
    }
    @Override public String getDescription() { return "Build a " + type.name().replace('_', ' ')
            + " within " + maximumDistance + " hexes of the camp"; }
    public Constants.BuildingType getBuildingType() { return type; }
    public int getMaximumDistance() { return maximumDistance; }
}
