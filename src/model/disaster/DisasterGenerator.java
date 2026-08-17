package model.disaster;

import model.GameMap;
import model.HexCoordinate;
import model.Player;
import model.season.Season;
import model.disaster.area.RadiusDisasterAreaCalculator;

import java.util.Random;

public class DisasterGenerator {

    private final DisasterOccurrencePolicy occurrencePolicy;
    private final DisasterSelector disasterSelector;
    private final DisasterOriginSelector originSelector;

    public DisasterGenerator(DisasterOccurrencePolicy occurrencePolicy, DisasterSelector disasterSelector, DisasterOriginSelector originSelector) {
        if (occurrencePolicy == null || disasterSelector == null || originSelector == null) {
            throw new IllegalArgumentException("dependencies must not be null");
        }

        this.occurrencePolicy = occurrencePolicy;
        this.disasterSelector = disasterSelector;
        this.originSelector = originSelector;
    }

    public DisasterEvent generate(
            Season season,
            boolean navalSystemEnabled,
            boolean bearAttackAllowed,
            GameMap map,
            Player player,
            Random random
    ) {
        if (season == null || map == null || player == null || random == null) {
            throw new IllegalArgumentException("arguments must not be null");
        }

        if (!occurrencePolicy.shouldOccur(random)) {
            return null;
        }

        DisasterType disasterType =
                disasterSelector.select(
                        season,
                        navalSystemEnabled,
                        bearAttackAllowed,
                        random
                );

        if (disasterType == null) {
            return null;
        }

        HexCoordinate origin = originSelector.select(disasterType, map, random);

        if (origin == null) {
            return null;
        }

        switch (disasterType) {
            case EARTHQUAKE:
                return new EarthquakeEvent(
                        origin,
                        map,
                        player,
                        new RadiusDisasterAreaCalculator(2),
                        new DisasterTargetCollector()
                );

            case BEAR_ATTACK:
                return new BearAttackEvent(
                        origin,
                        map,
                        player
                );

            case TORNADO:
                return new TornadoEvent(
                        origin,
                        map,
                        player,
                        random,
                        new DisasterPathBuilder(),
                        new DisasterTargetCollector()
                );

            case AVALANCHE:
                return new AvalancheEvent(
                        origin,
                        map,
                        player,
                        random,
                        new DisasterPathBuilder(),
                        new DisasterTargetCollector()
                );

            case VOLCANIC_ERUPTION:
                return new VolcanicEruptionEvent(
                        origin,
                        map,
                        player,
                        random,
                        new DisasterPathBuilder(),
                        new DisasterTargetCollector()
                );

            case FLOOD:
                return new FloodEvent(
                        origin,
                        map,
                        player,
                        new DisasterTargetCollector()
                );

            case TSUNAMI:
                return new TsunamiEvent(
                        origin,
                        map,
                        player,
                        new DisasterTargetCollector(),
                        random
                );

            case SEA_STORM:
                return new SeaStormEvent(
                        origin,
                        map,
                        player,
                        new RadiusDisasterAreaCalculator(2),
                        new DisasterTargetCollector()
                );

            default:
                throw new UnsupportedOperationException("event creation is not implemented for " + disasterType);
        }
    }
}