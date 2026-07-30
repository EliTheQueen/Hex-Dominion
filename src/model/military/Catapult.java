package model.military;

import model.Constants;
import model.HexCoordinate;

public class Catapult extends MilitaryUnit {
    public Catapult(HexCoordinate position) {
        super(position, Constants.UnitType.MILITARY, 50, 20, 2);
    }

    @Override
    public MilitaryUnitType getMilitaryUnitType() {
        return MilitaryUnitType.CATAPULT;
    }
}
