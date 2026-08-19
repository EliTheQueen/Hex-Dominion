package model;

import java.util.Objects;

public class HexEdge implements java.io.Serializable {

    private final HexCoordinate first;
    private final HexCoordinate second;
    private Wall wall;
    private boolean bridge;

    public HexEdge(HexCoordinate first, HexCoordinate second) {
        if (first == null || second == null) {
            throw new IllegalArgumentException("coordinates must not be null");
        }

        if (!first.findNeighbours().contains(second)) {
            throw new IllegalArgumentException("coordinates must be neighbours");
        }

        this.first = first;
        this.second = second;
        this.bridge = false;
    }

    public HexCoordinate getFirst() {
        return first;
    }

    public HexCoordinate getSecond() {
        return second;
    }

    public boolean connects(HexCoordinate a, HexCoordinate b) {
        return (first.equals(a) && second.equals(b)) || (first.equals(b) && second.equals(a));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof HexEdge))
            return false;

        HexEdge other = (HexEdge) obj;

        return connects(other.first, other.second);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(first) + Objects.hashCode(second);
    }

    public boolean hasWall() {
        return wall != null && !wall.isDestroyed();
    }

    public Wall getWall() {
        return wall;
    }

    public void buildWall(int maxHp) {
        if (hasWall()) {
            throw new IllegalStateException("wall already exists");
        }

        wall = new Wall(maxHp);
    }

    public void removeWall() {
        wall = null;
    }

    public boolean hasBridge() {
        return bridge;
    }

    public void buildBridge() {
        bridge = true;
    }

    public void removeBridge() {
        bridge = false;
    }
}
