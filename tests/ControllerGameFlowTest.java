import controller.GameController;
import model.Builder;
import model.Building;
import model.Constants;
import model.GameState;
import model.Hex;
import model.HexCoordinate;
import model.ResourceAmount;
import model.Unit;
import model.tribe.Tribe;
import model.tribe.mission.MissionActionResult;

/** End-to-end Controller -> GameState paths for core player commands. */
public final class ControllerGameFlowTest {
    public static void main(String[] args) {
        GameController controller = new GameController();
        controller.startNewGame(777L);
        GameState state = controller.getGameState();

        Builder builder = findBuilder(state);
        controller.onHexClicked(builder.getPosition());
        require(controller.getSelectedUnit() == builder, "Controller must select the Builder");
        HexCoordinate site = prepareFarmSite(state, builder);
        int charges = builder.getCharges();
        controller.onBuildChosen(Constants.BuildingType.FARM);
        require(controller.isBuildMode(), "build command must enter placement mode");
        controller.onHexClicked(site);
        require(state.getPlayer().getBuildingAt(site) != null
                        && state.getPlayer().getBuildingAt(site).getType() == Constants.BuildingType.FARM
                        && builder.getCharges() == charges - 1,
                "Controller placement must mutate authoritative building and Builder state");

        state.getPlayer().addResources(ResourceAmount.of(100, 100, 100, 100));
        controller.onResearchChosen(Constants.TechnologyType.STORAGE_I);
        require(state.getTownHall().getCommandSlot().isBusy(),
                "Controller research must occupy the Town Hall command slot");
        require(!controller.getCivilianRecruitmentAvailability(Constants.UnitType.EXPLORER).isEnabled(),
                "recruitment must observe the same occupied command slot");
        advance(state, 3);
        require(state.getPlayer().hasResearched(Constants.TechnologyType.STORAGE_I),
                "Controller-started research must complete through endTurn");

        int unitsBefore = state.getPlayer().getUnitCount();
        controller.onRecruitUnit(Constants.UnitType.EXPLORER);
        require(state.getTownHall().getCommandSlot().isBusy(),
                "Controller recruitment must occupy the shared command slot");
        advance(state, Constants.UNIT_BUILD_TURNS.get(Constants.UnitType.EXPLORER));
        require(state.getPlayer().getUnitCount() == unitsBefore + 1,
                "Controller-started recruitment must create the unit through GameState");

        HexCoordinate bazaarSite = state.getTownHallPos().findNeighbours().get(1);
        state.getPlayer().addBuilding(new Building(bazaarSite, Constants.BuildingType.BAZAAR));
        int stoneBefore = state.getPlayer().getResources().get(Constants.ResourceType.STONE);
        require(controller.tradeAtBazaar(Constants.ResourceType.FOOD,
                        Constants.ResourceType.STONE, 10),
                "Controller trade must complete through the authoritative TradeService");
        require(state.getPlayer().getResources().get(Constants.ResourceType.STONE) == stoneBefore + 5,
                "Controller trade must apply the exact Bazaar tier output");

        Tribe tribe = state.getTribes().get(0);
        tribe.discover();
        state.getMap().getHex(tribe.getCampCoordinate()).setVisible(true);
        tribe.getRelation().setScore(20);
        require(controller.requestMission(tribe) == MissionActionResult.SUCCESS
                        && state.getMission(tribe) != null,
                "Controller mission action must create authoritative mission state");
        System.out.println("ControllerGameFlowTest passed");
    }

    private static Builder findBuilder(GameState state) {
        for (Unit unit : state.getPlayer().getUnits()) {
            if (unit instanceof Builder) return (Builder) unit;
        }
        throw new AssertionError("starting Builder missing");
    }

    private static HexCoordinate prepareFarmSite(GameState state, Builder builder) {
        for (HexCoordinate coordinate : builder.getPosition().findNeighbours()) {
            Hex hex = state.getMap().getHex(coordinate);
            if (hex == null || state.getPlayer().getBuildingAt(coordinate) != null) continue;
            hex.setTerrainType(Constants.TerrainType.GRASSLAND);
            hex.addNaturalResource(Constants.NaturalResourceType.WHEAT, 100);
            hex.explore(true);
            state.getPlayer().expandTerritory(coordinate);
            return coordinate;
        }
        throw new AssertionError("Builder has no adjacent farm fixture site");
    }

    private static void advance(GameState state, int turns) {
        for (int i = 0; i < turns; i++) state.endTurn();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
