package model;

/** Computes the player's score from territory, buildings, tech, exploration and resources. */
public class ScoreCalculator {

    public static int calculate(Player player, GameMap map) {
        return territoryScore(player)
             + buildingScore(player)
             + techScore(player)
             + explorationScore(map)
             + resourceScore(player);
    }

    public static int territoryScore(Player p) { return p.getTerritorySize() * 5; }

    public static int buildingScore(Player p) { return p.getActiveBuildingCount() * 10; }

    public static int techScore(Player p) { return p.getResearched().size() * 15; }

    public static int explorationScore(GameMap map) {
        int s = 0;
        for (Hex h : map.getAllHexes()) {
            if (h.getIsExplored()) s += 2;
        }
        return s;
    }

    public static int resourceScore(Player p) {
        ResourceStorage rs = p.getResources();
        int total = rs.get(Constants.ResourceType.FOOD)
                  + rs.get(Constants.ResourceType.WOOD)
                  + rs.get(Constants.ResourceType.STONE)
                  + rs.get(Constants.ResourceType.IRON);
        return total / 10;
    }
}
