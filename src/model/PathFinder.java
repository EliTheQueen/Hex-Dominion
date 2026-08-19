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

    public static List<HexCoordinate> findPath(
            GameMap map,
            HexCoordinate from,
            HexCoordinate to,
            int maxAP
    ) {
        return findPath(map, from, to, maxAP, MovementPolicy.basic());
    }

    public static List<HexCoordinate> findPath(
            GameMap map, HexCoordinate from, HexCoordinate to, int maxAP,
            MovementPolicy policy
    ) {
        if (from.equals(to)) {
            return Collections.singletonList(from);
        }

        Map<HexCoordinate, Integer> dist = new HashMap<>();
        Map<HexCoordinate, HexCoordinate> prev = new HashMap<>();

        PriorityQueue<HexCoordinate> queue =
                new PriorityQueue<>(
                        Comparator.comparingInt(
                                c -> dist.getOrDefault(
                                        c,
                                        Integer.MAX_VALUE
                                )
                        )
                );

        dist.put(from, 0);
        queue.add(from);

        while (!queue.isEmpty()) {

            HexCoordinate curr = queue.poll();

            int currDist =
                    dist.getOrDefault(
                            curr,
                            Integer.MAX_VALUE
                    );

            if (curr.equals(to)) {
                break;
            }

            for (HexCoordinate neighbor : curr.findNeighbours()) {

                if (!map.containsCoordinate(neighbor)) {
                    continue;
                }

                Hex hex = map.getHex(neighbor);

                if (!policy.canEnter(hex)) {
                    continue;
                }

                int moveCost = policy.edgeCost(map, curr, neighbor);

                int newDist = currDist + moveCost;

                if (newDist <= maxAP && newDist < dist.getOrDefault(neighbor, Integer.MAX_VALUE)) {

                    dist.put(
                            neighbor,
                            newDist
                    );

                    prev.put(
                            neighbor,
                            curr
                    );

                    queue.remove(neighbor);
                    // Entering water ends movement, so a path cannot sail onward on that action.
                    if (!policy.enteringWater(map, curr, neighbor)) queue.add(neighbor);
                }
            }
        }

        if (!dist.containsKey(to)) {
            return null;
        }

        List<HexCoordinate> path =
                new ArrayList<>();

        HexCoordinate curr = to;

        while (curr != null) {
            path.add(0, curr);
            curr = prev.get(curr);
        }

        return path;
    }

    public static Set<HexCoordinate> reachable(
            GameMap map,
            HexCoordinate from,
            int maxAP
    ) {
        return reachable(map, from, maxAP, MovementPolicy.basic());
    }

    public static Set<HexCoordinate> reachable(
            GameMap map, HexCoordinate from, int maxAP, MovementPolicy policy
    ) {
        Map<HexCoordinate, Integer> dist =
                new HashMap<>();

        PriorityQueue<HexCoordinate> queue =
                new PriorityQueue<>(
                        Comparator.comparingInt(
                                c -> dist.getOrDefault(
                                        c,
                                        Integer.MAX_VALUE
                                )
                        )
                );

        dist.put(from, 0);
        queue.add(from);

        Set<HexCoordinate> result =
                new HashSet<>();

        while (!queue.isEmpty()) {

            HexCoordinate curr =
                    queue.poll();

            int currDist =
                    dist.getOrDefault(
                            curr,
                            Integer.MAX_VALUE
                    );

            for (HexCoordinate neighbor :
                    curr.findNeighbours()) {

                if (!map.containsCoordinate(neighbor)) {
                    continue;
                }

                Hex hex =
                        map.getHex(neighbor);

                if (!policy.canEnter(hex)) {
                    continue;
                }

                int moveCost = policy.edgeCost(map, curr, neighbor);

                int newDist =
                        currDist + moveCost;

                if (newDist <= maxAP
                        && newDist < dist.getOrDefault(
                        neighbor,
                        Integer.MAX_VALUE
                )) {

                    dist.put(
                            neighbor,
                            newDist
                    );

                    result.add(neighbor);

                    queue.remove(neighbor);
                    if (!policy.enteringWater(map, curr, neighbor)) queue.add(neighbor);
                }
            }
        }

        return result;
    }

    public static int pathCost(
            GameMap map,
            HexCoordinate from,
            HexCoordinate to,
            int maxAP
    ) {
        return pathCost(map, from, to, maxAP, MovementPolicy.basic());
    }

    public static int pathCost(GameMap map, HexCoordinate from, HexCoordinate to,
                               int maxAP, MovementPolicy policy) {
        List<HexCoordinate> path =
                findPath(
                        map,
                        from,
                        to,
                        maxAP, policy
                );

        if (path == null) {
            return -1;
        }

        int cost = 0;

        for (int i = 1; i < path.size(); i++) {
            HexCoordinate fromm = path.get(i - 1);
            HexCoordinate tto = path.get(i);
            cost += policy.edgeCost(map, fromm, tto);
        }

        return cost;
    }

}
