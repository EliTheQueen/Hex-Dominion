package controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import model.BorderExpander;
import model.Builder;
import model.Building;
import model.Constants;
import model.Explorer;
import model.GameState;
import model.Hex;
import model.HexCoordinate;
import model.PathFinder;
import model.Player;
import model.ResourceAmount;
import model.Unit;
import model.Worker;
import view.MainWindow;

import static model.Constants.BuildingType;
import static model.Constants.TechnologyType;
import static model.Constants.UnitType;
import static model.Constants.BUILD_COST;
import static model.Constants.UNIT_COST;

/** Mediates between the Swing views and the game model. */
public class GameController {
    private GameState gameState;
    private MainWindow mainWindow;

    private Unit selectedUnit;
    private HexCoordinate selectedHex;
    private BuildingType pendingBuildType;
    private boolean buildMode;
    private String statusMessage = "";

    public GameController() {}

    public void setMainWindow(MainWindow w) { this.mainWindow = w; }

    public void startNewGame() {
        gameState = new GameState(15, 13);
        selectedUnit = null;
        selectedHex = null;
        pendingBuildType = null;
        buildMode = false;
        statusMessage = "";
    }

    public GameState getGameState() { return gameState; }
    public Unit getSelectedUnit() { return selectedUnit; }
    public HexCoordinate getSelectedHex() { return selectedHex; }
    public boolean isBuildMode() { return buildMode; }
    public BuildingType getPendingBuildType() { return pendingBuildType; }
    public String getStatusMessage() { return statusMessage; }

    public void onHexClicked(HexCoordinate coord) {
        if (gameState == null || gameState.isGameOver()) return;
        Hex hex = gameState.getMap().getHex(coord);
        if (hex == null) return;

        Player player = gameState.getPlayer();

        // --- Placing a building ---
        if (buildMode && pendingBuildType != null && selectedUnit instanceof Builder) {
            placeBuilding(coord, player);
            return;
        }

        // --- A unit is selected: try to move it ---
        if (selectedUnit != null) {
            if (coord.equals(selectedUnit.getPosition())) {
                // Clicking the unit again keeps it selected.
                selectedHex = coord;
                return;
            }
            if (selectedUnit.canMoveTo(gameState.getMap(), coord)) {
                Unit existing = player.getUnitAt(coord);
                if (existing == null || existing == selectedUnit) {
                    selectedUnit.moveTo(gameState.getMap(), coord);
                    gameState.updateVisibility();
                    statusMessage = "";
                } else {
                    statusMessage = "Hex occupied by another unit";
                }
            } else {
                // Re-select a unit if one is here, otherwise deselect.
                Unit u = player.getUnitAt(coord);
                if (u != null && u.isAlive()) {
                    selectedUnit = u;
                    selectedHex = coord;
                } else {
                    selectedUnit = null;
                    selectedHex = coord;
                }
            }
            return;
        }

        // --- Nothing selected: select a unit if present ---
        Unit u = player.getUnitAt(coord);
        if (u != null && u.isAlive()) {
            selectedUnit = u;
            selectedHex = coord;
        } else {
            selectedUnit = null;
            selectedHex = coord;
        }
    }

    private void placeBuilding(HexCoordinate coord, Player player) {
        Builder builder = (Builder) selectedUnit;
        ResourceAmount cost = BUILD_COST.get(pendingBuildType);

        if (!player.canBuildAt(coord, pendingBuildType)) {
            statusMessage = "Cannot build there (need territory / tech / empty hex)";
        } else if (coord.distanceTo(builder.getPosition()) > 1) {
            statusMessage = "Builder must be adjacent to the build site";
        } else if (!builder.hasCharges()) {
            statusMessage = "Builder has no charges left";
        } else if (cost == null || !player.canAfford(cost)) {
            statusMessage = "Not enough resources";
        } else {
            player.spend(cost);
            Building b = new Building(coord, pendingBuildType);
            player.addBuilding(b);
            gameState.getMap().getHex(coord).setHasBuilding(true);
            builder.useCharge();
            statusMessage = "Built " + pendingBuildType.name();
            if (!builder.hasCharges()) {
                builder.kill();
                selectedUnit = null;
            }
            gameState.updateVisibility();
        }
        buildMode = false;
        pendingBuildType = null;
    }

    public void onEndTurnClicked() {
        if (gameState == null) return;
        gameState.endTurn();
        selectedUnit = null;
        selectedHex = null;
        buildMode = false;
        pendingBuildType = null;
        statusMessage = "";
        if (gameState.isGameOver() && mainWindow != null) {
            mainWindow.showEndGame(gameState.getFinalScore());
        }
    }

    public void onBuildChosen(BuildingType type) {
        if (selectedUnit instanceof Builder) {
            Builder b = (Builder) selectedUnit;
            if (!b.hasCharges()) {
                statusMessage = "Builder has no charges left";
                return;
            }
            ResourceAmount cost = BUILD_COST.get(type);
            if (cost != null && !gameState.getPlayer().canAfford(cost)) {
                statusMessage = "Not enough resources for " + type.name();
                return;
            }
            if ((type == BuildingType.STONE_MINE && !gameState.getPlayer().hasResearched(TechnologyType.STONE_MINING))
                || (type == BuildingType.IRON_MINE && !gameState.getPlayer().hasResearched(TechnologyType.IRON_MINING))
                || (type == BuildingType.TOWNSHIP && !gameState.getPlayer().hasResearched(TechnologyType.TOWNSHIP))) {
                statusMessage = "Requires research first";
                return;
            }
            pendingBuildType = type;
            buildMode = true;
            statusMessage = "Select an adjacent territory hex to build " + type.name();
        }
    }

    public boolean canRecruit(UnitType type) {
        if (gameState == null) return false;
        ResourceAmount cost = UNIT_COST.get(type);
        return cost != null && gameState.getPlayer().canAfford(cost) && findFreeHexNearTownHall() != null;
    }

    public void onRecruitUnit(UnitType type) {
        if (gameState == null) return;
        ResourceAmount cost = UNIT_COST.get(type);
        if (cost == null || !gameState.getPlayer().canAfford(cost)) {
            statusMessage = "Not enough resources to recruit " + type.name();
            return;
        }
        HexCoordinate pos = findFreeHexNearTownHall();
        if (pos == null) {
            statusMessage = "No free hex near Town Hall";
            return;
        }
        gameState.getPlayer().spend(cost);

        Unit unit;
        switch (type) {
            case EXPLORER:        unit = new Explorer(pos); break;
            case BUILDER:         unit = new Builder(pos); break;
            case WORKER:          unit = new Worker(pos); break;
            case BORDER_EXPANDER: unit = new BorderExpander(pos); break;
            default: return;
        }
        gameState.getPlayer().addUnit(unit);
        gameState.updateVisibility();
        statusMessage = "Recruited " + type.name();
    }

    private HexCoordinate findFreeHexNearTownHall() {
        Player player = gameState.getPlayer();
        for (Building b : player.getBuildings()) {
            if (b.getType() == BuildingType.TOWN_HALL) {
                HexCoordinate center = b.getPosition();
                if (player.getUnitAt(center) == null) return center;
                for (HexCoordinate n : center.findNeighbours()) {
                    if (gameState.getMap().containsCoordinate(n) && player.getUnitAt(n) == null) {
                        return n;
                    }
                }
            }
        }
        return null;
    }

    public void onResearchChosen(TechnologyType tech) {
        if (gameState == null) return;
        if (gameState.getPlayer().research(tech)) {
            statusMessage = "Researched " + tech.name();
        } else {
            statusMessage = "Cannot research " + tech.name();
        }
    }

    public void onAutoExploreToggled() {
        if (selectedUnit instanceof Explorer) {
            ((Explorer) selectedUnit).toggleAutoExplore();
        }
    }

    public void onExpandBorder() {
        if (selectedUnit instanceof BorderExpander) {
            BorderExpander be = (BorderExpander) selectedUnit;
            if (be.expand(gameState.getMap(), gameState.getPlayer())) {
                gameState.updateVisibility();
                statusMessage = "Border expanded";
            } else {
                statusMessage = "Not enough AP to expand";
            }
        }
    }

    public void onStationWorker() {
        if (!(selectedUnit instanceof Worker)) return;
        Worker w = (Worker) selectedUnit;
        Player player = gameState.getPlayer();
        Building b = player.getBuildingAt(w.getPosition());
        if (b == null) {
            statusMessage = "No building on this hex";
        } else if (b.getWorkerCap() == 0) {
            statusMessage = b.getType().name() + " cannot host workers";
        } else if (w.station(b)) {
            statusMessage = "Worker stationed at " + b.getType().name();
        } else {
            statusMessage = "Cannot station (full or no AP)";
        }
    }

    public void onUnstationWorker() {
        if (selectedUnit instanceof Worker) {
            ((Worker) selectedUnit).unstation();
            statusMessage = "Worker unstationed";
        }
    }

    public void deselectUnit() {
        selectedUnit = null;
        selectedHex = null;
        buildMode = false;
        pendingBuildType = null;
    }

    /** Hexes the selected unit can reach this turn (used to tint the map). */
    public List<HexCoordinate> getReachableHexes() {
        if (selectedUnit == null || !selectedUnit.canAct()) return Collections.emptyList();
        List<HexCoordinate> reachable = new ArrayList<>();
        int ap = selectedUnit.getCurrentAP();
        for (Hex h : gameState.getMap().getAllHexes()) {
            HexCoordinate coord = h.getCoordinate();
            int dist = selectedUnit.getPosition().distanceTo(coord);
            if (dist > 0 && dist <= ap) {
                List<HexCoordinate> path = PathFinder.findPath(
                        gameState.getMap(), selectedUnit.getPosition(), coord, ap);
                if (path != null) reachable.add(coord);
            }
        }
        return reachable;
    }
}
