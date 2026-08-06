package model.disaster;

import model.GameMap;
import model.HexCoordinate;
import model.disaster.area.DisasterArea;
import model.disaster.area.DisasterAreaCalculator;

public class EarthquakeEvent extends DisasterEvent {

    private final GameMap map;
    private final DisasterAreaCalculator areaCalculator;

    private DisasterArea affectedArea;

    public EarthquakeEvent(HexCoordinate origin, GameMap map, DisasterAreaCalculator areaCalculator) {
        super(DisasterType.EARTHQUAKE, origin);

        if (map == null || areaCalculator == null) {
            throw new IllegalArgumentException("dependencies must not be null");
        }

        this.map = map;
        this.areaCalculator = areaCalculator;
    }

    @Override
    protected void onStarted() {
        affectedArea = areaCalculator.calculate(getOrigin(), map);
    }

    public DisasterArea getAffectedArea() {
        return affectedArea;
    }
}