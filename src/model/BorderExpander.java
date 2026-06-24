package model;

public class BorderExpander extends Unit {
    public BorderExpander(HexCoordinate pos) {
        super(pos, Constants.UnitType.BORDER_EXPANDER);
    }

    public boolean canExpand() { return currentAP >= 2; }

    /** Claims the current hex and all of its in-bounds neighbours for the player. */
    public boolean expand(GameMap map, Player player) {
        if (!canExpand()) return false;
        player.expandTerritory(position);
        for (HexCoordinate neighbor : position.findNeighbours()) {
            if (map.containsCoordinate(neighbor)) {
                player.expandTerritory(neighbor);
            }
        }
        spendAP(2);
        state = Constants.UnitState.IDLE;
        return true;
    }
}
