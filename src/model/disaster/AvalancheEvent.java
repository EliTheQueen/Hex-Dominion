package model.disaster;

import model.HexCoordinate;

public class AvalancheEvent extends DisasterEvent {

    public AvalancheEvent(HexCoordinate origin) {
        super(DisasterType.AVALANCHE, origin);
    }
}