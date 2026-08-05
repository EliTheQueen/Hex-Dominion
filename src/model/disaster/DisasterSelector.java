package model.disaster;

import model.season.Season;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DisasterSelector {

    public DisasterType select(Season season, boolean navalSystemEnabled, Random random) {
        if (season == null || random == null) {
            throw new IllegalArgumentException("arguments must not be null");
        }

        List<DisasterType> eligible = new ArrayList<>();

        for (DisasterType type : DisasterType.values()) {

            if (DisasterEligibility.isEligible(type, season, navalSystemEnabled)) {
                eligible.add(type);
            }
        }

        if (eligible.isEmpty()) {
            return null;
        }

        return eligible.get(random.nextInt(eligible.size()));
    }
}