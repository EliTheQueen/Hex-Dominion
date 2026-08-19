package model.townhall;

import model.ResourceAmount;

public interface ProductionCommand extends java.io.Serializable {

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
