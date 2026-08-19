package model.disaster;

import model.GameMap;
import model.Hex;
import model.HexCoordinate;
import model.Player;
import model.Unit;
import model.military.MilitaryUnit;
import model.military.MilitaryHex;
import model.combat.CombatRequest;
import model.combat.CombatService;
import model.combat.CombatReport;

import java.util.ArrayList;
import java.util.List;

public class BearAttackEvent extends DisasterEvent {

    private static final int HUNT_RADIUS = 3;

    private final GameMap map;
    private final Player player;
    private final CombatService combatService;

    private final List<Bear> bears = new ArrayList<>();
    private CombatReport lastCombatReport;

    public BearAttackEvent(
            HexCoordinate origin,
            GameMap map,
            Player player,
            CombatService combatService
    ) {
        super(DisasterType.BEAR_ATTACK, origin);

        if (map == null || player == null || combatService == null) {
            throw new IllegalArgumentException("dependencies must not be null");
        }

        this.map = map;
        this.player = player;
        this.combatService = combatService;
    }

    @Override
    protected void onStarted() {
        spawnBears();
    }

    private void spawnBears() {
        int playerUnitsNearDen = 0;

        for (Unit unit : player.getUnits()) {
            if (unit.isAlive() && unit.getPosition().distanceTo(getOrigin()) <= HUNT_RADIUS) {
                playerUnitsNearDen++;
            }
        }

        int bearCount = playerUnitsNearDen >= 3 ? 2 : 1;

        List<HexCoordinate> spawnPositions = findSpawnPositions();

        for (int i = 0; i < bearCount && i < spawnPositions.size(); i++) {

            bears.add(new Bear(spawnPositions.get(i), getOrigin()));
        }
    }

    private List<HexCoordinate> findSpawnPositions() {
        List<HexCoordinate> positions = new ArrayList<>();

        Hex originHex = map.getHex(getOrigin());

        if (originHex != null && originHex.getTerrainType() == model.Constants.TerrainType.FOREST) {
            positions.add(getOrigin());
        }

        for (Hex neighbour : map.getNeighboursOf(getOrigin())) {

            if (neighbour.getTerrainType() == model.Constants.TerrainType.FOREST) {

                positions.add(neighbour.getCoordinate());
            }
        }

        return positions;
    }

    public void processTurn() {

        for (Bear bear : new ArrayList<>(bears)) {

            if (!bear.isAlive()) {
                bears.remove(bear);
                continue;
            }

            bear.resetAP();

            Unit target = findTarget(bear);

            if (target == null) {
                continue;
            }

            while (bear.getCurrentAP() > 0 && !bear.canAttack(target)) {

                if (!bear.moveOneStepToward(
                        target,
                        map
                )) {
                    break;
                }
            }

            if (bear.getCurrentAP() > 0
                    && bear.canAttack(target)) {
                MilitaryHex militaryDefenders = null;
                if (target instanceof MilitaryUnit) {
                    militaryDefenders = new MilitaryHex(map.getHex(target.getPosition()));
                    for (Unit unit : player.getUnits()) {
                        if (unit instanceof MilitaryUnit && unit.isAlive()
                                && unit.getPosition().equals(target.getPosition())) {
                            militaryDefenders.addUnit((MilitaryUnit) unit);
                        }
                    }
                }
                lastCombatReport = combatService.resolve(
                        CombatRequest.bearAttack(bear, target, militaryDefenders));
            }
        }

        player.removeDeadUnits();
        bears.removeIf(bear -> !bear.isAlive());
    }

    private Unit findTarget(Bear bear) {

        Unit civilian =
                findNearestTarget(
                        bear,
                        false
                );

        if (civilian != null) {
            return civilian;
        }

        return findNearestTarget(
                bear,
                true
        );
    }

    private Unit findNearestTarget(
            Bear bear,
            boolean military
    ) {
        Unit nearest = null;
        int nearestDistance =
                Integer.MAX_VALUE;

        for (Unit unit : player.getUnits()) {

            if (!unit.isAlive()) {
                continue;
            }

            boolean isMilitary =
                    unit instanceof MilitaryUnit;

            if (isMilitary != military) {
                continue;
            }

            int distance =
                    getOrigin().distanceTo(
                            unit.getPosition()
                    );

            if (distance > HUNT_RADIUS) {
                continue;
            }

            int bearDistance =
                    bear.getPosition()
                            .distanceTo(
                                    unit.getPosition()
                            );

            if (bearDistance < nearestDistance) {
                nearestDistance = bearDistance;
                nearest = unit;
            }
        }

        return nearest;
    }

    public boolean shouldEnd() {
        if (bears.isEmpty()) {
            return true;
        }

        for (Unit unit : player.getUnits()) {
            if (unit.isAlive()
                    && getOrigin()
                    .distanceTo(unit.getPosition())
                    <= HUNT_RADIUS) {
                return false;
            }
        }

        return true;
    }

    public List<Bear> getBears() {
        return new ArrayList<>(bears);
    }

    public CombatReport getLastCombatReport() { return lastCombatReport; }
}
