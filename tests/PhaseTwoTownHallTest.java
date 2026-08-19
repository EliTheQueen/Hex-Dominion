import model.GameState;
import model.ResourceAmount;
import model.townhall.CommandStartResult;
import model.townhall.TownHallLevel;
import model.technology.ResearchStartResult;
import model.technology.TechnologyType;

/** Lightweight assertions runnable without third-party test libraries. */
public final class PhaseTwoTownHallTest {
    public static void main(String[] args) {
        upgradeUsesSingleSlotAndTakesThreeTurns();
        cancellationDoesNotRefund();
        researchUsesTheSameSlot();
        System.out.println("PhaseTwoTownHallTest passed");
    }

    private static void upgradeUsesSingleSlotAndTakesThreeTurns() {
        GameState state = fundedState();
        require(state.startTownHallUpgrade() == CommandStartResult.STARTED, "upgrade starts");
        require(state.startTownHallUpgrade() == CommandStartResult.TOWN_HALL_BUSY, "one command only");
        state.endTurn();
        state.endTurn();
        require(state.getTownHall().getLevel() == TownHallLevel.BASE_CAMP, "not early");
        state.endTurn();
        require(state.getTownHall().getLevel() == TownHallLevel.SETTLEMENT, "finishes on third turn");
    }

    private static void cancellationDoesNotRefund() {
        GameState state = fundedState();
        int wood = state.getPlayer().getResources().get(model.Constants.ResourceType.WOOD);
        require(state.startTownHallUpgrade() == CommandStartResult.STARTED, "upgrade starts");
        int afterPayment = state.getPlayer().getResources().get(model.Constants.ResourceType.WOOD);
        require(afterPayment == wood - 50, "cost charged");
        require(state.cancelTownHallCommand(), "cancel succeeds");
        require(state.getPlayer().getResources().get(model.Constants.ResourceType.WOOD) == afterPayment,
                "cancel has no refund");
    }

    private static void researchUsesTheSameSlot() {
        GameState state = fundedState();
        require(state.startTownHallUpgrade() == CommandStartResult.STARTED, "upgrade starts");
        state.endTurn(); state.endTurn(); state.endTurn();
        state.getPlayer().addResources(ResourceAmount.of(0, 100, 0, 100));
        require(state.startPhaseTwoResearch(TechnologyType.SAILING) == ResearchStartResult.STARTED,
                "research starts at settlement");
        require(state.startPhaseTwoResearch(TechnologyType.STEEL_TOOLS) == ResearchStartResult.TOWN_HALL_BUSY,
                "research shares command slot");
        state.endTurn(); state.endTurn(); state.endTurn(); state.endTurn();
        require(state.hasSailing(), "research effect applied");
    }

    private static GameState fundedState() {
        GameState state = new GameState(15, 13);
        state.getPlayer().addResources(ResourceAmount.of(100, 100, 100, 100));
        return state;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
