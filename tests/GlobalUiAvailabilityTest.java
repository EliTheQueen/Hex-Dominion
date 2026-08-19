import controller.GameController;
import model.ActionAvailability;
import model.Builder;
import model.Building;
import model.Constants;
import model.GameState;
import model.Hex;
import model.HexCoordinate;
import model.ResourceAmount;
import model.Unit;
import model.military.MilitaryUnitType;

/** Rule-level regression coverage for the shared enabled/reason UI contract. */
public final class GlobalUiAvailabilityTest {
    public static void main(String[] args) {
        centralizedResultCoversSpecializedActions();
        movementExplainsCostAndProhibitions();
        buildRecruitAndTechnologyReasonsAreSpecific();
        tradeExplainsResourceAndStorageFailures();
        System.out.println("GlobalUiAvailabilityTest passed");
    }

    private static void centralizedResultCoversSpecializedActions() {
        GameController controller = new GameController();
        controller.startNewGame();
        require(controller.getSaveAvailability() instanceof ActionAvailability,
                "save availability must use the global result");
        require(controller.getTribeActionAvailability(controller.getGameState().getTribes().get(0),
                model.tribe.TribeAction.GIFT) instanceof ActionAvailability,
                "mission/diplomacy availability must use the global result");
        assertReason(controller.getSaveAvailability(), "save");
        boolean rejected = false;
        try {
            ActionAvailability.disabled(" ");
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        require(rejected, "an action result must never carry a blank reason");
    }

    private static void movementExplainsCostAndProhibitions() {
        GameController controller = new GameController();
        controller.startNewGame();
        GameState state = controller.getGameState();
        Unit selected = null;
        HexCoordinate reachable = null;
        for (Unit unit : state.getPlayer().getUnits()) {
            for (HexCoordinate neighbour : unit.getPosition().findNeighbours()) {
                if (state.getPlayer().getUnitAt(neighbour) == null
                        && unit.canMoveTo(state.getMap(), neighbour, state.getMovementPolicy())) {
                    selected = unit;
                    reachable = neighbour;
                    break;
                }
            }
            if (reachable != null) break;
        }
        require(selected != null, "fixture needs a movable starting unit");
        controller.onHexClicked(selected.getPosition());
        ActionAvailability allowed = controller.getMovementAvailability(reachable);
        require(allowed.isEnabled() && allowed.getReason().contains("AP"),
                "enabled movement must state exact AP cost");

        HexCoordinate range = null;
        for (Hex hex : state.getMap().getAllHexes()) {
            if (hex.getTerrainType() == Constants.TerrainType.MOUNTAIN_RANGE) {
                range = hex.getCoordinate();
                break;
            }
        }
        require(range != null, "generated map needs a Mountain Range");
        Unit before = controller.getSelectedUnit();
        ActionAvailability blocked = controller.getMovementAvailability(range);
        require(!blocked.isEnabled() && blocked.getReason().contains("Mountain Range"),
                "Mountain Range movement must have an explicit prohibition");
        controller.onHexClicked(range);
        require(controller.getSelectedUnit() == before
                        && controller.getStatusMessage().contains("Mountain Range"),
                "failed movement must retain selection and surface its reason");
    }

    private static void buildRecruitAndTechnologyReasonsAreSpecific() {
        GameController controller = new GameController();
        controller.startNewGame();
        GameState state = controller.getGameState();
        Builder builder = null;
        for (Unit unit : state.getPlayer().getUnits()) {
            if (unit instanceof Builder) { builder = (Builder) unit; break; }
        }
        require(builder != null, "starting Builder missing");
        controller.onHexClicked(builder.getPosition());
        ActionAvailability mine = controller.getBuildTypeAvailability(Constants.BuildingType.IRON_MINE);
        require(!mine.isEnabled() && mine.getReason().contains("IRON MINING"),
                "disabled build action must identify missing technology");

        HexCoordinate mountainRange = null;
        for (Hex hex : state.getMap().getAllHexes()) {
            if (hex.getTerrainType() == Constants.TerrainType.MOUNTAIN_RANGE) {
                hex.explore(true);
                state.getPlayer().expandTerritory(hex.getCoordinate());
                mountainRange = hex.getCoordinate();
                break;
            }
        }
        ActionAvailability site = state.getBuildSiteAvailability(
                mountainRange, Constants.BuildingType.FARM);
        require(!site.isEnabled() && site.getReason().contains("Mountain Range"),
                "buildability must explain Mountain Range prohibition");

        ActionAvailability archer = controller.getMilitaryRecruitmentAvailability(MilitaryUnitType.ARCHER);
        require(!archer.isEnabled() && archer.getReason().contains("Settlement"),
                "military card must state its Town Hall requirement");
        assertReason(controller.getCivilianRecruitmentAvailability(Constants.UnitType.EXPLORER),
                "civilian recruitment");
        assertReason(controller.getLegacyResearchAvailability(Constants.TechnologyType.STORAGE_I),
                "legacy research");
        ActionAvailability phaseTwo = controller.getPhaseTwoResearchAvailability(
                model.technology.TechnologyType.SAILING);
        require(!phaseTwo.isEnabled() && phaseTwo.getReason().contains("SETTLEMENT"),
                "Phase 2 technology must state its Town Hall requirement");
        ActionAvailability upgrade = controller.getTownHallUpgradeAvailability();
        require(!upgrade.isEnabled() && upgrade.getReason().contains("more STONE"),
                "upgrade must state the exact resource shortage");
    }

    private static void tradeExplainsResourceAndStorageFailures() {
        GameController controller = new GameController();
        controller.startNewGame();
        GameState state = controller.getGameState();
        ActionAvailability missingSource = controller.getBazaarTradeAvailability(
                Constants.ResourceType.FOOD, Constants.ResourceType.WOOD, 10);
        require(!missingSource.isEnabled() && missingSource.getReason().contains("Bazaar"),
                "missing trade source must be explicit");

        HexCoordinate position = state.getTownHallPos().findNeighbours().get(0);
        state.getPlayer().addBuilding(new Building(position, Constants.BuildingType.BAZAAR));
        state.getPlayer().addResources(ResourceAmount.of(0, 100, 0, 0));
        ActionAvailability storage = controller.getBazaarTradeAvailability(
                Constants.ResourceType.FOOD, Constants.ResourceType.WOOD, 10);
        require(!storage.isEnabled() && storage.getReason().contains("storage")
                        && storage.getReason().contains("100/100")
                        && storage.getReason().contains("add 5"),
                "failed trade must show current capacity and incoming amount");

        ActionAvailability sameResource = controller.getBazaarTradeAvailability(
                Constants.ResourceType.FOOD, Constants.ResourceType.FOOD, 10);
        require(!sameResource.isEnabled() && sameResource.getReason().contains("different"),
                "same-resource trade must be explained");
    }

    private static void assertReason(ActionAvailability availability, String action) {
        require(availability != null && availability.getReason() != null
                        && !availability.getReason().isBlank(),
                action + " lacks an availability reason");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
