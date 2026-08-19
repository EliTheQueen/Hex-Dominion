package model.disaster;

import model.Building;
import model.Constants;
import model.GameMap;
import model.HexCoordinate;
import model.Player;
import model.Unit;
import model.disaster.area.DisasterArea;
import model.disaster.area.DisasterAreaCalculator;

import java.util.List;

public class SeaStormEvent extends DisasterEvent implements AffectedAreaDisaster {

    private static final int COASTAL_UNIT_DAMAGE = 20;
    private static final int DOCK_DAMAGE = 30;

    private final GameMap map;
    private final Player player;

    private final DisasterAreaCalculator areaCalculator;
    private final DisasterTargetCollector targetCollector;

    private DisasterArea affectedArea;

    public SeaStormEvent(
            HexCoordinate origin,
            GameMap map,
            Player player,
            DisasterAreaCalculator areaCalculator,
            DisasterTargetCollector targetCollector
    ) {
        super(DisasterType.SEA_STORM, origin);

        if (map == null
                || player == null
                || areaCalculator == null
                || targetCollector == null) {
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

        damageCoastalUnits();
        damageDocks();

        player.removeDeadUnits();
    }

    private void damageCoastalUnits() {
        List<Unit> units = targetCollector.collectUnits(player, affectedArea);

        for (Unit unit : units) {
            if (map.isCoastal(unit.getPosition())) {
                unit.takeDamage(COASTAL_UNIT_DAMAGE);

                unit.setCurrentAP(0);
            }
        }
    }

    private void damageDocks() {
        List<Building> buildings = targetCollector.collectBuildings(player, affectedArea);

        for (Building building : buildings) {

            if (building.getType() == Constants.BuildingType.DOCK) {

                building.takeDamage(DOCK_DAMAGE);
                if (!building.isActive()) player.destroyBuilding(map, building);
            }
        }
    }

    public DisasterArea getAffectedArea() {
        return affectedArea;
    }
}
