package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

public class PathFinder {

    public static List<HexCoordinate> findPath(GameMap map, HexCoordinate from, HexCoordinate to, int maxAP) {
        if (from.equals(to))
            //یک لیست تغییرناپذیرِ یک‌عضوی می‌سازد
            return Collections.singletonList(from);

        Map<HexCoordinate, Integer> dist = new HashMap<>();
        Map<HexCoordinate, HexCoordinate> prev = new HashMap<>();
        PriorityQueue<HexCoordinate> queue =
                new PriorityQueue<>(Comparator.comparingInt(c -> dist.getOrDefault(c, Integer.MAX_VALUE)));

        dist.put(from, 0);
        queue.add(from);

        while (!queue.isEmpty()) {
            HexCoordinate curr = queue.poll();
            int currDist = dist.getOrDefault(curr, Integer.MAX_VALUE);
            if (curr.equals(to)) break;

            for (HexCoordinate neighbor : curr.findNeighbours()) {
                if (!map.containsCoordinate(neighbor))
                    continue;
                Hex hex = map.getHex(neighbor);
                if (hex == null)
                    continue;
                int moveCost;

                if (hex.hasRoad()) {
                    moveCost = 1;
                } else {
                    moveCost = Constants.MOVE_COST.getOrDefault(
                            hex.getTerrainType(),
                            1
                    );
                }
                int newDist = currDist + moveCost;
                if (newDist <= maxAP && newDist < dist.getOrDefault(neighbor, Integer.MAX_VALUE)) {
                    dist.put(neighbor, newDist);
                    prev.put(neighbor, curr);
                    queue.remove(neighbor);
                    queue.add(neighbor);
                }
            }
        }
        //kheyli nafahmidam bazam bekhoonesh
        if (!dist.containsKey(to)) return null;

        List<HexCoordinate> path = new ArrayList<>();
        HexCoordinate curr = to;
        while (curr != null) {
            path.add(0, curr);
            curr = prev.get(curr);
        }
        return path;
    }

    public static Set<HexCoordinate> reachable(GameMap map, HexCoordinate from, int maxAP) {
        Map<HexCoordinate, Integer> dist = new HashMap<>();
        PriorityQueue<HexCoordinate> queue =
                new PriorityQueue<>(Comparator.comparingInt(c -> dist.getOrDefault(c, Integer.MAX_VALUE)));
        dist.put(from, 0);
        queue.add(from);
        Set<HexCoordinate> result = new HashSet<>();

        while (!queue.isEmpty()) {
            HexCoordinate curr = queue.poll();
            int currDist = dist.getOrDefault(curr, Integer.MAX_VALUE);
            for (HexCoordinate neighbor : curr.findNeighbours()) {
                if (!map.containsCoordinate(neighbor)) continue;
                Hex hex = map.getHex(neighbor);
                if (hex == null) continue;
                int moveCost;

                if (hex.hasRoad()) {
                    moveCost = 1;
                } else {
                    moveCost = Constants.MOVE_COST.getOrDefault(
                            hex.getTerrainType(),
                            1
                    );
                }
                int newDist = currDist + moveCost;
                if (newDist <= maxAP && newDist < dist.getOrDefault(neighbor, Integer.MAX_VALUE)) {
                    dist.put(neighbor, newDist);
                    result.add(neighbor);
                    //chera?
                    queue.remove(neighbor);
                    queue.add(neighbor);
                }
            }
        }
        return result;
    }

    public static int pathCost(GameMap map, HexCoordinate from, HexCoordinate to, int maxAP) {
        List<HexCoordinate> path = findPath(map, from, to, maxAP);
        if (path == null) return -1;
        int cost = 0;
        for (int i = 1; i < path.size(); i++) {
            Hex hex = map.getHex(path.get(i));
            int moveCost;

            if (hex.hasRoad()) {
                moveCost = 1;
            } else {
                moveCost = Constants.MOVE_COST.getOrDefault(
                        hex.getTerrainType(),
                        1
                );
            }
            cost += moveCost;
        }
        return cost;
    }
}
