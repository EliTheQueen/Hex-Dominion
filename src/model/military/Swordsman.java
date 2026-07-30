package model.military;

import model.Constants;
import model.HexCoordinate;

public class Swordsman extends MilitaryUnit {

    public Swordsman(HexCoordinate position) {
        super(position, Constants.UnitType.MILITARY, 1, 10, 1);
    }

    @Override
    public MilitaryUnitType getMilitaryUnitType() {
        return MilitaryUnitType.SWORDSMAN;
    }
}
