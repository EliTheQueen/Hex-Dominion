package model.military;

import model.HexCoordinate;

public class Archer extends MilitaryUnit{
    public Archer(HexCoordinate position) {
        super(position, 1, 2, 6, 2);
    }

    @Override
    public MilitaryUnitType getUnitType() {
        return MilitaryUnitType.ARCHER;
    }
}
