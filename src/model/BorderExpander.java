package model;

public class BorderExpander extends Unit {
    public BorderExpander(HexCoordinate pos) {
        super(pos, Constants.UnitType.BORDER_EXPANDER);
    }

    public boolean canExpand() { return currentAP >= 2; }

    public boolean expand(GameMap map, Player player) {
        if (!canExpand()) return false;
        Hex here = map.getHex(position);
        if (here == null || !here.getIsExplored()) return false;

        player.expandTerritory(position);
        for (HexCoordinate neighbor : position.findNeighbours()) {
            Hex nh = map.getHex(neighbor);
            if (nh != null && nh.getIsExplored()) {
                player.expandTerritory(neighbor);
            }
        }
        spendAP(2);
        state = Constants.UnitState.IDLE;
        return true;
    }
}
