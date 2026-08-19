package model.turn;

import java.util.Objects;
import java.util.function.Consumer;

/** Executes every turn phase exactly once in the declared order. */
public final class TurnCoordinator {
    public void execute(Consumer<TurnPhase> phaseProcessor) {
        Objects.requireNonNull(phaseProcessor, "phase processor is required");
        for (TurnPhase phase : TurnPhase.values()) phaseProcessor.accept(phase);
    }
}
