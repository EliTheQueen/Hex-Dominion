package model.disaster;

import model.Building;
import model.GameMap;
import model.Hex;
import model.HexCoordinate;
import model.Player;
import model.Unit;
import model.disaster.area.DisasterArea;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class TornadoEvent extends DisasterEvent implements AffectedAreaDisaster {

    private static final int UNIT_DAMAGE = 30;
    private static final int BUILDING_DAMAGE = 40;

    private final GameMap map;
    private final Player player;
    private final Random random;
    private final DisasterPathBuilder pathBuilder;
    private final DisasterTargetCollector targetCollector;

    private DisasterArea affectedArea;

    public TornadoEvent(
            HexCoordinate origin,
            GameMap map,
            Player player,
            Random random,
            DisasterPathBuilder pathBuilder,
            DisasterTargetCollector targetCollector
    ) {
        super(DisasterType.TORNADO, origin);

        if (map == null
                || player == null
                || random == null
                || pathBuilder == null
                || targetCollector == null) {
            throw new IllegalArgumentException("dependencies must not be null");
        }

        this.map = map;
        this.player = player;
        this.random = random;
        this.pathBuilder = pathBuilder;
        this.targetCollector = targetCollector;
    }

    @Override
    protected void onStarted() {

        List<HexCoordinate> path =
                pathBuilder.buildRandomPath(
                        getOrigin(),
                        map,
                        4,
                        random,
                        false
                );

        Set<HexCoordinate> coordinates = new HashSet<>(path);

        affectedArea = new DisasterArea(
                getOrigin(),
                coordinates
        );

        damageUnits();
        damageBuildings();

        player.removeDeadUnits();
    }

    private void damageUnits() {

        List<Unit> units =
                targetCollector.collectUnits(
                        player,
                        affectedArea
                );

        for (Unit unit : units) {
            unit.takeDamage(UNIT_DAMAGE);

            if (unit.isAlive()) {
                throwUnitToNeighbour(unit);
            }
        }
    }

    private void throwUnitToNeighbour(Unit unit) {

        for (Hex neighbour :
                map.getNeighboursOf(unit.getPosition())) {

            if (canThrowTo(neighbour)) {
                unit.setPosition(neighbour.getCoordinate());
                return;
            }
        }
    }

    private boolean canThrowTo(Hex hex) {

        if (hex.isBlocked()) {
            return false;
        }

        switch (hex.getTerrainType()) {

            case SEA:
            case MOUNTAIN_RANGE:
                return false;

            default:
                return player.getUnitAt(
                        hex.getCoordinate()
                ) == null;
        }
    }

    private void damageBuildings() {

        List<Building> buildings =
                targetCollector.collectBuildings(
                        player,
                        affectedArea
                );

        for (Building building : buildings) {
            building.takeDamage(BUILDING_DAMAGE);
        }
    }

    public DisasterArea getAffectedArea() {
        return affectedArea;
    }
}