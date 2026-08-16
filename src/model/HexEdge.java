package model;

import java.util.Objects;

public class HexEdge {

    private final HexCoordinate first;
    private final HexCoordinate second;

    public HexEdge(HexCoordinate first, HexCoordinate second) {
        if (first == null || second == null) {
            throw new IllegalArgumentException("coordinates must not be null");
        }

        if (!first.findNeighbours().contains(second)) {
            throw new IllegalArgumentException("coordinates must be neighbours");
        }

        this.first = first;
        this.second = second;
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
}