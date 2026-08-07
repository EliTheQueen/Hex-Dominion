package model.disaster;

import model.GameMap;
import model.HexCoordinate;
import model.Player;
import model.Unit;
import model.disaster.area.DisasterArea;
import model.disaster.area.DisasterAreaCalculator;

import java.util.List;

public class EarthquakeEvent extends DisasterEvent {

    private static final int UNIT_DAMAGE = 10;

    private final GameMap map;
    private final Player player;
    private final DisasterAreaCalculator areaCalculator;
    private final DisasterTargetCollector targetCollector;

    private DisasterArea affectedArea;

    public EarthquakeEvent(
            HexCoordinate origin,
            GameMap map,
            Player player,
            DisasterAreaCalculator areaCalculator,
            DisasterTargetCollector targetCollector
    ) {
        super(DisasterType.EARTHQUAKE, origin);

        if (map == null || player == null || areaCalculator == null || targetCollector == null) {
            throw new IllegalArgumentException("dependencies must not be null");
        }

        this.map = map;
        this.player = player;
        this.areaCalculator = areaCalculator;
        this.targetCollector = targetCollector;
    }

    @Override
    protected void onStarted() {
        affectedArea = areaCalculator.calculate(getOrigin(), map);

        List<Unit> affectedUnits = targetCollector.collectUnits(player, affectedArea);

        for (Unit unit : affectedUnits) {
            unit.takeDamage(UNIT_DAMAGE);
        }

        player.removeDeadUnits();
    }

    public DisasterArea getAffectedArea() {
        return affectedArea;
    }
}