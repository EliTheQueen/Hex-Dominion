package model.tribe;

import model.HexCoordinate;
import model.military.MilitaryUnit;
import model.military.MilitaryUnitType;

/** A real military unit owned by a tribe and resolved by the common combat engine. */
public final class TribeMilitaryUnit extends MilitaryUnit {
    private final String tribeId;
    private final MilitaryUnitType category;

    public TribeMilitaryUnit(String tribeId, MilitaryUnitType category, HexCoordinate position) {
        super(position, hitPoints(category), structureDamage(category), range(category), actionPoints(category));
        if (tribeId == null || tribeId.isBlank() || category == null) {
            throw new IllegalArgumentException("tribe id and military category are required");
        }
        if (category == MilitaryUnitType.CATAPULT) {
            throw new IllegalArgumentException("tribe guards map only to Sword, Archer, or Cavalry");
        }
        this.tribeId = tribeId;
        this.category = category;
    }

    public String getTribeId() { return tribeId; }
    @Override public MilitaryUnitType getMilitaryUnitType() { return category; }

    private static int hitPoints(MilitaryUnitType type) {
        requireCategory(type);
        return type == MilitaryUnitType.CAVALRY ? 2 : 1;
    }

    private static int structureDamage(MilitaryUnitType type) {
        requireCategory(type);
        return switch (type) {
            case SWORDSMAN -> 10;
            case ARCHER -> 6;
            case CAVALRY -> 8;
            case CATAPULT -> throw new IllegalArgumentException("unsupported tribe category");
        };
    }

    private static int range(MilitaryUnitType type) {
        requireCategory(type);
        return type == MilitaryUnitType.ARCHER ? 2 : 1;
    }

    private static int actionPoints(MilitaryUnitType type) {
        requireCategory(type);
        return type == MilitaryUnitType.CAVALRY ? 4 : 2;
    }

    private static void requireCategory(MilitaryUnitType type) {
        if (type == null || type == MilitaryUnitType.CATAPULT) {
            throw new IllegalArgumentException("tribe unit category must be Sword, Archer, or Cavalry");
        }
    }
}
