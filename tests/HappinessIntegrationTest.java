import model.*;
import model.happiness.*;
import model.military.Swordsman;

public final class HappinessIntegrationTest {
    public static void main(String[] args) {
        HappinessTracker tracker = new HappinessTracker();
        HappinessService service = new HappinessService(tracker);
        service.applyEvent(HappinessEventType.MONUMENT_ACTIVATED);
        service.applyEvent(HappinessEventType.MONUMENT_ACTIVATED);
        require(service.getCurrentScore() == 4 && service.getCurrentLevel() == HappinessLevel.GOLDEN_AGE,
                "happiness accumulates into golden age");
        require(HappinessModifiers.applyProductionModifiers(9, 1, HappinessLevel.GOLDEN_AGE) == 9,
                "golden age floors ten percent bonus");
        require(HappinessModifiers.applyProductionModifiers(5, 2, HappinessLevel.UNHAPPY) == 3,
                "unhappy worker penalty");
        require(HappinessModifiers.actionPointPenalty(HappinessLevel.REVOLT) == 1, "revolt AP penalty");

        GameState state = new GameState(15, 13);
        Swordsman guard = new Swordsman(state.getTownHallPos()); state.getPlayer().addUnit(guard);
        state.endTurn(); int afterFirst = state.getHappinessScore();
        state.endTurn(); require(afterFirst == 1 && state.getHappinessScore() == 1,
                "Town Hall garrison reward applies once per unit");
        System.out.println("HappinessIntegrationTest passed");
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
