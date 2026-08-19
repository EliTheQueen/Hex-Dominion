package model.military;

import model.HexCoordinate;

public class UnitFactory implements java.io.Serializable {

    public MilitaryUnit createUnit(MilitaryUnitType militaryUnitType, HexCoordinate position) {
        if (militaryUnitType == null) {
            throw  new IllegalArgumentException("Military Unit Type cannot be null");
        }
        if (position == null) {
            throw  new IllegalArgumentException("HexCoordinate cannot be null");
        }

        switch (militaryUnitType) {
            case SWORDSMAN:
                return new Swordsman(position);

            case ARCHER:
                return new Archer(position);

            case CAVALRY:
                return new Cavalry(position);

            case CATAPULT:
                return new Catapult(position);

            default:
                throw  new IllegalArgumentException("Invalid Military Unit Type");
        }
    }
}
