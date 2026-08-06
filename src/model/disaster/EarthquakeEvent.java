package model.disaster;

import model.HexCoordinate;

public class EarthquakeEvent extends DisasterEvent {

    public EarthquakeEvent(HexCoordinate origin) {
        super(DisasterType.EARTHQUAKE, origin);
    }
}