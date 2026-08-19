package model.disaster.area;

import model.HexCoordinate;

import java.util.HashSet;
import java.util.Set;

public class DisasterArea implements java.io.Serializable {

    private final HexCoordinate origin;
    private final Set<HexCoordinate> affectedCoordinates;

    public DisasterArea(HexCoordinate origin, Set<HexCoordinate> affectedCoordinates) {
        if (origin == null) {
            throw new IllegalArgumentException("origin cannot be null");
        }
        if (affectedCoordinates == null || affectedCoordinates.contains(null)) {
            throw new IllegalArgumentException("affectCoordinates cannot be null or contain null elements");
        }

        if (!affectedCoordinates.contains(origin)) {
            throw new IllegalArgumentException("origin must be included in affected coordinates");
        }

        this.origin = origin;
        Set<HexCoordinate> affectCoordinatesCopy = new HashSet<>(affectedCoordinates);
        this.affectedCoordinates = affectCoordinatesCopy;
    }

    public HexCoordinate getOrigin() {
        return origin;
    }

    public Set<HexCoordinate> getAffectedCoordinates() {
        return new HashSet<>(affectedCoordinates);
    }

    public boolean contains(HexCoordinate coordinate) {
        if (coordinate == null) {
            throw new IllegalArgumentException("coordinate cannot be null");
        }
        return affectedCoordinates.contains(coordinate);
    }

    public int size() {
        return affectedCoordinates.size();
    }
}
