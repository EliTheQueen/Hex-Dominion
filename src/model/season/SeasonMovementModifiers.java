package model.season;

public final class SeasonMovementModifiers {

    private SeasonMovementModifiers() {
    }

    public static int getAdditionalCost(Season season, MovementDomain movementDomain) {
        if (season == null || movementDomain == null) {
            throw new IllegalArgumentException("arguments must not be null");
        }

        if (season == Season.WINTER && movementDomain == MovementDomain.LAND) {
            return 1;
        }

        if (season == Season.AUTUMN && movementDomain == MovementDomain.WATER) {
            return 1;
        }

        return 0;
    }

    public static int calculateFinalCost(int baseCost, Season season, MovementDomain movementDomain) {
        if (baseCost < 0) {
            throw new IllegalArgumentException("baseCost must not be negative");
        }

        return baseCost + getAdditionalCost(season, movementDomain);
    }
}