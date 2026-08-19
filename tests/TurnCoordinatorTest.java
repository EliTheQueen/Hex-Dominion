import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import model.turn.TurnCoordinator;
import model.turn.TurnPhase;

/** The phase coordinator is the single executable source of turn ordering. */
public final class TurnCoordinatorTest {
    public static void main(String[] args) {
        List<TurnPhase> observed = new ArrayList<>();
        new TurnCoordinator().execute(observed::add);
        require(observed.equals(Arrays.asList(TurnPhase.values())),
                "every phase must run exactly once in declared order");
        require(observed.get(0) == TurnPhase.BEGINNING_OF_TURN,
                "disaster/start effects must be the first phase");
        require(observed.indexOf(TurnPhase.PRODUCTION_AND_UPKEEP)
                        < observed.indexOf(TurnPhase.CALENDAR_AND_TRIBES),
                "production must precede calendar advancement and tribe processing");
        require(observed.get(observed.size() - 1) == TurnPhase.END_CONDITIONS,
                "loss evaluation must close the transaction");
        System.out.println("TurnCoordinatorTest passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
