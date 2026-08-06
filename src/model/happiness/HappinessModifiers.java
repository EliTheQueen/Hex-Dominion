package model.happiness;

public final class HappinessModifiers {

    //قرار نیست از آن Object ساخته شود.
    private HappinessModifiers() {
    }

    public static double productionMultiplier(HappinessLevel level) {
        requireLevel(level);

        if (level == HappinessLevel.GOLDEN_AGE) {
            return 1.10;
        }

        return 1.0;
    }

    public static int workerProductionPenalty(HappinessLevel level) {
        requireLevel(level);

        if (level == HappinessLevel.UNHAPPY || level == HappinessLevel.REVOLT) {
            return 1;
        }

        return 0;
    }

    public static int actionPointPenalty(HappinessLevel level) {
        requireLevel(level);

        if (level == HappinessLevel.REVOLT) {
            return 1;
        }

        return 0;
    }

    public static int applyProductionModifiers(int baseProduction, int workerCount, HappinessLevel level) {
        if (baseProduction < 0) {
            throw new IllegalArgumentException("baseProduction must not be negative");
        }

        if (workerCount < 0) {
            throw new IllegalArgumentException("workerCount must not be negative");
        }

        requireLevel(level);

        int workerPenalty = workerProductionPenalty(level) * workerCount;

        int afterPenalty = Math.max(0, baseProduction - workerPenalty);

        return (int) Math.floor(afterPenalty * productionMultiplier(level));
    }

    private static void requireLevel(HappinessLevel level) {
        if (level == null) {
            throw new IllegalArgumentException("level must not be null");
        }
    }
}