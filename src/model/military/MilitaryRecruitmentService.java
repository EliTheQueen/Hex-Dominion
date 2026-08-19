package model.military;

import model.Building;
import model.Constants;
import model.GameMap;
import model.Hex;
import model.HexCoordinate;
import model.Player;
import model.Unit;
import model.townhall.TownHallLevel;

public class MilitaryRecruitmentService implements java.io.Serializable {

    private final Player player;
    private final GameMap map;
    private final UnitFactory unitFactory;

    public MilitaryRecruitmentService(Player player, GameMap map) {
        if (player == null || map == null) {
            throw new IllegalArgumentException("player and map must not be null");
        }

        this.player = player;
        this.map = map;
        this.unitFactory = new UnitFactory();
    }

    public boolean canRecruit(
            MilitaryUnitType type,
            HexCoordinate position,
            TownHallLevel townHallLevel
    ) {
        if (type == null || position == null || townHallLevel == null) {
            return false;
        }

        Hex hex = map.getHex(position);

        if (hex == null) {
            return false;
        }

        if (!player.isInTerritory(position)) {
            return false;
        }

        if (!isUnlocked(type, townHallLevel)) {
            return false;
        }

        if (getMilitaryUnitCount() >= getCap(townHallLevel)) {
            return false;
        }

        return hasStackSpace(type, position);
    }

    public MilitaryUnit recruit(
            MilitaryUnitType type,
            HexCoordinate position,
            TownHallLevel townHallLevel
    ) {
        if (!canRecruit(type, position, townHallLevel)) {
            return null;
        }

        MilitaryUnit unit = unitFactory.createUnit(type, position);
        player.addUnit(unit);

        return unit;
    }

    public int getMilitaryUnitCount() {
        int count = 0;

        for (Unit unit : player.getUnits()) {
            if (unit instanceof MilitaryUnit && unit.isAlive()) {
                count++;
            }
        }

        return count;
    }

    public int getCap(TownHallLevel level) {
        switch (level) {
            case BASE_CAMP:
                return 5;

            case SETTLEMENT:
                return 10;

            case CAPITAL:
                return 15;

            default:
                throw new IllegalArgumentException("Unknown Town Hall level");
        }
    }

    public boolean isUnlocked(MilitaryUnitType type, TownHallLevel level) {
        switch (type) {
            case SWORDSMAN:
                return true;

            case ARCHER:
                return level.getLevelNumber() >= 2;

            case CAVALRY:
                return level.getLevelNumber() >= 2 && hasActiveStable();

            case CATAPULT:
                return level.getLevelNumber() >= 3;

            default:
                return false;
        }
    }

    private boolean hasActiveStable() {
        for (Building building : player.getBuildings()) {
            if (building.isActive()
                    && building.getType() == Constants.BuildingType.STABLE) {

                return true;
            }
        }

        return false;
    }

    private boolean hasStackSpace(MilitaryUnitType type, HexCoordinate position) {
        Hex hex = map.getHex(position);

        if (hex == null) {
            return false;
        }

        MilitaryHex militaryHex = new MilitaryHex(hex);

        for (Unit existing : player.getUnits()) {
            if (!(existing instanceof MilitaryUnit)) {
                continue;
            }

            MilitaryUnit militaryUnit = (MilitaryUnit) existing;

            if (militaryUnit.isDead()) {
                continue;
            }

            if (!position.equals(militaryUnit.getPosition())) {
                continue;
            }

            militaryHex.addUnit(militaryUnit);
        }

        MilitaryUnit candidate = unitFactory.createUnit(type, position);

        try {
            militaryHex.addUnit(candidate);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
