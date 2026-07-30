package model.military;

import model.HexCoordinate;

public class Catapult extends MilitaryUnit {
    public Catapult(HexCoordinate position) {
        super(position, 50, 2, 20, 2);
    }

    @Override
    public MilitaryUnitType getUnitType() {
        return MilitaryUnitType.CATAPULT;
    }
}
