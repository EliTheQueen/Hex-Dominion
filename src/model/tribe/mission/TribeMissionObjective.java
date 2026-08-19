package model.tribe.mission;

public interface TribeMissionObjective extends java.io.Serializable {

    boolean isCompleted();

    String getDescription();

    default boolean fulfill(model.Player player) { return isCompleted(); }
}
