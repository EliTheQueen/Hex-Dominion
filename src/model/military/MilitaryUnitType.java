package model.military;

import model.ResourceAmount;

public enum MilitaryUnitType {
    SWORDSMAN(ResourceAmount.of(20, 10, 0, 0), 2),
    ARCHER(ResourceAmount.of(10, 15, 0, 0), 2),
    CAVALRY(ResourceAmount.of(20, 10, 0, 5), 3),
    CATAPULT(ResourceAmount.of(10, 25, 15, 0), 4);

    private final ResourceAmount cost;
    private final int trainingTurns;
    MilitaryUnitType(ResourceAmount cost, int trainingTurns) {
        this.cost = cost; this.trainingTurns = trainingTurns;
    }
    public ResourceAmount getCost() { return cost.copy(); }
    public int getTrainingTurns() { return trainingTurns; }
}
