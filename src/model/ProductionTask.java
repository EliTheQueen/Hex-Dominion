package model;

/** Serialization-only companion to the retired ProductionQueue. */
@Deprecated
final class ProductionTask implements java.io.Serializable {
    private static final long serialVersionUID = 5959157121002252976L;

    enum Kind { UNIT, TECH }

    @SuppressWarnings("unused") private Kind kind;
    @SuppressWarnings("unused") private Constants.UnitType unitType;
    @SuppressWarnings("unused") private Constants.TechnologyType techType;
    @SuppressWarnings("unused") private int totalTurns;
    @SuppressWarnings("unused") private int turnsRemaining;
}
