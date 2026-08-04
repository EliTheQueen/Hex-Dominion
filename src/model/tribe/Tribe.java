package model.tribe;

import model.HexCoordinate;

import java.util.Objects;
import java.util.UUID;

public class Tribe {

    private final String id;
    private final String name;
    private final TribeType type;

    private final HexCoordinate campCoordinate;

    private final int maxHp;
    private final TribeRelation relation;

    private int currentHp;
    private boolean discovered;
    private boolean defeated;

    private int lastTradeTurn = -1;

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