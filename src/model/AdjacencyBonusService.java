package model;

public class AdjacencyBonusService {

    public int calculateBonus(Building building, Player player, GameMap map) {
        if (building == null || player == null || map == null) {
            throw new IllegalArgumentException("arguments must not be null");
        }

        switch (building.getType()) {
            case STONE_MINE:
            case IRON_MINE:
                return deepMineBonus(building, map);

            case LUMBER_MILL:
                return lumberCoastBonus(building, map);

            case FARM:
                return farmPairBonus(building, player, map);

            default:
                return 0;
        }
    }

    private int deepMineBonus(Building building, GameMap map) {
        int mountainCount = 0;

        for (Hex neighbour : map.getNeighboursOf(building.getPosition())) {
            if (neighbour.getTerrainType() == Constants.TerrainType.MOUNTAIN) {
                mountainCount++;
            }
        }

        return mountainCount >= 2 ? 1 : 0;
    }

    private int lumberCoastBonus(Building building, GameMap map) {
        return map.isCoastal(building.getPosition()) ? 2 : 0;
    }

    private int farmPairBonus(Building building, Player player, GameMap map) {
        int bonus = 0;
        for (Hex neighbour : map.getNeighboursOf(building.getPosition())) {
            Building neighbourBuilding = player.getBuildingAt(neighbour.getCoordinate());

            if (neighbourBuilding != null
                    && neighbourBuilding.isActive()
                    && neighbourBuilding.getType() == Constants.BuildingType.FARM
                    && comesBefore(building.getPosition(), neighbour.getCoordinate())) {
                bonus++;
            }
        }
        return bonus;
    }

    private boolean comesBefore(HexCoordinate a, HexCoordinate b) {
        return a.getQ() < b.getQ() || a.getQ() == b.getQ() && a.getR() < b.getR();
    }
}
