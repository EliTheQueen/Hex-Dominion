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

public class VolcanicEruptionEvent extends DisasterEvent implements AffectedAreaDisaster {

    private static final int LAVA_DAMAGE = 60;
    private static final int NEIGHBOUR_DAMAGE = 30;

    private final GameMap map;
    private final Player player;
    private final Random random;

    private final DisasterPathBuilder pathBuilder;
    private final DisasterTargetCollector targetCollector;

    private DisasterArea affectedArea;

    public VolcanicEruptionEvent(
            HexCoordinate origin,
            GameMap map,
            Player player,
            Random random,
            DisasterPathBuilder pathBuilder,
            DisasterTargetCollector targetCollector
    ) {
        super(DisasterType.VOLCANIC_ERUPTION, origin);

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

        Set<HexCoordinate> coordinates = new HashSet<>();

        coordinates.add(getOrigin());

        for (model.Hex hex : map.getNeighboursOf(getOrigin())) {
            coordinates.add(hex.getCoordinate());
        }

        List<HexCoordinate> lavaPath =
                pathBuilder.buildRandomPath(
                        getOrigin(),
                        map,
                        4,
                        random,
                        false
                );

        coordinates.addAll(lavaPath);

        affectedArea = new DisasterArea(
                getOrigin(),
                coordinates
        );

        destroyOriginUnits();
        damageLavaUnits(lavaPath);
        damageNeighbourUnits(lavaPath);

        destroyLavaBuildings(lavaPath);
        blockLavaPath(lavaPath);

        player.removeDeadUnits();
    }

    private void destroyOriginUnits() {

        for (Unit unit :
                targetCollector.collectUnits(
                        player,
                        affectedArea
                )) {

            if (unit.getPosition().equals(getOrigin())) {
                unit.kill();
            }
        }
    }

    private void damageLavaUnits(List<HexCoordinate> lavaPath) {

        for (Unit unit : player.getUnits()) {

            if (!unit.isAlive()) {
                continue;
            }

            if (lavaPath.contains(unit.getPosition()) && !unit.getPosition().equals(getOrigin())) {

                unit.takeDamage(LAVA_DAMAGE);
            }
        }
    }

    private void damageNeighbourUnits(List<HexCoordinate> lavaPath) {

        for (Unit unit : player.getUnits()) {

            if (!unit.isAlive()) {
                continue;
            }

            if (!affectedArea.contains(unit.getPosition())) {
                continue;
            }

            if (lavaPath.contains(unit.getPosition())) {
                continue;
            }

            unit.takeDamage(NEIGHBOUR_DAMAGE);
        }
    }

    private void destroyLavaBuildings(List<HexCoordinate> lavaPath) {

        for (Building building : player.getBuildings()) {

            if (lavaPath.contains(building.getPosition())) {
                building.ruin();
            }
        }
    }

    private void blockLavaPath(
            List<HexCoordinate> lavaPath
    ) {

        for (HexCoordinate coordinate : lavaPath) {

            map.getHex(coordinate).blockForTurns(3);
        }
    }

    public DisasterArea getAffectedArea() {
        return affectedArea;
    }
}