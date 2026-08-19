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
import model.townhall.CommandStartResult;
import model.technology.ResearchStartResult;

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
            if (selectedUnit.canMoveTo(gameState.getMap(), coord, gameState.getMovementPolicy())) {
                Unit existing = player.getUnitAt(coord);
                if (existing == null || existing == selectedUnit) {
                    selectedUnit.moveTo(gameState.getMap(), coord, gameState.getMovementPolicy());
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

        if (!gameState.canBuildAt(coord, pendingBuildType)) {
            statusMessage = "Cannot build there (need territory / tech / matching resource)";
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

    /** Whether a unit of this type can be queued: affordable and below the unit cap. */
    public boolean canRecruit(UnitType type) {
        if (gameState == null) return false;
        Player player = gameState.getPlayer();
        ResourceAmount cost = UNIT_COST.get(type);
        return cost != null && player.canAfford(cost) && !player.atUnitCap();
    }

    /** Recruiting enqueues the unit in the Town Hall production queue (not instant). */
    public void onRecruitUnit(UnitType type) {
        if (gameState == null) return;
        Player player = gameState.getPlayer();
        ResourceAmount cost = UNIT_COST.get(type);
        if (cost == null || !player.canAfford(cost)) {
            statusMessage = "Not enough resources to recruit " + type.name();
            return;
        }
        if (player.atUnitCap()) {
            statusMessage = "Unit cap reached (build a Township to raise it)";
            return;
        }
        player.spend(cost);
        player.getProductionQueue().enqueue(model.ProductionTask.forUnit(type));
        statusMessage = "Queued " + type.name() + " for production";
    }

    /** Research is queued in the Town Hall and applies when the queue completes it. */
    public void onResearchChosen(TechnologyType tech) {
        if (gameState == null) return;
        if (gameState.getPlayer().queueTech(tech)) {
            statusMessage = "Queued research: " + tech.name();
        } else {
            statusMessage = "Cannot research " + tech.name() + " (prereqs/resources)";
        }
    }

    public CommandStartResult onTownHallUpgrade() {
        CommandStartResult result = gameState.startTownHallUpgrade();
        statusMessage = "Town Hall upgrade: " + result.name().replace('_', ' ');
        return result;
    }

    public ResearchStartResult onPhaseTwoResearch(model.technology.TechnologyType tech) {
        ResearchStartResult result = gameState.startPhaseTwoResearch(tech);
        statusMessage = "Research " + tech.name() + ": " + result.name().replace('_', ' ');
        return result;
    }

    public boolean onCancelTownHallCommand() {
        boolean cancelled = gameState.cancelTownHallCommand();
        statusMessage = cancelled ? "Town Hall command cancelled (no refund)" : "No active command";
        return cancelled;
    }

    /** Whether the selected builder could place this building on some adjacent valid hex now. */
    public boolean canBuildType(BuildingType type) {
        if (gameState == null || !(selectedUnit instanceof Builder)) return false;
        Builder b = (Builder) selectedUnit;
        if (!b.hasCharges()) return false;
        ResourceAmount cost = BUILD_COST.get(type);
        if (cost == null || !gameState.getPlayer().canAfford(cost)) return false;
        if (gameState.canBuildAt(b.getPosition(), type)) return true;
        for (HexCoordinate n : b.getPosition().findNeighbours()) {
            if (gameState.canBuildAt(n, type)) return true;
        }
        return false;
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
                // The expander is consumed (removed from the map) after claiming territory.
                be.kill();
                gameState.getPlayer().removeUnit(be);
                selectedUnit = null;
                selectedHex = null;
                gameState.updateVisibility();
                statusMessage = "Border expanded — expander consumed";
            } else {
                statusMessage = "Cannot expand (need AP and an explored hex)";
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

    /** Whether the selected worker is standing on a building with a free worker slot. */
    public boolean canStationHere() {
        if (gameState == null || !(selectedUnit instanceof Worker)) return false;
        Worker w = (Worker) selectedUnit;
        if (w.getCurrentAP() < Constants.WORKER_STATION_AP_COST) return false;
        Building b = gameState.getPlayer().getBuildingAt(w.getPosition());
        return b != null && b.isActive() && b.getWorkerCap() > 0
                && (b.getWorkerCount() < b.getWorkerCap() || (w.isStationed() && w.getStationedAt() == b));
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

    /** Hexes the selected unit can reach this turn (used to tint the map green). */
    public List<HexCoordinate> getReachableHexes() {
        if (selectedUnit == null || !selectedUnit.canAct()) return Collections.emptyList();
        return new ArrayList<>(PathFinder.reachable(
                gameState.getMap(), selectedUnit.getPosition(), selectedUnit.getCurrentAP(),
                gameState.getMovementPolicy()));
    }
}
