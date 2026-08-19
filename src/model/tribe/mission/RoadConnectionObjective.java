package model.tribe.mission;

import model.*;
import java.util.*;

public final class RoadConnectionObjective implements TribeMissionObjective {
    private final GameMap map;
    private final HexCoordinate start;
    private final HexCoordinate camp;
    public RoadConnectionObjective(GameMap map, HexCoordinate start, HexCoordinate camp) {
        this.map = map; this.start = start; this.camp = camp;
    }
    @Override public boolean isCompleted() {
        Set<HexCoordinate> seen = new HashSet<>();
        ArrayDeque<HexCoordinate> queue = new ArrayDeque<>(); queue.add(start); seen.add(start);
        while (!queue.isEmpty()) {
            HexCoordinate current = queue.remove();
            if (current.equals(camp) || current.distanceTo(camp) == 1) return true;
            for (HexCoordinate next : current.findNeighbours()) {
                if (!seen.contains(next) && map.hasRoad(current) && map.hasRoad(next)) {
                    seen.add(next); queue.add(next);
                }
            }
        }
        return false;
    }
    @Override public String getDescription() { return "Connect a road from the Town Hall to the camp"; }
}
