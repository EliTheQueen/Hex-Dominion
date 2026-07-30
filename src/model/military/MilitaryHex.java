package model.military;

import model.Constants;
import model.Hex;
import model.HexCoordinate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MilitaryHex {

    private final List<MilitaryUnit> units;
    private final Hex hex;

    public MilitaryHex(Hex hex) {
        if (hex == null) throw new NullPointerException();

        this.hex = hex;
        this.units = new ArrayList<>();
    }

    public HexCoordinate getCoordinate() {
        return hex.getCoordinate();
    }

    public Hex getHex() {
        return hex;
    }

    public void addUnit(MilitaryUnit unit) {
        if (unit == null) {
            throw new IllegalArgumentException("Unit cannot be null");
        }

        if (units.contains(unit)) {
            throw new IllegalArgumentException("Unit already exists in this hex");
        }

        removeDeadUnits();

        MilitaryUnitType unitType = unit.getMilitaryUnitType();

        if (howMany(unitType) >= getCapacity(unitType))
            {
            throw  new IllegalArgumentException("Capacity reached for unit type: " + unit.getUnitType());
            }

        unit.setPosition(getCoordinate());
        units.add(unit);
    }

    public boolean isAlive(MilitaryUnit unit) {
        if (unit == null) {
            throw new IllegalArgumentException("Unit cannot be null");
        }

        if (!units.contains(unit)) {
            throw new IllegalArgumentException("Unit does not exist");
        }

        return !unit.isDead();
    }

    public void removeDeadUnits() {
        units.removeIf(MilitaryUnit::isDead);
    }

    public List<MilitaryUnit> getUnits() {
        return new ArrayList<>(units);
    }

    public List<MilitaryUnit> getAliveUnits() {
        List<MilitaryUnit> aliveUnits = new ArrayList<>();

        for (MilitaryUnit unit : units) {
            if (!unit.isDead()) {
                aliveUnits.add(unit);
            }
        }

        return aliveUnits;
    }

    public MilitaryUnit getNextDamageTarget() {
        return units.stream().filter(unit -> !unit.isDead()).min(Comparator.comparingInt(this::getDamagePriority)).orElse(null);
    }

    private int getDamagePriority(MilitaryUnit unit) {

        return switch (unit.getMilitaryUnitType()) {
            case SWORDSMAN -> 1;
            case ARCHER -> 2;
            case CAVALRY -> 3;
            case CATAPULT -> 4;
        };
    }

    public int howMany(MilitaryUnitType militaryUnitType) {
        if (militaryUnitType == null) {
            throw new IllegalArgumentException("Military Unit Type cannot be null");
        }

        int howMany = 0;
        List<MilitaryUnit> aliveUnits = getAliveUnits();

        for (MilitaryUnit unit : aliveUnits) {
            if (unit.getUnitType().equals(militaryUnitType)) {
                howMany++;
            }
        }
        return howMany;
    }

    private int getCapacity(MilitaryUnitType militaryUnitType) {
        if (militaryUnitType == null) {
            throw new IllegalArgumentException("Military Unit Type cannot be null");
        }

        switch (militaryUnitType) {
            case CATAPULT:
                return 1;

            case CAVALRY:
                return 1;

            case SWORDSMAN:
                return 2;

            case ARCHER:
                return 2;

            default:
                throw new IllegalArgumentException("Invalid military unit");
        }
    }
}