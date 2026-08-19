package model.tribe.mission;

import model.Building;
import model.GameMap;
import model.HexCoordinate;
import model.Player;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/** A continuous road from any active player building to a hex adjacent to the camp. */
public final class RoadConnectionObjective implements TribeMissionObjective {
    private final GameMap map;
    private final Player player;
    private final HexCoordinate camp;

    public RoadConnectionObjective(GameMap map, Player player, HexCoordinate camp) {
        if (map == null || player == null || camp == null) {
            throw new IllegalArgumentException("road objective dependencies must not be null");
        }
        this.map = map;
        this.player = player;
        this.camp = camp;
    }

    @Override public boolean isCompleted() {
        Set<HexCoordinate> seen = new HashSet<>();
        ArrayDeque<HexCoordinate> queue = new ArrayDeque<>();
        for (Building building : player.getBuildings()) {
            HexCoordinate start = building.getPosition();
            if (building.isActive() && map.hasRoad(start) && seen.add(start)) queue.add(start);
        }
        while (!queue.isEmpty()) {
            HexCoordinate current = queue.remove();
            if (current.distanceTo(camp) == 1) return true;
            for (HexCoordinate next : current.findNeighbours()) {
                if (map.containsCoordinate(next) && map.hasRoad(next) && seen.add(next)) queue.add(next);
            }
        }
        return false;
    }

    @Override public String getDescription() {
        return "Connect a road from any own building to a hex adjacent to the camp";
    }
}
