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

                if (!canEnter(hex)) {
                    continue;
                }

                int moveCost = getMoveCost(hex);

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
                    queue.add(neighbor);
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

                if (!canEnter(hex)) {
                    continue;
                }

                int moveCost =
                        getMoveCost(hex);

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
                    queue.add(neighbor);
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
        List<HexCoordinate> path =
                findPath(
                        map,
                        from,
                        to,
                        maxAP
                );

        if (path == null) {
            return -1;
        }

        int cost = 0;

        for (int i = 1; i < path.size(); i++) {

            Hex hex =
                    map.getHex(
                            path.get(i)
                    );

            cost += getMoveCost(hex);
        }

        return cost;
    }

    private static boolean canEnter(Hex hex) {

        if (hex == null) {
            return false;
        }

        if (hex.isBlocked()) {
            return false;
        }

        return hex.getTerrainType()
                != Constants.TerrainType.MOUNTAIN_RANGE;
    }

    private static int getMoveCost(Hex hex) {

        if (hex.hasRoad()) {
            return 1;
        }

        return Constants.MOVE_COST.getOrDefault(
                hex.getTerrainType(),
                1
        );
    }
}