package model.season;

import model.Constants;

public final class SeasonProductionModifiers {

    private SeasonProductionModifiers() {
    }

    public static int getFlatBonus(Season season, Constants.BuildingType buildingType) {
        if (season == null || buildingType == null) {
            throw new IllegalArgumentException("arguments must not be null");
        }

        switch (season) {
            case SPRING:
                if (buildingType == Constants.BuildingType.FARM
                        || buildingType == Constants.BuildingType.STABLE) {
                    return 1;
                }

                return 0;

            case WINTER:
                if (buildingType == Constants.BuildingType.FARM) {
                    return -1;
                }

                return 0;

            case SUMMER:
            case AUTUMN:
                return 0;

            default:
                throw new IllegalStateException("Unsupported season: " + season);
        }
    }

    public static int apply(
            int baseProduction,
            Season season,
            Constants.BuildingType buildingType
    ) {
        if (baseProduction < 0) {
            throw new IllegalArgumentException("baseProduction must not be negative");
        }

        int result = baseProduction + getFlatBonus(season, buildingType);

        return Math.max(0, result);
    }
}