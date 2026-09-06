package client;

import model.ActionAvailability;
import model.BorderExpander;
import model.Builder;
import model.Building;
import model.Constants;
import model.Explorer;
import model.GameMap;
import model.GameState;
import model.Hex;
import model.HexCoordinate;
import model.Player;
import model.ResourceAmount;
import model.ScoreCalculator;
import model.Unit;
import model.Worker;
import model.disaster.BearAttackEvent;
import model.disaster.DisasterEvent;
import model.military.MilitaryUnitType;
import model.military.UnitFactory;
import model.technology.TechnologyRegistry;
import model.technology.TechnologyType;
import model.tribe.Tribe;
import network.dto.GameStateSnapshotDto;
import network.dto.GameStateSnapshotDto.BuildingDto;
import network.dto.GameStateSnapshotDto.CoordinateDto;
import network.dto.GameStateSnapshotDto.EdgeDto;
import network.dto.GameStateSnapshotDto.HexDto;
import network.dto.GameStateSnapshotDto.PlayerDto;
import network.dto.GameStateSnapshotDto.UnitDto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Read-only UI projection built from JSON. It intentionally does not participate in
 * authoritative mutation; scoped actions are always sent back through NetworkManager.
 */
public final class ClientGameStateProjection extends GameState {
    private final GameStateSnapshotDto snapshot;
    private final GameMap projectedMap;
    private final Player projectedPlayer;
    private final HexCoordinate projectedTownHall;

    public ClientGameStateProjection(GameStateSnapshotDto snapshot) {
        super(requireSnapshot(snapshot).getWidth(), snapshot.getHeight(), 0L);
        this.snapshot = snapshot;
        configureTownHall(snapshot);
        configureTechnologies(snapshot);
        getSeasonCycle().restoreTurn(Math.max(1, snapshot.getSeasonTurn()));
        projectedMap = createMap(snapshot);
        projectedTownHall = coordinate(snapshot.getTownHall());
        projectedPlayer = createPlayer(snapshot.getPlayer());
    }

    private static GameStateSnapshotDto requireSnapshot(GameStateSnapshotDto snapshot) {
        if (snapshot == null) throw new IllegalArgumentException("state snapshot is required");
        if (snapshot.getWidth() < 1 || snapshot.getHeight() < 1) {
            throw new IllegalArgumentException("snapshot map dimensions are invalid");
        }
        return snapshot;
    }

    private void configureTownHall(GameStateSnapshotDto dto) {
        if (dto.getTownHallState() == null) return;
        model.townhall.TownHallLevel target = model.townhall.TownHallLevel.valueOf(
                dto.getTownHallState().getLevel());
        while (getTownHall().getLevel() != target && getTownHall().canUpgrade()) {
            getTownHall().upgrade();
        }
        if (dto.getTownHallState().hasDefensiveArchitecture()) {
            getTownHall().enableDefensiveArchitecture();
        }
        int damage = getTownHall().getCurrentHp() - dto.getTownHallState().getCurrentHp();
        if (damage > 0) getTownHall().takeDamage(damage);
    }

    private void configureTechnologies(GameStateSnapshotDto dto) {
        TechnologyRegistry registry = super.getPhaseTwoTechnologies();
        for (String name : dto.getCompletedPhaseTwoTechnologies()) {
            TechnologyType technology = TechnologyType.valueOf(name);
            registry.markQueued(technology);
            registry.complete(technology);
        }
        for (String name : dto.getQueuedPhaseTwoTechnologies()) {
            TechnologyType technology = TechnologyType.valueOf(name);
            if (registry.canQueue(technology)) registry.markQueued(technology);
        }
    }

    private GameMap createMap(GameStateSnapshotDto dto) {
        Map<HexCoordinate, Hex> cells = new HashMap<>();
        for (HexDto hexDto : dto.getHexes()) {
            HexCoordinate position = coordinate(hexDto.getCoordinate());
            Hex hex = new Hex(position, Constants.TerrainType.valueOf(hexDto.getTerrain()));
            for (Map.Entry<String, Integer> resource : hexDto.getNaturalResources().entrySet()) {
                hex.addNaturalResource(Constants.NaturalResourceType.valueOf(resource.getKey()),
                        resource.getValue());
            }
            if (hexDto.hasEverHadResource() && !hex.everHadResource()) {
                hex.addNaturalResource(Constants.NaturalResourceType.WOOD, 1);
                hex.decreaseNaturalResource(Constants.NaturalResourceType.WOOD, 1);
            }
            hex.explore(hexDto.isExplored());
            hex.setVisible(hexDto.isVisible());
            hex.expand(hexDto.isExpanded());
            hex.setHasBuilding(hexDto.hasBuilding());
            hex.blockForTurns(hexDto.getBlockedTurns());
            if (hexDto.hasRoad()) hex.buildRoad();
            cells.put(position, hex);
        }
        GameMap map = new GameMap(cells);
        for (EdgeDto edge : dto.getRivers()) {
            HexCoordinate first = coordinate(edge.getFirst());
            HexCoordinate second = coordinate(edge.getSecond());
            map.addRiver(first, second);
            if (edge.hasBridge()) map.buildBridge(first, second);
        }
        for (EdgeDto edge : dto.getWalls()) {
            HexCoordinate first = coordinate(edge.getFirst());
            HexCoordinate second = coordinate(edge.getSecond());
            map.buildWall(first, second);
            if (edge.getCurrentHp() < edge.getMaxHp()) {
                map.getWallBetween(first, second).takeDamage(edge.getMaxHp() - edge.getCurrentHp());
            }
        }
        for (CoordinateDto post : dto.getTradingPosts()) map.addTradingPost(coordinate(post));
        return map;
    }

    private Player createPlayer(PlayerDto dto) {
        if (dto == null) throw new IllegalArgumentException("snapshot player is required");
        Player player = new Player(dto.getName());
        player.getUnits().clear();
        player.getBuildings().clear();
        player.getTerritory().clear();

        int capacity = dto.getCapacities().values().stream().mapToInt(Integer::intValue)
                .max().orElse(Constants.INITIAL_STORAGE);
        ResourceAmount existing = player.getResources().getCurrent();
        player.getResources().spend(existing);
        player.getResources().setCapacity(capacity);
        ResourceAmount current = new ResourceAmount();
        for (Constants.ResourceType type : Constants.ResourceType.values()) {
            current.set(type, dto.getResources().getOrDefault(type.name(), 0));
        }
        player.addResources(current);
        for (String technology : dto.getLegacyTechnologies()) {
            player.completeLegacyTechnology(Constants.TechnologyType.valueOf(technology));
        }
        for (CoordinateDto coordinate : dto.getTerritory()) {
            player.expandTerritory(coordinate(coordinate));
        }

        List<Building> buildings = new ArrayList<>();
        for (BuildingDto buildingDto : dto.getBuildings()) {
            Building building = new Building(coordinate(buildingDto.getPosition()),
                    Constants.BuildingType.valueOf(buildingDto.getType()));
            if (building.getType() == Constants.BuildingType.TOWN_HALL) {
                building.bindTownHallProjection(getTownHall());
            } else {
                building.synchronizeHealth(buildingDto.getCurrentHp(), buildingDto.getMaxHp());
            }
            buildings.add(building);
            player.addBuilding(building);
        }

        List<UnitDto> units = dto.getUnits();
        for (UnitDto unitDto : units) player.addUnit(createUnit(unitDto));
        for (int i = 0; i < units.size(); i++) {
            Unit unit = player.getUnits().get(i);
            UnitDto unitDto = units.get(i);
            if (unit instanceof Worker && unitDto.getStationedBuildingIndex() >= 0
                    && unitDto.getStationedBuildingIndex() < buildings.size()) {
                unit.setCurrentAP(unit.getMaxAP());
                ((Worker) unit).station(buildings.get(unitDto.getStationedBuildingIndex()));
            }
            unit.setCurrentAP(unitDto.getCurrentAp());
            if (!unitDto.isAlive()) unit.kill();
        }
        return player;
    }

    private Unit createUnit(UnitDto dto) {
        HexCoordinate position = coordinate(dto.getPosition());
        Constants.UnitType type = Constants.UnitType.valueOf(dto.getType());
        Unit unit = switch (type) {
            case EXPLORER -> new Explorer(position);
            case BUILDER -> new Builder(position);
            case WORKER -> new Worker(position);
            case BORDER_EXPANDER -> new BorderExpander(position);
            case MILITARY -> new UnitFactory().createUnit(
                    MilitaryUnitType.valueOf(dto.getMilitaryType()), position);
            case BEAR -> throw new IllegalArgumentException("A Bear cannot belong to the player");
        };
        if (unit instanceof Builder) {
            while (((Builder) unit).getCharges() > dto.getBuilderCharges()) ((Builder) unit).useCharge();
        }
        if (unit instanceof Explorer && dto.isAutoExplore()) ((Explorer) unit).toggleAutoExplore();
        int damage = unit.getCurrentHp() - dto.getCurrentHp();
        if (damage > 0) unit.takeDamage(damage);
        unit.setState(Constants.UnitState.valueOf(dto.getState()));
        unit.setCurrentAP(dto.getCurrentAp());
        return unit;
    }

    private static HexCoordinate coordinate(CoordinateDto dto) {
        if (dto == null) throw new IllegalArgumentException("snapshot coordinate is required");
        return new HexCoordinate(dto.getQ(), dto.getR());
    }

    @Override public GameMap getMap() { return projectedMap; }
    @Override public Player getPlayer() { return projectedPlayer; }
    @Override public HexCoordinate getTownHallPos() { return projectedTownHall; }
    @Override public int getCurrentTurn() { return snapshot.getCurrentTurn(); }
    @Override public boolean isGameOver() { return snapshot.isGameOver(); }
    @Override public boolean isStarving() { return snapshot.isStarving(); }
    @Override public String getGameOverReason() { return snapshot.getGameOverReason(); }
    @Override public int getFinalScore() { return snapshot.getFinalScore(); }
    @Override public List<String> getLastTurnEvents() { return snapshot.getLastTurnEvents(); }
    @Override public int getCurrentScore() { return ScoreCalculator.calculate(projectedPlayer, projectedMap); }
    @Override public boolean hasSailing() { return snapshot.hasSailing(); }
    @Override public boolean hasSteelTools() { return snapshot.hasSteelTools(); }
    @Override public boolean hasDefensiveArchitecture() { return snapshot.hasDefensiveArchitecture(); }
    @Override public DisasterEvent getLastDisasterEvent() { return null; }
    @Override public BearAttackEvent getActiveBearAttack() { return null; }
    @Override public List<Tribe> getTribes() { return List.of(); }
    @Override public List<Tribe> getDiscoveredTribes() { return List.of(); }
    @Override public List<Tribe> getVisibleTribes() { return List.of(); }
    @Override public Tribe getVisibleTribeAt(HexCoordinate coordinate) { return null; }
    @Override public Set<HexCoordinate> getEligibleTradingPosts() { return projectedMap.getTradingPosts(); }
    @Override public Building getActiveBazaar() {
        for (Building building : projectedPlayer.getBuildings()) {
            if (building.getType() == Constants.BuildingType.BAZAAR && building.isActive()) return building;
        }
        return null;
    }

    @Override
    public ActionAvailability getBuildSiteAvailability(HexCoordinate coordinate,
            Constants.BuildingType type) {
        Hex hex = projectedMap.getHex(coordinate);
        if (type == null) return ActionAvailability.disabled("Choose a building type");
        if (hex == null) return ActionAvailability.disabled("This hex is outside the map");
        if (!hex.getIsExplored()) return ActionAvailability.disabled("Explore this hex before building");
        if (!projectedPlayer.isInTerritory(coordinate)) {
            return ActionAvailability.disabled("This hex is outside your territory");
        }
        if (projectedPlayer.getBuildingAt(coordinate) != null || hex.getHasBuilding()) {
            return ActionAvailability.disabled("This hex already contains a building");
        }
        if (hex.getTerrainType() == Constants.TerrainType.SEA
                || hex.getTerrainType() == Constants.TerrainType.MOUNTAIN_RANGE) {
            return ActionAvailability.disabled("This terrain cannot hold a building");
        }
        boolean allowed = switch (type) {
            case LUMBER_MILL -> hex.hasResource(Constants.NaturalResourceType.WOOD);
            case STONE_MINE -> hex.hasResource(Constants.NaturalResourceType.STONE);
            case IRON_MINE -> hex.hasResource(Constants.NaturalResourceType.IRON);
            case FARM -> hex.hasResource(Constants.NaturalResourceType.WHEAT)
                    || hex.hasResource(Constants.NaturalResourceType.RICE);
            case STABLE -> hex.getTerrainType() == Constants.TerrainType.PLAIN
                    || hex.hasResource(Constants.NaturalResourceType.COW)
                    || hex.hasResource(Constants.NaturalResourceType.SHEEP);
            case TOWNSHIP, BAZAAR -> !hex.everHadResource();
            case DOCK -> projectedMap.isCoastal(coordinate);
            case MONUMENT -> hex.getTerrainType() == Constants.TerrainType.PLAIN
                    && !hex.everHadResource();
            case TOWN_HALL -> false;
        };
        return allowed ? ActionAvailability.enabled(type + " can be built here")
                : ActionAvailability.disabled("This site does not satisfy " + type + " requirements");
    }
}
