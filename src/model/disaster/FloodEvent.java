package model.disaster;

import model.Building;
import model.Constants;
import model.GameMap;
import model.Hex;
import model.HexCoordinate;
import model.Player;
import model.Unit;
import model.disaster.area.DisasterArea;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FloodEvent extends DisasterEvent implements AffectedAreaDisaster{

    private static final int UNIT_DAMAGE = 20;
    private static final int BUILDING_DAMAGE = 30;

    private final GameMap map;
    private final Player player;
    private final DisasterTargetCollector targetCollector;

    private DisasterArea affectedArea;

    public FloodEvent(
            HexCoordinate origin,
            GameMap map,
            Player player,
            DisasterTargetCollector targetCollector
    ) {
        super(DisasterType.FLOOD, origin);

        if (map == null || player == null || targetCollector == null) {
            throw new IllegalArgumentException("dependencies must not be null");
        }

        this.map = map;
        this.player = player;
        this.targetCollector = targetCollector;
    }

    @Override
    protected void onStarted() {
        affectedArea = calculateAffectedArea();

        damageUnits();
        damageBuildings();
        destroyRoads();

        player.removeDeadUnits();
    }

    private void destroyRoads() {
        for (HexCoordinate coordinate : affectedArea.getAffectedCoordinates()) {
            if (map.hasRoad(coordinate)) {
                map.removeRoad(coordinate);
            }
        }
    }

    private DisasterArea calculateAffectedArea() {
        Set<HexCoordinate> affectedCoordinates = new HashSet<>();

        affectedCoordinates.add(getOrigin());

        for (Hex neighbour : map.getNeighboursOf(getOrigin())) {
            if (canBeFlooded(neighbour)) {
                affectedCoordinates.add(neighbour.getCoordinate());
            }
        }

        return new DisasterArea(
                getOrigin(),
                affectedCoordinates
        );
    }

    private boolean canBeFlooded(Hex hex) {
        Constants.TerrainType terrain = hex.getTerrainType();

        return terrain != Constants.TerrainType.SEA
                && terrain != Constants.TerrainType.MOUNTAIN
                && terrain != Constants.TerrainType.MOUNTAIN_RANGE
                && terrain != Constants.TerrainType.VOLCANO;
    }

    private void damageUnits() {
        List<Unit> affectedUnits = targetCollector.collectUnits(player, affectedArea);

        for (Unit unit : affectedUnits) {
            unit.takeDamage(UNIT_DAMAGE);
            unit.setCurrentAP(0);
        }
    }

    private void damageBuildings() {
        List<Building> affectedBuildings = targetCollector.collectBuildings(player, affectedArea);

        for (Building building : affectedBuildings) {

            if (building.getType() == Constants.BuildingType.FARM) {
                building.ruin();
                continue;
            }

            if (building.getType() == Constants.BuildingType.TOWN_HALL) {
                damageTownHall(building);
                continue;
            }

            building.takeDamage(BUILDING_DAMAGE);

            if (building.isActive()) {
                building.blockProductionForTurns(1);
            }
        }
    }

    private void damageTownHall(Building townHall) {
        int currentHp = townHall.getCurrentHp();

        if (currentHp <= 1) {
            return;
        }

        int damage = Math.min(
                BUILDING_DAMAGE,
                currentHp - 1
        );

        townHall.takeDamage(damage);
    }

    public DisasterArea getAffectedArea() {
        return affectedArea;
    }
}