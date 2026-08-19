import java.util.Random;

import model.GameState;
import model.disaster.DisasterGenerator;
import model.disaster.DisasterOccurrencePolicy;
import model.disaster.DisasterOriginSelector;
import model.disaster.DisasterSelector;

/** Deterministic large-map/high-turn guardrail; generous bound avoids CI noise. */
public final class PerformanceScenarioTest {
    public static void main(String[] args) {
        DisasterGenerator none = new DisasterGenerator(new DisasterOccurrencePolicy(0),
                new DisasterSelector(), new DisasterOriginSelector());
        long started = System.nanoTime();
        GameState state = new GameState(41, 35, new Random(8801L), none);
        int firstTurn = state.getCurrentTurn();
        for (int i = 0; i < 250; i++) state.endTurn();
        long elapsedMillis = (System.nanoTime() - started) / 1_000_000L;
        require(state.getCurrentTurn() == firstTurn + 250,
                "high-turn simulation must complete every turn");
        require(state.getMap().getAllHexes().size() == 41 * 35,
                "large map must retain every generated hex");
        require(elapsedMillis < 20_000L,
                "large-map 250-turn scenario exceeded 20 seconds: " + elapsedMillis + "ms");
        System.out.println("PerformanceScenarioTest passed (" + elapsedMillis + " ms)");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
