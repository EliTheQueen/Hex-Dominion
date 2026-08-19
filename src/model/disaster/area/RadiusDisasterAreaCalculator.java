package model.disaster.area;

import model.GameMap;
import model.HexCoordinate;

import java.util.HashSet;
import java.util.Set;

public class RadiusDisasterAreaCalculator implements DisasterAreaCalculator {

    private final int radius;

    public RadiusDisasterAreaCalculator(int radius) {
        if (radius < 0) {
            throw new IllegalArgumentException("radius must not be negative");
        }

        this.radius = radius;
    }
    @Override
    public DisasterArea calculate(HexCoordinate origin, GameMap map) {
        if (origin == null) {
            throw new IllegalArgumentException("origin cannot be null");
        }
        if (map == null) {
            throw new IllegalArgumentException("map cannot be null");
        }
        if (!map.containsCoordinate(origin)) {
            throw new IllegalArgumentException("origin is not in map");
        }

        Set<HexCoordinate> affectedCoordinates = new HashSet<>();

        for (HexCoordinate coordinate : map.getHexCoordinatesInRadius(origin, radius)) {
            affectedCoordinates.add(coordinate);
        }

        return new DisasterArea(origin, affectedCoordinates);
    }
}
