package model.military;

import model.Constants;
import model.HexCoordinate;

public class Cavalry extends MilitaryUnit {
    public Cavalry(HexCoordinate position) {
        super(position, Constants.UnitType.MILITARY, 2, 8, 1);
    }

    @Override
    public MilitaryUnitType getMilitaryUnitType() {
        return MilitaryUnitType.CAVALRY;
    }
}
