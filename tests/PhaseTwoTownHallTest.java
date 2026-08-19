import model.GameState;
import model.ResourceAmount;
import model.townhall.CommandStartResult;
import model.townhall.TownHallLevel;
import model.technology.ResearchStartResult;
import model.technology.TechnologyType;
import model.military.MilitaryUnitType;

/** Lightweight assertions runnable without third-party test libraries. */
public final class PhaseTwoTownHallTest {
    public static void main(String[] args) {
        upgradeUsesSingleSlotAndTakesThreeTurns();
        cancellationDoesNotRefund();
        researchUsesTheSameSlot();
        militaryTrainingUsesTheSameSlot();
        legacyResearchUsesTheSameSlot();
        capitalAndDefensiveArchitecture();
        System.out.println("PhaseTwoTownHallTest passed");
    }

    private static void capitalAndDefensiveArchitecture() {
        GameState state = fundedState();
        require(state.startTownHallUpgrade() == CommandStartResult.STARTED, "settlement start");
        state.endTurn(); state.endTurn(); state.endTurn();
        state.getPlayer().addResources(ResourceAmount.of(0, 0, 100, 0));
        require(state.startTownHallUpgrade() == CommandStartResult.STARTED, "capital start");
        for (int i = 0; i < 5; i++) state.endTurn();
        require(state.getTownHall().getLevel() == TownHallLevel.CAPITAL, "capital after five turns");
        require(state.getTownHall().getMaxHp() == 250, "capital does not pre-grant defensive tech HP");
        state.getPlayer().addResources(ResourceAmount.of(0, 0, 100, 0));
        require(state.startPhaseTwoResearch(TechnologyType.DEFENSIVE_ARCHITECTURE) == ResearchStartResult.STARTED,
                "defensive architecture start");
        for (int i = 0; i < 4; i++) state.endTurn();
        require(state.hasDefensiveArchitecture() && state.getTownHall().getDefense() == 30
                && state.getTownHall().getMaxHp() == 350, "defensive architecture effect");
        int walls = 0;
        for (model.HexCoordinate neighbour : state.getTownHallPos().findNeighbours())
            if (state.getMap().containsCoordinate(neighbour)
                    && state.getMap().hasWallBetween(state.getTownHallPos(), neighbour)) walls++;
        require(walls == 6, "automatic Town Hall walls");
    }

    private static void legacyResearchUsesTheSameSlot() {
        GameState state = fundedState();
        require(state.startTownHallUpgrade() == CommandStartResult.STARTED, "upgrade starts");
        require(state.startLegacyResearch(model.Constants.TechnologyType.STORAGE_I)
                == CommandStartResult.TOWN_HALL_BUSY, "legacy research shares slot");
    }

    private static void militaryTrainingUsesTheSameSlot() {
        GameState state = fundedState();
        require(state.startMilitaryTraining(MilitaryUnitType.SWORDSMAN) == CommandStartResult.STARTED,
                "military training starts");
        require(state.startTownHallUpgrade() == CommandStartResult.TOWN_HALL_BUSY, "training owns slot");
        state.endTurn(); state.endTurn();
        require(state.getMilitaryUnitCount() == 1, "military unit created on completion");
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
        require(state.getPlayer().getBuildingAt(state.getTownHallPos()).getMaxHp() == 250,
                "map building synchronized with domain Town Hall");
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
        GameState state = new GameState(15, 13, 102L);
        state.getPlayer().addResources(ResourceAmount.of(100, 100, 100, 100));
        return state;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
