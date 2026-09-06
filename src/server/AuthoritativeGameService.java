package server;

import model.ActionAvailability;
import model.Builder;
import model.Building;
import model.Constants;
import model.GameState;
import model.Hex;
import model.HexCoordinate;
import model.PathFinder;
import model.ResourceAmount;
import model.Unit;
import network.GameStateSnapshotCodec;
import network.messages.BuildRequest;
import network.messages.MoveUnitRequest;
import network.messages.StartGameRequest;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/** Owns the single authoritative game and serializes all mutations under one lock. */
public final class AuthoritativeGameService {
    private final ReentrantLock stateLock = new ReentrantLock();
    private GameState gameState;
    private long revision;

    public ActionResult start(StartGameRequest request) {
        stateLock.lock();
        try {
            int width = request == null || request.getWidth() == null ? 15 : request.getWidth();
            int height = request == null || request.getHeight() == null ? 13 : request.getHeight();
            if (width < 5 || height < 5 || width > 50 || height > 50) {
                return ActionResult.failure("INVALID_MAP_SIZE",
                        "Map width and height must be between 5 and 50.");
            }
            gameState = request != null && request.getSeed() != null
                    ? new GameState(width, height, request.getSeed())
                    : new GameState(width, height);
            revision++;
            return ActionResult.success("Game started.");
        } finally {
            stateLock.unlock();
        }
    }

    public ActionResult move(MoveUnitRequest request) {
        stateLock.lock();
        try {
            ActionResult readiness = requireRunningGame();
            if (readiness != null) return readiness;
            if (request == null) return malformed("MOVE_UNIT payload is required.");
            List<Unit> units = gameState.getPlayer().getUnits();
            if (request.getUnitIndex() < 0 || request.getUnitIndex() >= units.size()) {
                return ActionResult.failure("UNIT_NOT_FOUND", "The requested unit does not exist.");
            }
            Unit unit = units.get(request.getUnitIndex());
            HexCoordinate destination = new HexCoordinate(request.getX(), request.getY());
            ActionAvailability validation = movementAvailability(unit, destination);
            if (!validation.isEnabled()) {
                return ActionResult.failure("INVALID_MOVE", validation.getReason());
            }
            if (!unit.moveTo(gameState.getMap(), destination, gameState.getMovementPolicy())) {
                return ActionResult.failure("MOVE_REJECTED", "Movement could not be completed.");
            }
            gameState.updateVisibility();
            revision++;
            return ActionResult.success("Unit moved for " + validation.getReason() + ".");
        } finally {
            stateLock.unlock();
        }
    }

    public ActionResult endTurn() {
        stateLock.lock();
        try {
            ActionResult readiness = requireRunningGame();
            if (readiness != null) return readiness;
            if (gameState.isGameOver()) {
                return ActionResult.failure("GAME_OVER", "The game is already over.");
            }
            try {
                gameState.endTurn();
            } catch (IllegalStateException exception) {
                return ActionResult.failure("TURN_REJECTED", exception.getMessage());
            }
            revision++;
            return ActionResult.success("Turn ended.");
        } finally {
            stateLock.unlock();
        }
    }

    public ActionResult build(BuildRequest request) {
        stateLock.lock();
        try {
            ActionResult readiness = requireRunningGame();
            if (readiness != null) return readiness;
            if (request == null || request.getBuildingType() == null) {
                return malformed("BUILD requires builderIndex, buildingType, x and y.");
            }
            List<Unit> units = gameState.getPlayer().getUnits();
            if (request.getBuilderIndex() < 0 || request.getBuilderIndex() >= units.size()) {
                return ActionResult.failure("BUILDER_NOT_FOUND", "The requested Builder does not exist.");
            }
            Unit candidate = units.get(request.getBuilderIndex());
            if (!(candidate instanceof Builder)) {
                return ActionResult.failure("NOT_A_BUILDER", "The selected unit is not a Builder.");
            }
            Builder builder = (Builder) candidate;
            if (!builder.isAlive()) return ActionResult.failure("BUILDER_DEAD", "This Builder is no longer alive.");
            if (!builder.hasCharges()) {
                return ActionResult.failure("NO_BUILD_CHARGES", "Builder has no construction charges left.");
            }
            Constants.BuildingType type;
            try {
                type = Constants.BuildingType.valueOf(request.getBuildingType());
            } catch (IllegalArgumentException exception) {
                return ActionResult.failure("UNKNOWN_BUILDING", "Unknown building type: " + request.getBuildingType());
            }
            if (type == Constants.BuildingType.TOWN_HALL) {
                return ActionResult.failure("SECOND_TOWN_HALL", "A second Town Hall cannot be built.");
            }
            HexCoordinate coordinate = new HexCoordinate(request.getX(), request.getY());
            if (builder.getPosition().distanceTo(coordinate) > 1) {
                return ActionResult.failure("BUILDER_TOO_FAR",
                        "Builder must be on or adjacent to the build site.");
            }
            ResourceAmount cost = gameState.getBuildCost(type);
            if (cost == null) {
                return ActionResult.failure("NO_BUILD_COST", "No construction cost is defined for " + type + ".");
            }
            String shortage = resourceShortage(cost);
            if (shortage != null) return ActionResult.failure("INSUFFICIENT_RESOURCES", shortage);
            ActionAvailability site = gameState.getBuildSiteAvailability(coordinate, type);
            if (!site.isEnabled()) return ActionResult.failure("INVALID_BUILD_SITE", site.getReason());

            gameState.getPlayer().spend(cost);
            Building building = new Building(coordinate, type);
            gameState.getPlayer().addBuilding(building);
            gameState.getMap().getHex(coordinate).setHasBuilding(true);
            gameState.onBuildingConstructed(building);
            builder.useCharge();
            if (!builder.hasCharges()) builder.kill();
            gameState.updateVisibility();
            revision++;
            return ActionResult.success("Built " + type.name().replace('_', ' ') + ".");
        } finally {
            stateLock.unlock();
        }
    }

    public Snapshot snapshot() {
        stateLock.lock();
        try {
            if (gameState == null) return null;
            return new Snapshot(revision, GameStateSnapshotCodec.encode(gameState));
        } finally {
            stateLock.unlock();
        }
    }

    private ActionAvailability movementAvailability(Unit unit, HexCoordinate destination) {
        if (!unit.isAlive()) return ActionAvailability.disabled("This unit is no longer alive.");
        if (!gameState.getMap().containsCoordinate(destination)) {
            return ActionAvailability.disabled("Destination is outside the map.");
        }
        if (destination.equals(unit.getPosition())) {
            return ActionAvailability.disabled("This is the unit's current position.");
        }
        Hex hex = gameState.getMap().getHex(destination);
        if (hex.isBlocked()) {
            return ActionAvailability.disabled("Destination is blocked by a disaster for "
                    + hex.getBlockedTurns() + " more turn(s).");
        }
        if (hex.getTerrainType() == Constants.TerrainType.MOUNTAIN_RANGE) {
            return ActionAvailability.disabled("Mountain Range is impassable.");
        }
        if (hex.getTerrainType() == Constants.TerrainType.SEA && !gameState.hasSailing()) {
            return ActionAvailability.disabled("Research SAILING before entering sea.");
        }
        Unit occupant = gameState.getPlayer().getUnitAt(destination);
        if (occupant != null && occupant != unit) {
            return ActionAvailability.disabled("Destination is occupied by " + occupant.getUnitType() + ".");
        }
        if (unit.getCurrentAP() <= 0) {
            return ActionAvailability.disabled("This unit has no action points remaining.");
        }
        int cost = PathFinder.pathCost(gameState.getMap(), unit.getPosition(), destination,
                Integer.MAX_VALUE / 4, gameState.getMovementPolicy());
        if (cost < 0) return ActionAvailability.disabled("No traversable path reaches this hex.");
        if (cost > unit.getCurrentAP()) {
            return ActionAvailability.disabled("Movement costs " + cost + " AP; only "
                    + unit.getCurrentAP() + " AP remains.");
        }
        return ActionAvailability.enabled(cost + " AP");
    }

    private String resourceShortage(ResourceAmount cost) {
        for (Constants.ResourceType type : Constants.ResourceType.values()) {
            int available = gameState.getPlayer().getResources().get(type);
            int required = cost.get(type);
            if (available < required) {
                return "Not enough " + type + ": requires " + required + ", available " + available + ".";
            }
        }
        return null;
    }

    private ActionResult requireRunningGame() {
        return gameState == null
                ? ActionResult.failure("GAME_NOT_STARTED", "Start a game before sending gameplay actions.")
                : null;
    }

    private ActionResult malformed(String message) {
        return ActionResult.failure("MALFORMED_REQUEST", message);
    }

    public static final class Snapshot {
        private final long revision;
        private final String encodedState;

        private Snapshot(long revision, String encodedState) {
            this.revision = revision;
            this.encodedState = encodedState;
        }

        public long getRevision() { return revision; }
        public String getEncodedState() { return encodedState; }
    }
}
