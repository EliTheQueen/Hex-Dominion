package model.townhall;

import model.ResourceAmount;

public interface ProductionCommand {

    ResourceAmount getCost();

    int getTotalTurns();

    int getRemainingTurns();

    void advanceOneTurn();

    boolean isCompleted();

    void complete();

    void cancel();

    void onStarted();

    default void onCancelled() {
    }
}