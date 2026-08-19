package model.disaster;

import model.Building;
import model.Player;
import model.Unit;
import model.disaster.area.DisasterArea;

import java.util.ArrayList;
import java.util.List;

public class DisasterTargetCollector implements java.io.Serializable {

    public List<Unit> collectUnits(Player player, DisasterArea area) {
        if (player == null || area == null) {
            throw new IllegalArgumentException("arguments must not be null");
        }

        List<Unit> affectedUnits = new ArrayList<>();

        for (Unit unit : player.getUnits()) {
            if (unit.isAlive() && area.contains(unit.getPosition())) {
                affectedUnits.add(unit);
            }
        }

        return affectedUnits;
    }

    public List<Building> collectBuildings(Player player, DisasterArea area) {
        if (player == null || area == null) {
            throw new IllegalArgumentException("arguments must not be null");
        }

        List<Building> affectedBuildings = new ArrayList<>();

        for (Building building : player.getBuildings()) {
            if (building.isActive() && area.contains(building.getPosition())) {
                affectedBuildings.add(building);
            }
        }

        return affectedBuildings;
    }
}
