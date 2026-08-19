package model.military;

import model.HexCoordinate;

public class Swordsman extends MilitaryUnit {

    public Swordsman(HexCoordinate position) {
        super(
                position,
                1,
                10,
                1,
                2
        );
    }

    @Override
    public MilitaryUnitType getMilitaryUnitType() {
        return MilitaryUnitType.SWORDSMAN;
    }
}