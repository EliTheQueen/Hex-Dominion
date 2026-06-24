package model;

import java.util.List;

/** Central game state: holds the map, the player and turn/turn-end progression. */
public class GameState {
    private final GameMap map;
    private final Player player;
    private int currentTurn;
    private static final int MAX_TURNS = 50;
    private boolean gameOver;
    private String gameOverReason;
    private int finalScore;

    public GameState(int mapWidth, int mapHeight) {
        MapGenerator gen = new MapGenerator();
        map = gen.generateMap(mapWidth, mapHeight);

        player = new Player("Player");

        HexCoordinate center = new HexCoordinate(mapWidth / 2, mapHeight / 2);

        // Town Hall at the center.
        Building townHall = new Building(center, Constants.BuildingType.TOWN_HALL);
        player.addBuilding(townHall);
        Hex centerHex = map.getHex(center);
        if (centerHex != null) centerHex.setHasBuilding(true);

        // Initial territory: center hex and its in-bounds neighbours.
        player.expandTerritory(center);
        for (HexCoordinate n : center.findNeighbours()) {
            if (map.containsCoordinate(n)) {
                player.expandTerritory(n);
            }
        }

        // Initial Explorer placed on a neighbouring hex.
        HexCoordinate explorerStart = center;
        for (HexCoordinate n : center.findNeighbours()) {
            if (map.containsCoordinate(n)) { explorerStart = n; break; }
        }
        player.addUnit(new Explorer(explorerStart));

        updateVisibility();

        currentTurn = 1;
        gameOver = false;
        gameOverReason = "";
        finalScore = 0;
    }

    public void endTurn() {
        if (gameOver) return;
        boolean profTools = player.hasProfessionalTools();

        // 1. Production + upkeep for every building.
        for (Building b : player.getBuildings()) {
            player.addResources(b.produce(profTools));
            player.spend(b.getUpkeepCost());
        }

        // 2. Each living unit eats food.
        int unitCount = player.getUnitCount();
        player.getResources().forceSpendFood(unitCount * Constants.FOOD_PER_UNIT);

        // 3. Refresh action points and clear transient movement state.
        for (Unit u : player.getUnits()) {
            if (u.isAlive()) {
                u.resetAP();
                if (u.getState() == Constants.UnitState.MOVING) {
                    u.setState(Constants.UnitState.IDLE);
                }
            }
        }

        // 4. Auto-explore for explorers in auto mode.
        for (Unit u : player.getUnits()) {
            if (u.isAlive() && u instanceof Explorer && ((Explorer) u).isAutoExploreMode()) {
                autoExplore((Explorer) u);
            }
        }

        // 5. Reveal hexes around units/buildings.
        updateVisibility();

        // 6. Advance turn.
        currentTurn++;

        // 7. Win / lose checks.
        if (currentTurn > MAX_TURNS) {
            gameOver = true;
            gameOverReason = "Max turns reached";
            finalScore = ScoreCalculator.calculate(player, map);
        } else if (player.getUnitCount() == 0 && player.getBuildingCount() == 0) {
            gameOver = true;
            gameOverReason = "All units and buildings lost";
            finalScore = ScoreCalculator.calculate(player, map);
        }
    }

    /** Greedily walks an auto-explore unit one step toward the nearest unexplored hex. */
    private void autoExplore(Explorer e) {
        HexCoordinate best = null;
        int bestDist = Integer.MAX_VALUE;
        for (Hex h : map.getAllHexes()) {
            if (!h.getIsExplored()) {
                int dist = e.getPosition().distanceTo(h.getCoordinate());
                if (dist < bestDist) {
                    bestDist = dist;
                    best = h.getCoordinate();
                }
            }
        }
        if (best == null) return;

        // Repeatedly step toward the target while AP remains.
        while (e.getCurrentAP() > 0) {
            List<HexCoordinate> path = PathFinder.findPath(map, e.getPosition(), best, e.getCurrentAP());
            if (path == null || path.size() < 2) {
                // Fall back: step onto the cheapest reachable neighbour toward target.
                HexCoordinate step = null;
                int stepDist = Integer.MAX_VALUE;
                for (HexCoordinate n : e.getPosition().findNeighbours()) {
                    if (!map.containsCoordinate(n)) continue;
                    Hex nh = map.getHex(n);
                    int cost = Constants.MOVE_COST.getOrDefault(nh.getTerrainType(), 1);
                    if (cost <= e.getCurrentAP()) {
                        int d = n.distanceTo(best);
                        if (d < stepDist) { stepDist = d; step = n; }
                    }
                }
                if (step == null) break;
                e.moveTo(map, step);
            } else {
                HexCoordinate next = path.get(1);
                if (!e.moveTo(map, next)) break;
            }
            if (e.getPosition().equals(best)) break;
        }
    }

    /** Recomputes fog of war from current unit and Town Hall positions. */
    public void updateVisibility() {
        for (Hex h : map.getAllHexes()) {
            if (h.isVisible()) h.setVisible(false);
        }
        for (Unit u : player.getUnits()) {
            if (!u.isAlive()) continue;
            for (Hex h : map.getHexesInRadius(u.getPosition(), u.getVisionRadius())) {
                h.setVisible(true);
            }
        }
        for (Building b : player.getBuildings()) {
            if (b.getType() == Constants.BuildingType.TOWN_HALL) {
                for (Hex h : map.getHexesInRadius(b.getPosition(), 2)) {
                    h.setVisible(true);
                }
            }
        }
    }

    public GameMap getMap() { return map; }
    public Player getPlayer() { return player; }
    public int getCurrentTurn() { return currentTurn; }
    public int getMaxTurns() { return MAX_TURNS; }
    public boolean isGameOver() { return gameOver; }
    public String getGameOverReason() { return gameOverReason; }
    public int getFinalScore() { return finalScore; }

    public int getCurrentScore() { return ScoreCalculator.calculate(player, map); }
}
