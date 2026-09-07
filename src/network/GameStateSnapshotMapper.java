package network;

import model.Building;
import model.Builder;
import model.Constants;
import model.Explorer;
import model.GameState;
import model.Hex;
import model.HexCoordinate;
import model.HexEdge;
import model.Player;
import model.Unit;
import model.Worker;
import model.military.MilitaryUnit;
import model.technology.TechnologyType;
import network.dto.GameStateSnapshotDto;
import network.dto.GameStateSnapshotDto.BuildingDto;
import network.dto.GameStateSnapshotDto.CoordinateDto;
import network.dto.GameStateSnapshotDto.EdgeDto;
import network.dto.GameStateSnapshotDto.HexDto;
import network.dto.GameStateSnapshotDto.PlayerDto;
import network.dto.GameStateSnapshotDto.TownHallDto;
import network.dto.GameStateSnapshotDto.UnitDto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Maps the domain graph to an acyclic, Gson-friendly snapshot DTO. */
public final class GameStateSnapshotMapper {
    private GameStateSnapshotMapper() {}

    public static GameStateSnapshotDto toDto(GameState state) {
        if (state == null) throw new IllegalArgumentException("game state is required");
        int width = 0;
        int height = 0;
        List<HexDto> hexes = new ArrayList<>();
        for (Hex hex : state.getMap().getAllHexes()) {
            width = Math.max(width, hex.getCoordinate().getQ() + 1);
            height = Math.max(height, hex.getCoordinate().getR() + 1);
            Map<String, Integer> resources = new HashMap<>();
            hex.getNaturalResources().forEach((type, amount) -> resources.put(type.name(), amount));
            hexes.add(new HexDto(coordinate(hex.getCoordinate()), hex.getTerrainType().name(),
                    resources, hex.getIsExplored(), hex.isVisible(), hex.getIsExpanded(),
                    hex.getHasBuilding(), hex.everHadResource(), hex.getBlockedTurns(), hex.hasRoad()));
        }

        Player player = state.getPlayer();
        List<BuildingDto> buildings = new ArrayList<>();
        Map<Building, Integer> buildingIndexes = new java.util.IdentityHashMap<>();
        for (Building building : player.getBuildings()) {
            if (building.isActive()) {
                buildingIndexes.put(building, buildings.size());
                buildings.add(new BuildingDto(coordinate(building.getPosition()),
                        building.getType().name(), building.getCurrentHp(), building.getMaxHp()));
            }
        }

        List<UnitDto> units = new ArrayList<>();
        for (Unit unit : player.getUnits()) {
            String militaryType = unit instanceof MilitaryUnit
                    ? ((MilitaryUnit) unit).getMilitaryUnitType().name() : null;
            int charges = unit instanceof Builder ? ((Builder) unit).getCharges() : 0;
            boolean autoExplore = unit instanceof Explorer && ((Explorer) unit).isAutoExploreMode();
            int stationedAt = unit instanceof Worker && ((Worker) unit).getStationedAt() != null
                    ? buildingIndexes.getOrDefault(((Worker) unit).getStationedAt(), -1) : -1;
            units.add(new UnitDto(unit.getUnitType().name(), militaryType,
                    coordinate(unit.getPosition()), unit.getCurrentAP(), unit.getCurrentHp(),
                    unit.isAlive(), unit.getState().name(), charges, autoExplore, stationedAt));
        }

        Map<String, Integer> resources = new HashMap<>();
        Map<String, Integer> capacities = new HashMap<>();
        for (Constants.ResourceType type : Constants.ResourceType.values()) {
            resources.put(type.name(), player.getResources().get(type));
            capacities.put(type.name(), player.getResources().getCap(type));
        }
        List<String> legacy = player.getResearchedLegacyTechnologies().stream()
                .map(Enum::name).sorted().toList();
        List<CoordinateDto> territory = player.getTerritory().stream()
                .map(GameStateSnapshotMapper::coordinate).toList();
        PlayerDto playerDto = new PlayerDto(player.getName(), resources, capacities, legacy,
                territory, units, buildings);

        List<EdgeDto> rivers = state.getMap().getRiverEdges().stream()
                .map(edge -> edge(edge, true)).toList();
        List<EdgeDto> walls = state.getMap().getWallEdges().stream()
                .filter(HexEdge::hasWall).map(edge -> edge(edge, false)).toList();
        List<CoordinateDto> posts = state.getMap().getTradingPosts().stream()
                .map(GameStateSnapshotMapper::coordinate).toList();
        List<String> completed = state.getPhaseTwoTechnologies().getCompletedTechnologies()
                .stream().map(Enum::name).sorted().toList();
        List<String> queued = new ArrayList<>();
        for (TechnologyType technology : TechnologyType.values()) {
            if (state.getPhaseTwoTechnologies().isQueued(technology)) queued.add(technology.name());
        }
        TownHallDto hall = new TownHallDto(state.getTownHall().getLevel().name(),
                state.getTownHall().getCurrentHp(), state.hasDefensiveArchitecture());
        return new GameStateSnapshotDto(width, height, state.getCurrentTurn(),
                state.getSeasonCycle().getCurrentTurn(), state.isGameOver(), state.isStarving(),
                state.getGameOverReason(), state.getFinalScore(), coordinate(state.getTownHallPos()),
                playerDto, hall, hexes, rivers, walls, posts, completed, queued,
                state.hasSailing(), state.hasSteelTools(), state.hasDefensiveArchitecture(),
                new ArrayList<>(state.getLastTurnEvents()));
    }

    private static CoordinateDto coordinate(HexCoordinate coordinate) {
        return new CoordinateDto(coordinate.getQ(), coordinate.getR());
    }

    private static EdgeDto edge(HexEdge edge, boolean includeBridge) {
        int currentHp = edge.hasWall() ? edge.getWall().getCurrentHp() : 0;
        int maxHp = edge.hasWall() ? edge.getWall().getMaxHp() : 0;
        return new EdgeDto(coordinate(edge.getFirst()), coordinate(edge.getSecond()),
                includeBridge && edge.hasBridge(), currentHp, maxHp);
    }
}
