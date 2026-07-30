package model.military;

import model.HexCoordinate;

public class Swordsman extends MilitaryUnit {

    public Swordsman(HexCoordinate position) {
        super(position, 1, 2, 10, 1);
    }

    @Override
    public MilitaryUnitType getUnitType() {
        return MilitaryUnitType.SWORDSMAN;
    }
}
