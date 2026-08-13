package model.disaster;

import model.HexCoordinate;

public class TornadoEvent extends DisasterEvent {

    public TornadoEvent(HexCoordinate origin) {
        super(DisasterType.TORNADO, origin);
    }
}