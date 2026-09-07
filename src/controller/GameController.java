package controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.nio.file.Paths;

import model.BorderExpander;
import model.Builder;
import model.Building;
import model.ActionAvailability;
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
import client.NetworkEventListener;
import client.NetworkManager;

import static model.Constants.BuildingType;
import static model.Constants.TechnologyType;
import static model.Constants.UnitType;
import static model.Constants.UNIT_COST;
import model.townhall.CommandStartResult;
import model.technology.ResearchStartResult;
import model.save.*;
import model.tribe.*;
import model.tribe.mission.MissionActionResult;
import model.combat.CombatReport;
import model.military.MilitaryUnit;
import model.disaster.Bear;
import model.military.MilitaryUnitType;
import model.townhall.TownHallLevel;

/** Mediates between the Swing views and the game model. */
public class GameController {
    private GameState gameState;
    private MainWindow mainWindow;

    private Unit selectedUnit;
    private HexCoordinate selectedHex;
    private BuildingType pendingBuildType;
    private boolean buildMode;
    private String statusMessage = "";
    private Tribe lastOpenedTribe;
    private final SaveManager saveManager;
    private String savedStateFingerprint;
    private final NetworkManager networkManager;
    private boolean remoteController;

    public GameController() {
        this(new SaveManager(Paths.get(
                System.getProperty("hex.dominion.saveDir", "saves"))));
    }

    public GameController(SaveManager saveManager) {
        if (saveManager == null) throw new IllegalArgumentException("save manager is required");
        this.saveManager = saveManager;
        this.networkManager = null;
    }

    /** Creates a controller whose scoped core actions are executed by the server. */
    public GameController(NetworkManager networkManager) {
        if (networkManager == null) throw new IllegalArgumentException("network manager is required");
        this.saveManager = new SaveManager(Paths.get(
                System.getProperty("hex.dominion.saveDir", "saves")));
        this.networkManager = networkManager;
        networkManager.addListener(new NetworkEventListener() {
            @Override public void onHello(int clientId, boolean controller) {
                remoteController = controller;
                statusMessage = controller
                        ? "Connected as controller" : "Connected as read-only observer";
                refreshRemoteView();
            }

            @Override public void onStateUpdate(GameState state, long revision) {
                gameState = state;
                resetSessionSelection();
                statusMessage = "Synchronized with server (revision " + revision + ")";
                refreshRemoteView();
            }

            @Override public void onError(String code, String message) {
                statusMessage = message;
                refreshRemoteView();
            }

            @Override public void onDisconnected(String reason) {
                statusMessage = reason;
                refreshRemoteView();
            }
        });
    }

    public void setMainWindow(MainWindow w) { this.mainWindow = w; }

    public void startNewGame() {
        if (isRemote()) {
            networkManager.startGame(15, 13, null);
            statusMessage = "Waiting for server to start the game...";
            return;
        }
        gameState = new GameState(15, 13);
        resetSessionSelection();
    }

    /** Starts a reproducible game for tests and deterministic evaluation scenarios. */
    public void startNewGame(long seed) {
        if (isRemote()) {
            networkManager.startGame(15, 13, seed);
            statusMessage = "Waiting for server to start the game...";
            return;
        }
        gameState = new GameState(15, 13, seed);
        resetSessionSelection();
    }

    private void resetSessionSelection() {
        selectedUnit = null;
        selectedHex = null;
        pendingBuildType = null;
        buildMode = false;
        statusMessage = "";
        savedStateFingerprint = null;
    }

    public GameState getGameState() { return gameState; }
    public Unit getSelectedUnit() { return selectedUnit; }
    public HexCoordinate getSelectedHex() { return selectedHex; }
    public boolean isBuildMode() { return buildMode; }
    public BuildingType getPendingBuildType() { return pendingBuildType; }
    public String getStatusMessage() { return statusMessage; }
    public boolean isRemote() { return networkManager != null; }
    public boolean isRemoteController() { return !isRemote() || remoteController; }

    public void onHexClicked(HexCoordinate coord) {
        if (gameState == null || gameState.isGameOver()) return;
        Hex hex = gameState.getMap().getHex(coord);
        if (hex == null) return;

        Player player = gameState.getPlayer();

        Tribe visibleTribe = gameState.getVisibleTribeAt(coord);
        if (visibleTribe != null) {
            selectedHex = coord;
            lastOpenedTribe = visibleTribe;
            if (mainWindow != null) mainWindow.showTribePanel(visibleTribe);
            return;
        }

        if (selectedUnit instanceof MilitaryUnit) {
            Bear bear = gameState.getBearAt(coord);
            if (bear != null) {
                CombatReport report = gameState.attackBear((MilitaryUnit) selectedUnit, bear);
                if (report != null) {
                    statusMessage = report.getCasualty();
                    presentCombatReport(report);
                    return;
                }
            }
        }

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
            ActionAvailability movement = getMovementAvailability(coord);
            if (movement.isEnabled()) {
                Unit existing = player.getUnitAt(coord);
                if (existing == null || existing == selectedUnit) {
                    if (isRemote()) {
                        networkManager.moveUnit(indexOfUnit(selectedUnit), coord);
                        statusMessage = "Movement request sent to server...";
                    } else {
                        selectedUnit.moveTo(gameState.getMap(), coord, gameState.getMovementPolicy());
                        gameState.updateVisibility();
                        statusMessage = "";
                    }
                }
            } else {
                // A click on another unit selects it; otherwise retain the unit and explain the rule.
                Unit u = player.getUnitAt(coord);
                if (u != null && u.isAlive()) {
                    selectedUnit = u;
                    selectedHex = coord;
                } else {
                    selectedHex = coord;
                    statusMessage = movement.getReason();
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
        ResourceAmount cost = gameState.getBuildCost(pendingBuildType);

        ActionAvailability availability = getBuildAtAvailability(coord, pendingBuildType);
        if (!availability.isEnabled()) {
            statusMessage = availability.getReason();
        } else if (isRemote()) {
            networkManager.build(indexOfUnit(builder), pendingBuildType, coord);
            statusMessage = "Build request sent to server...";
        } else {
            player.spend(cost);
            Building b = new Building(coord, pendingBuildType);
            player.addBuilding(b);
            gameState.getMap().getHex(coord).setHasBuilding(true);
            gameState.onBuildingConstructed(b);
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
        if (isRemote()) {
            if (!remoteController) {
                statusMessage = "Observer clients cannot end the turn";
                return;
            }
            networkManager.endTurn();
            selectedUnit = null;
            selectedHex = null;
            buildMode = false;
            pendingBuildType = null;
            statusMessage = "End-turn request sent to server...";
            return;
        }
        gameState.endTurn();
        selectedUnit = null;
        selectedHex = null;
        buildMode = false;
        pendingBuildType = null;
        statusMessage = "";
        if (!gameState.isGameOver()) {
            SaveWriteResult autosave = saveManager.saveWithResult(
                    SaveSlot.AUTOSAVE, "Autosave", gameState);
            if (autosave.isSuccessful()) {
                savedStateFingerprint = saveManager.fingerprint(gameState);
            } else {
                statusMessage = "Autosave failed: " + autosave.getMessage();
            }
        }
        if (gameState.isGameOver() && mainWindow != null) {
            mainWindow.showEndGame(gameState.getFinalScore());
        }
    }

    public void onBuildChosen(BuildingType type) {
        ActionAvailability availability = getBuildTypeAvailability(type);
        if (!availability.isEnabled()) {
            statusMessage = availability.getReason();
            return;
        }
        pendingBuildType = type;
        buildMode = true;
        statusMessage = availability.getReason();
    }

    /** Whether a unit of this type can be queued: affordable and below the unit cap. */
    public boolean canRecruit(UnitType type) {
        return getCivilianRecruitmentAvailability(type).isEnabled();
    }

    public ActionAvailability getMovementAvailability(HexCoordinate destination) {
        if (gameState == null) return ActionAvailability.disabled("Start or load a game first");
        if (isRemote() && !remoteController) {
            return ActionAvailability.disabled("Observer clients cannot move units");
        }
        if (selectedUnit == null) return ActionAvailability.disabled("Select a unit to see movement cost");
        if (!selectedUnit.isAlive()) return ActionAvailability.disabled("This unit is no longer alive");
        if (destination == null || !gameState.getMap().containsCoordinate(destination)) {
            return ActionAvailability.disabled("Destination is outside the map");
        }
        if (destination.equals(selectedUnit.getPosition())) {
            return ActionAvailability.disabled("This is the unit's current position");
        }
        Hex hex = gameState.getMap().getHex(destination);
        if (hex.isBlocked()) {
            return ActionAvailability.disabled("Destination is blocked by a disaster for "
                    + hex.getBlockedTurns() + " more turn(s)");
        }
        if (hex.getTerrainType() == Constants.TerrainType.MOUNTAIN_RANGE) {
            return ActionAvailability.disabled("Mountain Range is impassable");
        }
        if (hex.getTerrainType() == Constants.TerrainType.SEA && !gameState.hasSailing()) {
            return ActionAvailability.disabled("Research SAILING before entering sea");
        }
        Unit occupant = gameState.getPlayer().getUnitAt(destination);
        if (occupant != null && occupant != selectedUnit) {
            return ActionAvailability.disabled("Destination is occupied by "
                    + occupant.getUnitType().name().replace('_', ' '));
        }
        if (selectedUnit.getCurrentAP() <= 0) {
            return ActionAvailability.disabled("This unit has no action points remaining");
        }
        int unrestrictedCost = PathFinder.pathCost(gameState.getMap(), selectedUnit.getPosition(),
                destination, Integer.MAX_VALUE / 4, gameState.getMovementPolicy());
        if (unrestrictedCost < 0) {
            return ActionAvailability.disabled("No traversable path reaches this hex");
        }
        if (unrestrictedCost > selectedUnit.getCurrentAP()) {
            return ActionAvailability.disabled("Movement costs " + unrestrictedCost + " AP; only "
                    + selectedUnit.getCurrentAP() + " AP remains");
        }
        String suffix = hex.getTerrainType() == Constants.TerrainType.SEA
                ? "; embarking ends movement" : "";
        return ActionAvailability.enabled("Move here for " + unrestrictedCost + " AP" + suffix);
    }

    public ActionAvailability getBuildTypeAvailability(BuildingType type) {
        if (gameState == null) return ActionAvailability.disabled("Start or load a game first");
        if (isRemote() && !remoteController) {
            return ActionAvailability.disabled("Observer clients cannot construct buildings");
        }
        if (type == null || type == BuildingType.TOWN_HALL) {
            return ActionAvailability.disabled("Choose a constructible building type");
        }
        if (!(selectedUnit instanceof Builder)) return ActionAvailability.disabled("Select a Builder first");
        Builder builder = (Builder) selectedUnit;
        if (!builder.isAlive()) return ActionAvailability.disabled("This Builder is no longer alive");
        if (!builder.hasCharges()) return ActionAvailability.disabled("Builder has no construction charges left");
        if (type == BuildingType.STONE_MINE
                && !gameState.getPlayer().hasResearchedLegacyTechnology(Constants.TechnologyType.STONE_MINING)) {
            return ActionAvailability.disabled("Research STONE MINING before building a Stone Mine");
        }
        if (type == BuildingType.IRON_MINE
                && !gameState.getPlayer().hasResearchedLegacyTechnology(Constants.TechnologyType.IRON_MINING)) {
            return ActionAvailability.disabled("Research IRON MINING before building an Iron Mine");
        }
        if (type == BuildingType.TOWNSHIP
                && !gameState.getPlayer().hasResearchedLegacyTechnology(Constants.TechnologyType.TOWNSHIP)) {
            return ActionAvailability.disabled("Research TOWNSHIP before building a Township");
        }
        if ((type == BuildingType.DOCK || type == BuildingType.BAZAAR)
                && gameState.getTownHall().getLevel().getLevelNumber() < 2) {
            return ActionAvailability.disabled(type.name().replace('_', ' ')
                    + " requires a Settlement Town Hall");
        }
        if (type == BuildingType.BAZAAR && gameState.getPlayer().hasBuildingType(BuildingType.BAZAAR)) {
            return ActionAvailability.disabled("Only one active Bazaar is allowed");
        }
        ResourceAmount cost = gameState.getBuildCost(type);
        if (cost == null) return ActionAvailability.disabled("No construction cost is defined for " + type.name());
        String shortage = resourceShortage(cost);
        if (shortage != null) return ActionAvailability.disabled(shortage);
        ActionAvailability nearestReason = null;
        List<HexCoordinate> sites = new ArrayList<>();
        sites.add(builder.getPosition());
        sites.addAll(builder.getPosition().findNeighbours());
        for (HexCoordinate site : sites) {
            ActionAvailability siteAvailability = gameState.getBuildSiteAvailability(site, type);
            if (siteAvailability.isEnabled()) {
                return ActionAvailability.enabled("Choose a valid adjacent site; cost " + describe(cost));
            }
            if (nearestReason == null) nearestReason = siteAvailability;
        }
        return ActionAvailability.disabled("No valid site within 1 hex for "
                + type.name().replace('_', ' ') + (nearestReason == null ? "" : ": " + nearestReason.getReason()));
    }

    public ActionAvailability getBuildAtAvailability(HexCoordinate coordinate, BuildingType type) {
        ActionAvailability base = getBuildTypeAvailability(type);
        if (!base.isEnabled()) return base;
        Builder builder = (Builder) selectedUnit;
        if (coordinate == null || builder.getPosition().distanceTo(coordinate) > 1) {
            return ActionAvailability.disabled("Builder must be on or adjacent to the build site");
        }
        ActionAvailability site = gameState.getBuildSiteAvailability(coordinate, type);
        if (!site.isEnabled()) return site;
        return ActionAvailability.enabled("Build " + type.name().replace('_', ' ') + " here for "
                + describe(gameState.getBuildCost(type)));
    }

    public ActionAvailability getCivilianRecruitmentAvailability(UnitType type) {
        if (gameState == null) return ActionAvailability.disabled("Start or load a game first");
        ResourceAmount cost = UNIT_COST.get(type);
        if (cost == null) return ActionAvailability.disabled("This unit cannot be trained at the Town Hall");
        if (gameState.getTownHall().getCommandSlot().isBusy()) {
            return ActionAvailability.disabled("Town Hall command slot is occupied");
        }
        Player player = gameState.getPlayer();
        if (player.atUnitCap()) {
            return ActionAvailability.disabled("Unit cap reached (" + player.getUnitCount() + "/"
                    + player.getUnitCap() + "); build a Township to raise it");
        }
        String shortage = resourceShortage(cost);
        if (shortage != null) return ActionAvailability.disabled(shortage);
        return ActionAvailability.enabled("Train " + type.name().replace('_', ' ') + " for "
                + describe(cost) + " using the Town Hall command slot");
    }

    public ActionAvailability getMilitaryRecruitmentAvailability(MilitaryUnitType type) {
        if (gameState == null) return ActionAvailability.disabled("Start or load a game first");
        if (type == null) return ActionAvailability.disabled("Choose a military unit type");
        if (gameState.getTownHall().getCommandSlot().isBusy()) {
            return ActionAvailability.disabled("Town Hall command slot is occupied");
        }
        TownHallLevel level = gameState.getTownHall().getLevel();
        if (type == MilitaryUnitType.ARCHER && level.getLevelNumber() < 2) {
            return ActionAvailability.disabled("Archer requires a Settlement Town Hall");
        }
        if (type == MilitaryUnitType.CAVALRY && level.getLevelNumber() < 2) {
            return ActionAvailability.disabled("Cavalry requires a Settlement Town Hall and an active Stable");
        }
        if (type == MilitaryUnitType.CAVALRY
                && !gameState.getPlayer().hasBuildingType(BuildingType.STABLE)) {
            return ActionAvailability.disabled("Cavalry requires an active Stable");
        }
        if (type == MilitaryUnitType.CATAPULT && level.getLevelNumber() < 3) {
            return ActionAvailability.disabled("Catapult requires a Capital Town Hall");
        }
        if (gameState.getMilitaryUnitCount() >= gameState.getMilitaryUnitCap(level)) {
            return ActionAvailability.disabled("Military cap reached (" + gameState.getMilitaryUnitCount()
                    + "/" + gameState.getMilitaryUnitCap(level) + ")");
        }
        String shortage = resourceShortage(type.getCost());
        if (shortage != null) return ActionAvailability.disabled(shortage);
        if (!gameState.canRecruitMilitaryUnit(type, gameState.getTownHallPos(), level)) {
            return ActionAvailability.disabled("No compatible military stack space at the Town Hall");
        }
        return ActionAvailability.enabled("Train " + type.name() + " for " + describe(type.getCost())
                + " using the Town Hall command slot");
    }

    public ActionAvailability getLegacyResearchAvailability(Constants.TechnologyType technology) {
        if (gameState == null) return ActionAvailability.disabled("Start or load a game first");
        if (technology == null) return ActionAvailability.disabled("Choose a technology");
        Player player = gameState.getPlayer();
        if (player.hasResearchedLegacyTechnology(technology)) return ActionAvailability.disabled("Legacy technology already researched");
        if (player.isLegacyTechnologyQueued(technology)) return ActionAvailability.disabled("Legacy technology is already in progress");
        String prerequisite = switch (technology) {
            case STORAGE_II -> player.hasResearchedLegacyTechnology(Constants.TechnologyType.STORAGE_I)
                    ? null : "Research STORAGE I first";
            case IRON_MINING -> player.hasResearchedLegacyTechnology(Constants.TechnologyType.STONE_MINING)
                    ? null : "Research STONE MINING first";
            case PROFESSIONAL_TOOLS -> player.hasResearchedLegacyTechnology(Constants.TechnologyType.IRON_MINING)
                    ? null : "Research IRON MINING first";
            default -> null;
        };
        if (prerequisite != null) return ActionAvailability.disabled(prerequisite);
        if (gameState.getTownHall().getCommandSlot().isBusy()) {
            return ActionAvailability.disabled("Town Hall command slot is occupied");
        }
        ResourceAmount cost = player.getLegacyTechnologyCost(technology);
        String shortage = resourceShortage(cost);
        if (shortage != null) return ActionAvailability.disabled(shortage);
        return ActionAvailability.enabled("Research for " + describe(cost)
                + " using the Town Hall command slot");
    }

    public ActionAvailability getPhaseTwoResearchAvailability(model.technology.TechnologyType technology) {
        if (gameState == null) return ActionAvailability.disabled("Start or load a game first");
        if (technology == null) return ActionAvailability.disabled("Choose a technology");
        if (gameState.getPhaseTwoTechnologies().has(technology)) {
            return ActionAvailability.disabled("Technology already researched");
        }
        if (gameState.getPhaseTwoTechnologies().isQueued(technology)) {
            return ActionAvailability.disabled("Technology is already in progress");
        }
        if (!technology.isUnlockedAt(gameState.getTownHall().getLevel())) {
            return ActionAvailability.disabled("Requires "
                    + technology.getRequiredLevel().name().replace('_', ' ') + " Town Hall");
        }
        if (gameState.getTownHall().getCommandSlot().isBusy()) {
            return ActionAvailability.disabled("Town Hall command slot is occupied");
        }
        String shortage = resourceShortage(technology.getCost());
        if (shortage != null) return ActionAvailability.disabled(shortage);
        return ActionAvailability.enabled("Research for " + describe(technology.getCost())
                + " using the Town Hall command slot");
    }

    public ActionAvailability getTownHallUpgradeAvailability() {
        if (gameState == null) return ActionAvailability.disabled("Start or load a game first");
        TownHallLevel target = gameState.getTownHall().getLevel().next();
        if (target == null) return ActionAvailability.disabled("Town Hall is already at maximum level");
        if (gameState.getTownHall().getCommandSlot().isBusy()) {
            return ActionAvailability.disabled("Town Hall command slot is occupied");
        }
        ResourceAmount cost = target == TownHallLevel.SETTLEMENT
                ? ResourceAmount.of(0, 50, 50, 0) : ResourceAmount.of(0, 0, 100, 50);
        String shortage = resourceShortage(cost);
        if (shortage != null) return ActionAvailability.disabled(shortage);
        return ActionAvailability.enabled("Upgrade to " + target.name().replace('_', ' ') + " for " + describe(cost));
    }

    private String resourceShortage(ResourceAmount cost) {
        if (gameState == null || cost == null) return "Required resources are unavailable";
        List<String> missing = new ArrayList<>();
        for (Constants.ResourceType type : Constants.ResourceType.values()) {
            int shortage = cost.get(type) - gameState.getPlayer().getResources().get(type);
            if (shortage > 0) missing.add(shortage + " more " + type.name());
        }
        return missing.isEmpty() ? null : "Need " + String.join(", ", missing);
    }

    private String describe(ResourceAmount amount) {
        if (amount == null) return "unknown resources";
        List<String> parts = new ArrayList<>();
        for (Constants.ResourceType type : Constants.ResourceType.values()) {
            if (amount.get(type) > 0) parts.add(amount.get(type) + " " + type.name());
        }
        return parts.isEmpty() ? "no resources" : String.join(", ", parts);
    }

    /** Recruiting enqueues the unit in the Town Hall production queue (not instant). */
    public void onRecruitUnit(UnitType type) {
        if (gameState == null) return;
        ActionAvailability availability = getCivilianRecruitmentAvailability(type);
        if (!availability.isEnabled()) {
            statusMessage = availability.getReason();
            return;
        }
        CommandStartResult result = gameState.startCivilianTraining(type);
        statusMessage = result == CommandStartResult.STARTED ? "Training " + type.name()
                : "Cannot train " + type.name() + ": " + result.name().replace('_', ' ');
    }

    public CommandStartResult onRecruitMilitary(model.military.MilitaryUnitType type) {
        ActionAvailability availability = getMilitaryRecruitmentAvailability(type);
        if (!availability.isEnabled()) {
            statusMessage = availability.getReason();
            return CommandStartResult.INVALID_COMMAND;
        }
        CommandStartResult result = gameState.startMilitaryTraining(type);
        statusMessage = result == CommandStartResult.STARTED ? "Training " + type.name()
                : "Cannot train " + type.name() + ": " + result.name().replace('_', ' ');
        return result;
    }

    /** Research is queued in the Town Hall and applies when the queue completes it. */
    public void onResearchChosen(TechnologyType tech) {
        if (gameState == null) return;
        ActionAvailability availability = getLegacyResearchAvailability(tech);
        if (!availability.isEnabled()) {
            statusMessage = availability.getReason();
            return;
        }
        CommandStartResult result = gameState.startLegacyResearch(tech);
        statusMessage = result == CommandStartResult.STARTED ? "Researching " + tech.name()
                : "Cannot research " + tech.name() + ": " + result.name().replace('_', ' ');
    }

    public CommandStartResult onTownHallUpgrade() {
        ActionAvailability availability = getTownHallUpgradeAvailability();
        if (!availability.isEnabled()) {
            statusMessage = availability.getReason();
            return CommandStartResult.INVALID_COMMAND;
        }
        CommandStartResult result = gameState.startTownHallUpgrade();
        statusMessage = "Town Hall upgrade: " + result.name().replace('_', ' ');
        return result;
    }

    public ResearchStartResult onPhaseTwoResearch(model.technology.TechnologyType tech) {
        ActionAvailability availability = getPhaseTwoResearchAvailability(tech);
        if (!availability.isEnabled()) {
            statusMessage = availability.getReason();
            return ResearchStartResult.INVALID_REQUEST;
        }
        ResearchStartResult result = gameState.startPhaseTwoResearch(tech);
        statusMessage = "Research " + tech.name() + ": " + result.name().replace('_', ' ');
        return result;
    }

    public boolean onCancelTownHallCommand() {
        boolean cancelled = gameState.cancelTownHallCommand();
        statusMessage = cancelled ? "Town Hall command cancelled (no refund)" : "No active command";
        return cancelled;
    }

    public boolean onBuildRoad() {
        if (!(selectedUnit instanceof Builder)) return false;
        ActionAvailability availability = getRoadAvailability(false);
        if (!availability.isEnabled()) { statusMessage = availability.getReason(); return false; }
        boolean ok = gameState.getInfrastructureService().buildRoad((Builder) selectedUnit, selectedUnit.getPosition());
        statusMessage = ok ? "Road built" : "Road build failed because game state changed";
        return ok;
    }

    public boolean onDemolishRoad() {
        if (!(selectedUnit instanceof Builder)) return false;
        ActionAvailability availability = getRoadAvailability(true);
        if (!availability.isEnabled()) { statusMessage = availability.getReason(); return false; }
        boolean ok = gameState.getInfrastructureService().demolishRoad((Builder) selectedUnit, selectedUnit.getPosition());
        statusMessage = ok ? "Road demolished" : "No removable road here";
        return ok;
    }

    public boolean onBuildWall(HexCoordinate neighbour) {
        if (!(selectedUnit instanceof Builder)) return false;
        ActionAvailability availability = getWallAvailability(neighbour, false);
        if (!availability.isEnabled()) { statusMessage = availability.getReason(); return false; }
        boolean ok = gameState.getInfrastructureService().buildWall((Builder) selectedUnit,
                selectedUnit.getPosition(), neighbour);
        statusMessage = ok ? "Wall built" : "Cannot build wall on that edge";
        return ok;
    }

    public boolean onDemolishWall(HexCoordinate neighbour) {
        if (!(selectedUnit instanceof Builder)) return false;
        ActionAvailability availability = getWallAvailability(neighbour, true);
        if (!availability.isEnabled()) { statusMessage = availability.getReason(); return false; }
        boolean ok = gameState.getInfrastructureService().demolishWall((Builder) selectedUnit,
                selectedUnit.getPosition(), neighbour);
        statusMessage = ok ? "Wall demolished (no refund)" : "No removable wall on that edge";
        return ok;
    }

    public boolean onBuildBridge(HexCoordinate neighbour) {
        if (!(selectedUnit instanceof Builder)) return false;
        ActionAvailability availability = getBridgeAvailability(neighbour, false);
        if (!availability.isEnabled()) { statusMessage = availability.getReason(); return false; }
        boolean ok = gameState.getInfrastructureService().buildBridge((Builder) selectedUnit,
                selectedUnit.getPosition(), neighbour);
        statusMessage = ok ? "Bridge built" : "A bridge requires a river edge";
        return ok;
    }

    public boolean onDemolishBridge(HexCoordinate neighbour) {
        if (!(selectedUnit instanceof Builder)) return false;
        ActionAvailability availability = getBridgeAvailability(neighbour, true);
        if (!availability.isEnabled()) { statusMessage = availability.getReason(); return false; }
        boolean ok = gameState.getInfrastructureService().demolishBridge((Builder) selectedUnit,
                selectedUnit.getPosition(), neighbour);
        statusMessage = ok ? "Bridge demolished (no refund)" : "No removable bridge on that edge";
        return ok;
    }

    public boolean onDemolishBuilding() {
        return selectedUnit == null ? false : onDemolishBuilding(selectedUnit.getPosition());
    }

    public boolean onDemolishBuilding(HexCoordinate coordinate) {
        if (!(selectedUnit instanceof Builder)) return false;
        ActionAvailability availability = getBuildingDemolitionAvailability(coordinate);
        if (!availability.isEnabled()) { statusMessage = availability.getReason(); return false; }
        boolean ok = gameState.getInfrastructureService().demolishBuilding((Builder) selectedUnit,
                coordinate);
        statusMessage = ok ? "Building demolished (no refund)" : "No removable building here";
        return ok;
    }

    /** Whether the selected builder could place this building on some adjacent valid hex now. */
    public boolean canBuildType(BuildingType type) {
        return getBuildTypeAvailability(type).isEnabled();
    }

    public ActionAvailability getRoadAvailability(boolean demolish) {
        ActionAvailability builder = selectedBuilderActionAvailability();
        if (!builder.isEnabled()) return builder;
        HexCoordinate position = selectedUnit.getPosition();
        Hex hex = gameState.getMap().getHex(position);
        if (demolish) {
            return gameState.getMap().hasRoad(position)
                    ? ActionAvailability.enabled("Demolish this road for 1 AP; no refund")
                    : ActionAvailability.disabled("There is no road on this hex");
        }
        if (gameState.getMap().hasRoad(position)) return ActionAvailability.disabled("This hex already has a road");
        if (!hex.getIsExplored()) return ActionAvailability.disabled("Explore this hex before building a road");
        if (!gameState.getPlayer().isInTerritory(position)) {
            return ActionAvailability.disabled("Roads can only be built in your territory");
        }
        if (hex.getTerrainType() == Constants.TerrainType.SEA) {
            return ActionAvailability.disabled("Roads cannot be built at sea");
        }
        if (hex.getTerrainType() == Constants.TerrainType.MOUNTAIN_RANGE) {
            return ActionAvailability.disabled("Roads cannot cross a Mountain Range");
        }
        return ActionAvailability.enabled("Build a road here for 1 AP");
    }

    public ActionAvailability getBridgeAvailability(HexCoordinate neighbour, boolean demolish) {
        ActionAvailability builder = selectedBuilderActionAvailability();
        if (!builder.isEnabled()) return builder;
        HexCoordinate position = selectedUnit.getPosition();
        if (neighbour == null || !position.findNeighbours().contains(neighbour)
                || !gameState.getMap().containsCoordinate(neighbour)) {
            return ActionAvailability.disabled("Choose an adjacent map edge");
        }
        if (!gameState.getMap().hasRiverBetween(position, neighbour)) {
            return ActionAvailability.disabled("A bridge requires a river edge");
        }
        boolean exists = gameState.getMap().hasBridgeBetween(position, neighbour);
        if (demolish) return exists
                ? ActionAvailability.enabled("Demolish this bridge for 1 AP; no refund")
                : ActionAvailability.disabled("There is no bridge on this river edge");
        return exists ? ActionAvailability.disabled("This river edge already has a bridge")
                : ActionAvailability.enabled("Build a bridge on this river edge for 1 AP");
    }

    public ActionAvailability getWallAvailability(HexCoordinate neighbour, boolean demolish) {
        ActionAvailability builder = selectedBuilderActionAvailability();
        if (!builder.isEnabled()) return builder;
        Builder selectedBuilder = (Builder) selectedUnit;
        HexCoordinate position = selectedBuilder.getPosition();
        if (demolish) {
            return gameState.getInfrastructureService().canDemolishWall(selectedBuilder, position, neighbour)
                    ? ActionAvailability.enabled("Demolish this wall for 1 AP; no refund")
                    : ActionAvailability.disabled("No removable wall exists on this adjacent edge");
        }
        if (gameState.getMap().hasWallBetween(position, neighbour)) {
            return ActionAvailability.disabled("This edge already has a wall");
        }
        String shortage = resourceShortage(Constants.WALL_COST);
        if (shortage != null) return ActionAvailability.disabled(shortage);
        return gameState.getInfrastructureService().canBuildWall(selectedBuilder, position, neighbour)
                ? ActionAvailability.enabled("Build a wall for 1 AP and " + describe(Constants.WALL_COST))
                : ActionAvailability.disabled("Wall requires an adjacent explored land edge touching your territory");
    }

    public ActionAvailability getBuildingDemolitionAvailability(HexCoordinate coordinate) {
        ActionAvailability builder = selectedBuilderActionAvailability();
        if (!builder.isEnabled()) return builder;
        Building building = gameState.getPlayer().getBuildingAt(coordinate);
        if (building == null) return ActionAvailability.disabled("There is no active building here");
        if (building.getType() == BuildingType.TOWN_HALL) {
            return ActionAvailability.disabled("The Town Hall cannot be demolished");
        }
        return gameState.getInfrastructureService().canDemolishBuilding((Builder) selectedUnit, coordinate)
                ? ActionAvailability.enabled("Demolish " + building.getType().name().replace('_', ' ')
                    + " for 1 AP; no refund")
                : ActionAvailability.disabled("Builder must be on or adjacent to this building with 1 AP");
    }

    private ActionAvailability selectedBuilderActionAvailability() {
        if (gameState == null) return ActionAvailability.disabled("Start or load a game first");
        if (!(selectedUnit instanceof Builder)) return ActionAvailability.disabled("Select a Builder first");
        if (!selectedUnit.isAlive()) return ActionAvailability.disabled("This Builder is no longer alive");
        return selectedUnit.getCurrentAP() < 1
                ? ActionAvailability.disabled("Builder needs 1 AP for this action")
                : ActionAvailability.enabled("Builder has enough AP");
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
        return getStationAvailability().isEnabled();
    }

    public ActionAvailability getStationAvailability() {
        if (gameState == null) return ActionAvailability.disabled("Start or load a game first");
        if (!(selectedUnit instanceof Worker)) return ActionAvailability.disabled("Select a Worker first");
        Worker w = (Worker) selectedUnit;
        if (w.getCurrentAP() < Constants.WORKER_STATION_AP_COST) {
            return ActionAvailability.disabled("Worker needs 1 AP to station");
        }
        Building b = gameState.getPlayer().getBuildingAt(w.getPosition());
        if (b == null) return ActionAvailability.disabled("Move the Worker onto a building first");
        if (!b.isActive()) return ActionAvailability.disabled("Ruined buildings cannot host workers");
        if (b.getWorkerCap() <= 0) {
            return ActionAvailability.disabled(b.getType().name().replace('_', ' ') + " cannot host workers");
        }
        if (b.getWorkerCount() >= b.getWorkerCap() && (!w.isStationed() || w.getStationedAt() != b)) {
            return ActionAvailability.disabled("Building worker capacity is full (" + b.getWorkerCount()
                    + "/" + b.getWorkerCap() + ")");
        }
        return ActionAvailability.enabled("Station here for 1 AP (" + b.getWorkerCount() + "/"
                + b.getWorkerCap() + " workers)");
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

    public boolean saveGame(SaveSlot slot, String name) {
        SaveWriteResult result = gameState == null
                ? SaveWriteResult.failure("No active game to save")
                : saveManager.saveWithResult(slot, name, gameState);
        statusMessage = result.getMessage();
        if (result.isSuccessful()) savedStateFingerprint = saveManager.fingerprint(gameState);
        return result.isSuccessful();
    }

    public SaveLoadResult loadGame(SaveSlot slot) {
        SaveLoadResult result = saveManager.load(slot);
        if (result.isSuccessful()) {
            gameState = result.getGameState();
            deselectUnit();
            statusMessage = "Loaded " + slot.getDisplayName();
            savedStateFingerprint = saveManager.fingerprint(gameState);
        } else {
            statusMessage = result.getMessage();
        }
        return result;
    }

    public SavePreview getSavePreview(SaveSlot slot) { return saveManager.preview(slot); }
    public boolean deleteSave(SaveSlot slot) { return saveManager.delete(slot); }

    public SaveAvailability getSaveAvailability() {
        return gameState == null
                ? SaveAvailability.disabled("Start or load a game before saving")
                : gameState.getSaveAvailability();
    }

    public boolean hasUnsavedProgress() {
        if (gameState == null) return false;
        String current = saveManager.fingerprint(gameState);
        return savedStateFingerprint == null || current == null
                || !savedStateFingerprint.equals(current);
    }

    public boolean tradeAtBazaar(Constants.ResourceType sell, Constants.ResourceType buy, int amount) {
        ActionAvailability availability = getBazaarTradeAvailability(sell, buy, amount);
        if (!availability.isEnabled()) {
            statusMessage = availability.getReason();
            return false;
        }
        boolean ok = gameState.tradeAtBazaar(sell, buy, amount);
        statusMessage = ok ? "Bazaar trade completed: " + availability.getReason()
                : "Bazaar trade failed because game state changed";
        return ok;
    }

    public boolean tradeAtPost(HexCoordinate post, Constants.ResourceType sell,
                               Constants.ResourceType buy, int amount) {
        ActionAvailability availability = getTradingPostTradeAvailability(post, sell, buy, amount);
        if (!availability.isEnabled()) {
            statusMessage = availability.getReason();
            return false;
        }
        boolean ok = gameState.tradeAtTradingPost(post, sell, buy, amount);
        statusMessage = ok ? "Trading Post exchange completed: " + availability.getReason()
                : "Trading Post trade failed because game state changed";
        return ok;
    }

    public ActionAvailability getBazaarTradeAvailability(Constants.ResourceType sell,
                                                          Constants.ResourceType buy, int amount) {
        if (gameState == null) return ActionAvailability.disabled("Start or load a game first");
        Building bazaar = gameState.getActiveBazaar();
        if (bazaar == null) return ActionAvailability.disabled("Build an active Bazaar before trading");
        if (!bazaar.canTradeAt(gameState.getCurrentTurn())) {
            return ActionAvailability.disabled("The Bazaar has already traded this turn");
        }
        model.trade.BazaarTradeLevel tier = model.trade.BazaarTradeLevel.forQuantity(amount);
        if (tier == null) return ActionAvailability.disabled("Bazaar amount must be exactly 10, 100, or 500");
        return getTradeResourceAvailability(sell, buy, amount,
                (int) Math.floor(amount * tier.getRate()), "Bazaar");
    }

    public ActionAvailability getTradingPostTradeAvailability(HexCoordinate post,
                                                               Constants.ResourceType sell,
                                                               Constants.ResourceType buy, int amount) {
        if (gameState == null) return ActionAvailability.disabled("Start or load a game first");
        if (post == null || !gameState.getMap().hasTradingPost(post)) {
            return ActionAvailability.disabled("Choose a valid Trading Post");
        }
        if (!gameState.getPlayer().isInTerritory(post)) {
            return ActionAvailability.disabled("Claim this Trading Post inside your territory first");
        }
        if (!gameState.getMap().canTradeAtPost(post, gameState.getCurrentTurn())) {
            return ActionAvailability.disabled("This Trading Post has already traded this turn");
        }
        if (amount <= 0) return ActionAvailability.disabled("Trade amount must be positive");
        return getTradeResourceAvailability(sell, buy, amount,
                (int) Math.floor(amount * .8), "Trading Post");
    }

    private ActionAvailability getTradeResourceAvailability(Constants.ResourceType sell,
                                                              Constants.ResourceType buy,
                                                              int sold, int received,
                                                              String source) {
        if (sell == null || buy == null) return ActionAvailability.disabled("Choose both resources");
        if (sell == buy) return ActionAvailability.disabled("Choose different sell and receive resources");
        if (received <= 0) return ActionAvailability.disabled("Trade amount is too small to receive a resource");
        int available = gameState.getPlayer().getResources().get(sell);
        if (available < sold) {
            return ActionAvailability.disabled("Need " + (sold - available) + " more " + sell.name()
                    + " (have " + available + ", selling " + sold + ")");
        }
        int stored = gameState.getPlayer().getResources().get(buy);
        int capacity = gameState.getPlayer().getResources().getCap(buy);
        if (stored + received > capacity) {
            return ActionAvailability.disabled("Not enough " + buy.name() + " storage: " + stored + "/"
                    + capacity + ", trade would add " + received);
        }
        return ActionAvailability.enabled(source + " will exchange " + sold + " " + sell.name()
                + " for " + received + " " + buy.name());
    }

    public boolean giftTribe(Tribe tribe, Constants.ResourceType resource, int amount) {
        TribeActionAvailability availability = gameState.getGiftAvailability(tribe, resource, amount);
        if (!availability.isAvailable()) { statusMessage = availability.getReason(); return false; }
        boolean ok = gameState.giftTribe(tribe, resource, amount);
        statusMessage = ok ? "Gift sent to " + tribe.getName() : "Gift could not be completed"; return ok;
    }
    public boolean tradeWithTribe(Tribe tribe, Constants.ResourceType sell,
                                  Constants.ResourceType buy, int amount) {
        TribeActionAvailability availability = gameState.getTradeAvailability(tribe, sell, buy, amount);
        if (!availability.isAvailable()) { statusMessage = availability.getReason(); return false; }
        boolean ok = gameState.tradeWithTribe(tribe, sell, buy, amount);
        statusMessage = ok ? "Tribe trade completed" : "Tribe trade could not be completed"; return ok;
    }
    public MissionActionResult requestMission(Tribe tribe) {
        MissionActionResult result = gameState.requestMission(tribe);
        statusMessage = "Mission: " + result.name().replace('_', ' '); return result;
    }
    public MissionActionResult turnInMission(Tribe tribe) {
        MissionActionResult result = gameState.turnInMission(tribe);
        statusMessage = "Mission turn-in: " + result.name().replace('_', ' '); return result;
    }
    public MissionActionResult cancelMission(Tribe tribe) {
        MissionActionResult result = gameState.cancelMission(tribe);
        statusMessage = "Mission cancellation: " + result.name().replace('_', ' '); return result;
    }
    public DiplomacyResult declareWar(Tribe tribe) {
        DiplomacyResult result = gameState.declareWar(tribe);
        statusMessage = "War: " + result.name().replace('_', ' '); return result;
    }
    public DiplomacyResult requestPeace(Tribe tribe) {
        DiplomacyResult result = gameState.requestPeace(tribe);
        statusMessage = result == DiplomacyResult.SUCCESS ? "Peace request pending"
                : "Peace: " + result.name().replace('_', ' '); return result;
    }
    public DiplomacyResult requestAlliance(Tribe tribe) {
        DiplomacyResult result = gameState.requestAlliance(tribe);
        statusMessage = "Alliance: " + result.name().replace('_', ' '); return result;
    }

    public CombatReport attackSelectedTribeCamp(Tribe tribe) {
        if (!(selectedUnit instanceof MilitaryUnit)) {
            statusMessage = "Select a military unit first";
            return null;
        }
        CombatReport report = gameState.attackTribeCamp((MilitaryUnit) selectedUnit, tribe);
        if (report == null) {
            statusMessage = "Selected military unit cannot attack this camp";
            return null;
        }
        statusMessage = report.isStructureAttack() ? "Structure hit for " + report.getStructureDamage()
                : report.getCasualty();
        presentCombatReport(report);
        return report;
    }

    private void presentCombatReport(CombatReport report) {
        if (mainWindow == null || gameState == null || report == null) return;
        gameState.beginCombatPresentation();
        try {
            mainWindow.showCombatReport(report);
        } finally {
            gameState.endCombatPresentation();
        }
    }

    public TribeActionAvailability getTribeActionAvailability(Tribe tribe, TribeAction action) {
        return gameState.getTribeActionAvailability(tribe, action);
    }

    public TribeActionAvailability getGiftAvailability(Tribe tribe, Constants.ResourceType resource, int amount) {
        return gameState.getGiftAvailability(tribe, resource, amount);
    }

    public TribeActionAvailability getTradeAvailability(Tribe tribe, Constants.ResourceType sell,
                                                         Constants.ResourceType buy, int amount) {
        return gameState.getTradeAvailability(tribe, sell, buy, amount);
    }

    public Tribe getLastOpenedTribe() { return lastOpenedTribe; }

    /** Hexes the selected unit can reach this turn (used to tint the map green). */
    public List<HexCoordinate> getReachableHexes() {
        if (selectedUnit == null || !selectedUnit.canAct()) return Collections.emptyList();
        return new ArrayList<>(PathFinder.reachable(
                gameState.getMap(), selectedUnit.getPosition(), selectedUnit.getCurrentAP(),
                gameState.getMovementPolicy()));
    }

    private int indexOfUnit(Unit unit) {
        return gameState.getPlayer().getUnits().indexOf(unit);
    }

    private void refreshRemoteView() {
        if (mainWindow != null) mainWindow.onRemoteStateChanged();
    }

    public void disconnectNetwork() {
        if (networkManager != null) networkManager.disconnect();
    }
}
