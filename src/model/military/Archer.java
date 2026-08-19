package model.military;

import model.HexCoordinate;

public class Archer extends MilitaryUnit {

    public Archer(HexCoordinate position) {
        super(
                position,
                1,
                6,
                2,
                2
        );
    }

    @Override
    public MilitaryUnitType getMilitaryUnitType() {
        return MilitaryUnitType.ARCHER;
    }
}