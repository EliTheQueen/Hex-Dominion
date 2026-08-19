package model.military;

import model.HexCoordinate;

public class Cavalry extends MilitaryUnit {

    public Cavalry(HexCoordinate position) {
        super(
                position,
                2,
                8,
                1,
                4
        );
    }

    @Override
    public MilitaryUnitType getMilitaryUnitType() {
        return MilitaryUnitType.CAVALRY;
    }
}