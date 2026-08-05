package model.disaster;

import java.util.Random;

public class DisasterOccurrencePolicy {

    public static final double DEFAULT_PROBABILITY = 0.05;

    private final double probability;

    public DisasterOccurrencePolicy() {
        this(DEFAULT_PROBABILITY);
    }

    public DisasterOccurrencePolicy(double probability) {
        if (probability < 0.0 || probability > 1.0) {
            throw new IllegalArgumentException("probability must be between zero and one");
        }

        this.probability = probability;
    }

    public boolean shouldOccur(Random random) {
        if (random == null) {
            throw new IllegalArgumentException("random must not be null");
        }

        return random.nextDouble() < probability;
    }

    public double getProbability() {
        return probability;
    }
}