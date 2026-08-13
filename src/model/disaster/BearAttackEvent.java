package model.disaster;

import model.HexCoordinate;

public class BearAttackEvent extends DisasterEvent {

    public BearAttackEvent(HexCoordinate origin) {
        super(DisasterType.BEAR_ATTACK, origin);
    }
}