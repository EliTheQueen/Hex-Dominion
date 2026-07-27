package model.technology;

import model.ResourceAmount;
import model.townhall.TownHallLevel;

public enum TechnologyType {
    SAILING(TownHallLevel.SETTLEMENT, ResourceAmount.of(0, 80, 0, 0), 4),
    STEEL_TOOLS(TownHallLevel.SETTLEMENT, ResourceAmount.of(0, 0, 0, 40), 3),
    DEFENSIVE_ARCHITECTURE(TownHallLevel.CAPITAL, ResourceAmount.of(0, 0, 100, 0), 4);

    TownHallLevel requiredLevel;
    ResourceAmount cost;
    int researchTurns;

    TechnologyType (TownHallLevel requiredLevel, ResourceAmount cost, int researchTurns) {
        if (requiredLevel == null) {
            throw new IllegalArgumentException("requiredLevel cannot be null");
        }
        if (cost == null) {
            throw new IllegalArgumentException("cost cannot be null");
        }
        if (researchTurns <= 0) {
            throw new IllegalArgumentException("researchTurns cannot be negative");
        }
        this.requiredLevel = requiredLevel;
        this.cost = cost;
        this.researchTurns = researchTurns;
    }

    public TownHallLevel getRequiredLevel() {
        return requiredLevel;
    }
    public ResourceAmount getCost() {
        return cost.copy();
    }
    public int getResearchTurns() {
        return researchTurns;
    }

    public boolean isUnlockedAt(TownHallLevel currentLevel) {
        if (currentLevel == null) {
            throw new IllegalArgumentException("currentLevel cannot be null");
        }
        return currentLevel.getLevelNumber() >= requiredLevel.getLevelNumber();
    }
}
