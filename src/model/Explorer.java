package model;

public class Explorer extends Unit{
    private boolean autoExploreMode;

    public Explorer(HexCoordinate pos) {
        super(pos, Constants.UnitType.EXPLORER);
        this.autoExploreMode = false;
    }

    public boolean isAutoExploreMode() { return autoExploreMode; }
    public void toggleAutoExplore() {
        autoExploreMode = !autoExploreMode;
        state = autoExploreMode ? Constants.UnitState.AUTO_EXPLORE : Constants.UnitState.IDLE;
    }
}
