package model.disaster;

import model.Building;
import model.GameMap;
import model.HexCoordinate;
import model.Player;
import model.Unit;
import model.disaster.area.DisasterArea;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class AvalancheEvent extends DisasterEvent {

    private static final int UNIT_DAMAGE = 40;
    private static final int BUILDING_DAMAGE = 50;

    private final GameMap map;
    private final Player player;
    private final Random random;
    private final DisasterPathBuilder pathBuilder;
    private final DisasterTargetCollector targetCollector;

    private DisasterArea affectedArea;

    public AvalancheEvent(
            HexCoordinate origin,
            GameMap map,
            Player player,
            Random random,
            DisasterPathBuilder pathBuilder,
            DisasterTargetCollector targetCollector
    ) {
        super(DisasterType.AVALANCHE, origin);

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
                        3,
                        random,
                        true
                );

        Set<HexCoordinate> coordinates = new HashSet<>(path);

        affectedArea = new DisasterArea(getOrigin(), coordinates);

        damageUnits();
        damageBuildings();
        blockPath(path);

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

    private void blockPath(List<HexCoordinate> path) {
        for (HexCoordinate coordinate : path) {
            map.getHex(coordinate).blockForTurns(1);
        }
    }

    public DisasterArea getAffectedArea() {
        return affectedArea;
    }
}