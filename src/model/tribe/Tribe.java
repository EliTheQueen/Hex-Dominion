package model.tribe;

import model.HexCoordinate;
import model.military.MilitaryUnitType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Tribe implements java.io.Serializable {

    private final String id;
    private final String name;
    private final TribeType type;

    private final HexCoordinate campCoordinate;

    private final int maxHp;
    private final TribeRelation relation;

    private int currentHp;
    private boolean discovered;
    private boolean defeated;
    private boolean outpost;

    private int lastTradeTurn = -1;
    private final List<TribeMilitaryUnit> militaryUnits = new ArrayList<>();
    private int guardsCreated;
    private boolean campUnderAttack;
    private HexCoordinate lastAttackerCoordinate;

    public Tribe(
            String name,
            TribeType type,
            HexCoordinate campCoordinate,
            int maxHp
    ) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "name must not be blank"
            );
        }

        if (type == null) {
            throw new IllegalArgumentException(
                    "type must not be null"
            );
        }

        if (campCoordinate == null) {
            throw new IllegalArgumentException(
                    "campCoordinate must not be null"
            );
        }

        if (maxHp <= 0) {
            throw new IllegalArgumentException(
                    "maxHp must be greater than zero"
            );
        }

        this.id = UUID.randomUUID().toString();
        this.name = name.trim();
        this.type = type;
        this.campCoordinate = campCoordinate;
        this.maxHp = maxHp;
        this.currentHp = maxHp;
        this.relation = new TribeRelation();
        addGuard();
        addGuard();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public TribeType getType() {
        return type;
    }

    public HexCoordinate getCampCoordinate() {
        return campCoordinate;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public int getCurrentHp() {
        return currentHp;
    }

    public TribeRelation getRelation() {
        return relation;
    }

    public boolean isDiscovered() {
        return discovered;
    }

    public boolean isDefeated() {
        return defeated;
    }

    public boolean isOutpost() { return outpost; }

    public void discover() {
        discovered = true;
    }

    public void takeDamage(int damage) {
        if (damage < 0) {
            throw new IllegalArgumentException(
                    "damage must not be negative"
            );
        }

        if (defeated) {
            return;
        }

        currentHp = Math.max(0, currentHp - damage);

        if (currentHp == 0) {
            defeated = true;
        }
    }

    public boolean canTradeAt(int turn) {
        if (turn < 0) {
            return false;
        }

        return discovered
                && !defeated
                && relation.isFriendlyOrAllied()
                && lastTradeTurn != turn;
    }

    public void markTradedAt(int turn) {
        if (turn < 0) {
            throw new IllegalArgumentException(
                    "turn must not be negative"
            );
        }

        lastTradeTurn = turn;
    }

    public int getLastTradeTurn() {
        return lastTradeTurn;
    }
    public int getGuardCount() {
        removeDeadUnits();
        return militaryUnits.size();
    }

    public TribeMilitaryUnit addGuard() {
        if (defeated) return null;
        MilitaryUnitType[] roster = guardRoster(type);
        MilitaryUnitType category = roster[guardsCreated % roster.length];
        guardsCreated++;
        TribeMilitaryUnit unit = new TribeMilitaryUnit(id, category, campCoordinate);
        militaryUnits.add(unit);
        return unit;
    }

    public int removeGuards(int amount) {
        int removed = Math.min(Math.max(0, amount), getGuardCount());
        for (int i = 0; i < removed; i++) militaryUnits.get(i).kill();
        removeDeadUnits();
        return removed;
    }

    public List<TribeMilitaryUnit> getMilitaryUnits() {
        removeDeadUnits();
        return Collections.unmodifiableList(new ArrayList<>(militaryUnits));
    }

    public List<TribeMilitaryUnit> getMilitaryUnitsAt(HexCoordinate coordinate) {
        List<TribeMilitaryUnit> result = new ArrayList<>();
        if (coordinate == null) return result;
        for (TribeMilitaryUnit unit : getMilitaryUnits()) {
            if (unit.getPosition().equals(coordinate)) result.add(unit);
        }
        return result;
    }

    public void removeDeadUnits() { militaryUnits.removeIf(unit -> !unit.isAlive()); }

    public void resetMilitaryActionPoints() {
        for (TribeMilitaryUnit unit : getMilitaryUnits()) unit.resetAP();
    }

    public void disbandMilitary() {
        for (TribeMilitaryUnit unit : militaryUnits) unit.kill();
        militaryUnits.clear();
    }

    public boolean isCampUnderAttack() { return campUnderAttack; }
    public void setCampUnderAttack(boolean value) {
        campUnderAttack = value;
        if (!value) lastAttackerCoordinate = null;
    }

    public void recordCampAttack(HexCoordinate attackerCoordinate) {
        if (attackerCoordinate == null) throw new IllegalArgumentException("attacker coordinate is required");
        campUnderAttack = true;
        lastAttackerCoordinate = attackerCoordinate;
    }

    public HexCoordinate getLastAttackerCoordinate() { return lastAttackerCoordinate; }

    public void convertToOutpost() {
        if (!defeated) throw new IllegalStateException("only a defeated camp can become an Outpost");
        outpost = true;
        campUnderAttack = false;
        lastAttackerCoordinate = null;
        disbandMilitary();
    }

    private static MilitaryUnitType[] guardRoster(TribeType type) {
        return switch (type) {
            case FARMER -> new MilitaryUnitType[]{MilitaryUnitType.SWORDSMAN,
                    MilitaryUnitType.SWORDSMAN, MilitaryUnitType.ARCHER};
            case WARRIOR -> new MilitaryUnitType[]{MilitaryUnitType.SWORDSMAN,
                    MilitaryUnitType.ARCHER, MilitaryUnitType.CAVALRY,
                    MilitaryUnitType.SWORDSMAN, MilitaryUnitType.ARCHER};
            case MERCHANT -> new MilitaryUnitType[]{MilitaryUnitType.ARCHER,
                    MilitaryUnitType.ARCHER, MilitaryUnitType.SWORDSMAN};
            case MOUNTAIN -> new MilitaryUnitType[]{MilitaryUnitType.CAVALRY,
                    MilitaryUnitType.SWORDSMAN, MilitaryUnitType.ARCHER};
            case COASTAL -> new MilitaryUnitType[]{MilitaryUnitType.ARCHER,
                    MilitaryUnitType.SWORDSMAN, MilitaryUnitType.CAVALRY};
        };
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof Tribe)) {
            return false;
        }

        Tribe tribe = (Tribe) other;
        return id.equals(tribe.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
