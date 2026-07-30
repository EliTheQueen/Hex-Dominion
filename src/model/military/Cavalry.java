package model.military;

import model.HexCoordinate;

public class Cavalry extends MilitaryUnit {
    public Cavalry(HexCoordinate position) {
        super(position, 2, 4, 8, 1);
    }

    @Override
    public MilitaryUnitType getUnitType() {
        return MilitaryUnitType.CAVALRY;
    }
}
