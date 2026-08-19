package model.military;

import model.HexCoordinate;

public class Catapult extends MilitaryUnit {

    public Catapult(HexCoordinate position) {
        super(
                position,
                50,
                20,
                2,
                2
        );
    }

    @Override
    public MilitaryUnitType getMilitaryUnitType() {
        return MilitaryUnitType.CATAPULT;
    }
}