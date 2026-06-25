package model;

/** A single queued item in a Town Hall production queue: a unit or a technology. */
public class ProductionTask {
    public enum Kind { UNIT, TECH }

    private final Kind kind;
    private final Constants.UnitType unitType;
    private final Constants.TechnologyType techType;
    private final int totalTurns;
    private int turnsRemaining;

    private ProductionTask(Kind kind, Constants.UnitType unit, Constants.TechnologyType tech, int turns) {
        this.kind = kind;
        this.unitType = unit;
        this.techType = tech;
        this.totalTurns = turns;
        this.turnsRemaining = turns;
    }

    public static ProductionTask forUnit(Constants.UnitType type) {
        return new ProductionTask(Kind.UNIT, type, null,
                Constants.UNIT_BUILD_TURNS.getOrDefault(type, 2));
    }

    public static ProductionTask forTech(Constants.TechnologyType tech) {
        return new ProductionTask(Kind.TECH, null, tech, 3);
    }

    public Kind getKind() { return kind; }
    public Constants.UnitType getUnitType() { return unitType; }
    public Constants.TechnologyType getTechType() { return techType; }
    public int getTotalTurns() { return totalTurns; }
    public int getTurnsRemaining() { return turnsRemaining; }

    /** Advances the task by one turn. */
    public void tick() {
        if (turnsRemaining > 0) turnsRemaining--;
    }

    public boolean isComplete() { return turnsRemaining <= 0; }

    public String getLabel() {
        String name = kind == Kind.UNIT
                ? unitType.name().replace("_", " ")
                : techType.name().replace("_", " ");
        return name + " (" + turnsRemaining + " turn" + (turnsRemaining == 1 ? "" : "s") + ")";
    }
}
