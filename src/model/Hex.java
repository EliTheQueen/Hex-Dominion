package model;

public class Hex {

    private final HexCoordinate coordinate;
    private boolean isExplored = false;
    private boolean isExpanded = false;

    public Hex(HexCoordinate coordinate) {
        this.coordinate = coordinate;
    };

    public boolean isExplored() { return isExplored; }
    public boolean isExpanded() { return isExpanded; }
    public void setExplored(boolean isExplored) { this.isExplored = isExplored; }
    public void setExpanded(boolean isExpanded) { this.isExpanded = isExpanded; }
}
