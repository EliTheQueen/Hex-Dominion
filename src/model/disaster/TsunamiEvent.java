package model.disaster;

import model.Building;
import model.GameMap;
import model.Hex;
import model.HexCoordinate;
import model.Player;
import model.Unit;
import model.disaster.area.DisasterArea;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class TsunamiEvent extends DisasterEvent {

    private static final int UNIT_DAMAGE = 30;

    private final GameMap map;
    private final Player player;
    private final DisasterTargetCollector targetCollector;
    private final Random random;

    private DisasterArea affectedArea;

    public TsunamiEvent(
            HexCoordinate origin,
            GameMap map,
            Player player,
            DisasterTargetCollector targetCollector,
            Random random
    ) {
        super(DisasterType.TSUNAMI, origin);

        if (map == null
                || player == null
                || targetCollector == null
                || random == null) {
            throw new IllegalArgumentException("dependencies must not be null");
        }

        this.map = map;
        this.player = player;
        this.targetCollector = targetCollector;
        this.random = random;
    }

    @Override
    protected void onStarted() {
        affectedArea = calculateArea();

        damageUnits();
        destroyBuildings();

        player.removeDeadUnits();
    }

    private DisasterArea calculateArea() {
        Set<HexCoordinate> coordinates = new HashSet<>();

        coordinates.add(getOrigin());

        List<HexCoordinate> coastalNeighbours = new ArrayList<>();

        for (Hex neighbour : map.getNeighboursOf(getOrigin())) {
            HexCoordinate coordinate = neighbour.getCoordinate();

            if (map.isCoastal(coordinate)) {
                coastalNeighbours.add(coordinate);
            }
        }

        while (!coastalNeighbours.isEmpty() && coordinates.size() < 3) {

            int index = random.nextInt(coastalNeighbours.size());

            coordinates.add(coastalNeighbours.remove(index));
        }

        return new DisasterArea(getOrigin(), coordinates);
    }

    private void damageUnits() {
        List<Unit> units = targetCollector.collectUnits(player, affectedArea);

        for (Unit unit : units) {
            unit.takeDamage(UNIT_DAMAGE);
        }
    }

    private void destroyBuildings() {
        List<Building> buildings = targetCollector.collectBuildings(player, affectedArea);

        for (Building building : buildings) {
            building.ruin();
        }
    }

    public DisasterArea getAffectedArea() {
        return affectedArea;
    }
}