import controller.GameController;
import model.Constants;
import model.GameState;
import model.Hex;
import model.tribe.Tribe;
import model.tribe.TribeAction;
import model.tribe.TribeActionAvailability;
import model.tribe.TribeType;
import model.tribe.mission.MissionActionResult;

public final class TribeVisibilityTest {
    public static void main(String[] args) {
        undiscoveredTribesAreAbsentFromUiProjection();
        discoveryAndCurrentVisibilityAreDistinct();
        campClickRoutesToInteraction();
        actionReasonsAndFormValidationAreStateAware();
        storageFullTurnInHasExactDisabledReason();
        System.out.println("TribeVisibilityTest passed");
    }

    private static void undiscoveredTribesAreAbsentFromUiProjection() {
        GameState state = new GameState(15, 13);
        for (Tribe tribe : state.getTribes()) {
            Hex camp = state.getMap().getHex(tribe.getCampCoordinate());
            camp.setVisible(false);
        }
        require(state.getTribes().size() == 5, "authoritative model retains all tribes");
        require(state.getDiscoveredTribes().isEmpty(), "undiscovered tribes are absent from UI projection");
        require(state.getVisibleTribes().isEmpty(), "undiscovered tribes are not visible");
    }

    private static void discoveryAndCurrentVisibilityAreDistinct() {
        GameState state = new GameState(15, 13);
        Tribe farmer = find(state, TribeType.FARMER);
        Hex camp = state.getMap().getHex(farmer.getCampCoordinate());
        farmer.discover();
        camp.setVisible(false);
        require(state.getDiscoveredTribes().contains(farmer), "discovered tribe remains known");
        require(!state.getVisibleTribes().contains(farmer), "known tribe outside sight is not currently visible");
        require(state.getVisibleTribeAt(farmer.getCampCoordinate()) == null,
                "out-of-sight camp cannot be clicked as current information");
        TribeActionAvailability hiddenGift = state.getTribeActionAvailability(farmer, TribeAction.GIFT);
        require(!hiddenGift.isAvailable() && hiddenGift.getReason().contains("outside current vision"),
                "out-of-sight interactions expose a visibility reason");
        require(state.getTribeActionAvailability(farmer, TribeAction.VIEW_ALLIANCE_BENEFIT).isAvailable(),
                "static alliance reward remains viewable after discovery");

        camp.setVisible(true);
        require(state.getVisibleTribes().contains(farmer), "visible camp enters current projection");
        require(state.getVisibleTribeAt(farmer.getCampCoordinate()) == farmer,
                "visible camp resolves to its interaction target");
    }

    private static void campClickRoutesToInteraction() {
        GameController controller = new GameController();
        controller.startNewGame();
        Tribe merchant = find(controller.getGameState(), TribeType.MERCHANT);
        merchant.discover();
        controller.getGameState().getMap().getHex(merchant.getCampCoordinate()).setVisible(true);
        controller.onHexClicked(merchant.getCampCoordinate());
        require(controller.getLastOpenedTribe() == merchant, "camp click opens the matching tribe interaction");
        require(merchant.getCampCoordinate().equals(controller.getSelectedHex()),
                "camp click retains selected map coordinate");
    }

    private static void actionReasonsAndFormValidationAreStateAware() {
        GameState state = new GameState(15, 13);
        Tribe merchant = visible(state, TribeType.MERCHANT);
        merchant.getRelation().setScore(20);
        for (TribeAction action : TribeAction.values()) {
            TribeActionAvailability availability = state.getTribeActionAvailability(merchant, action);
            require(availability.getReason() != null && !availability.getReason().isBlank(),
                    action + " always has a tooltip reason");
        }
        require(!state.getGiftAvailability(merchant, Constants.ResourceType.FOOD, 1).isAvailable(),
                "gift selector rejects amount with zero relation gain");
        require(state.getGiftAvailability(merchant, Constants.ResourceType.FOOD, 10).isAvailable(),
                "gift selector accepts a funded exact increment");
        require(!state.getTradeAvailability(merchant, Constants.ResourceType.WOOD,
                        Constants.ResourceType.WOOD, 10).isAvailable(),
                "trade selector rejects identical resources");
        require(state.getTradeAvailability(merchant, Constants.ResourceType.WOOD,
                        Constants.ResourceType.FOOD, 10).isAvailable(),
                "trade selector accepts valid sell/buy/amount parameters");
    }

    private static void storageFullTurnInHasExactDisabledReason() {
        GameState state = new GameState(15, 13);
        Tribe farmer = visible(state, TribeType.FARMER);
        farmer.getRelation().setScore(20);
        require(state.requestMission(farmer) == MissionActionResult.SUCCESS, "Farmer mission accepts");
        state.getMission(farmer).refreshCompletionState();
        state.getPlayer().addResources(model.ResourceAmount.of(100, 100, 100, 100));
        TribeActionAvailability availability = state.getTribeActionAvailability(farmer,
                TribeAction.TURN_IN_MISSION);
        require(!availability.isAvailable() && availability.getReason().contains("Insufficient storage"),
                "full storage is an explicit turn-in disabled reason");
    }

    private static Tribe visible(GameState state, TribeType type) {
        Tribe tribe = find(state, type);
        tribe.discover();
        state.getMap().getHex(tribe.getCampCoordinate()).setVisible(true);
        return tribe;
    }

    private static Tribe find(GameState state, TribeType type) {
        for (Tribe tribe : state.getTribes()) if (tribe.getType() == type) return tribe;
        throw new AssertionError("missing " + type);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
