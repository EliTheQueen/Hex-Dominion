package model.tribe.mission;

import model.tribe.Tribe;

import java.util.UUID;

//این کلاس Aggregate اصلی Mission است.
public class TribeMission implements java.io.Serializable {

    private final String id;
    private final String title;
    private final String description;

    private final Tribe tribe;

    private final TribeMissionObjective objective;
    private final TribeMissionReward reward;

    private final int totalTurns;

    private int remainingTurns;

    private TribeMissionStatus status;

    public TribeMission(
            String title,
            String description,
            Tribe tribe,
            TribeMissionObjective objective,
            TribeMissionReward reward,
            int totalTurns
    ) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("title must not be blank");
        }

        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("description must not be blank");
        }

        if (tribe == null || objective == null || reward == null) {
            throw new IllegalArgumentException("dependencies must not be null");
        }

        if (totalTurns <= 0) {
            throw new IllegalArgumentException("totalTurns must be greater than zero");
        }

        this.id = UUID.randomUUID().toString();
        this.title = title.trim();
        this.description = description.trim();
        this.tribe = tribe;
        this.objective = objective;
        this.reward = reward;
        this.totalTurns = totalTurns;
        this.remainingTurns = totalTurns;
        this.status = TribeMissionStatus.AVAILABLE;
    }

    public void accept() {
        if (status != TribeMissionStatus.AVAILABLE) {
            throw new IllegalStateException("only available missions can be accepted");
        }

        status = TribeMissionStatus.ACTIVE;
    }

    public void advanceOneTurn() {
        if (status != TribeMissionStatus.ACTIVE) {
            return;
        }

        if (objective.isCompleted()) {
            status = TribeMissionStatus.READY_TO_TURN_IN;
            return;
        }

        remainingTurns--;

        if (remainingTurns <= 0) {
            remainingTurns = 0;
            status = TribeMissionStatus.FAILED;
        }
    }

    public void refreshCompletionState() {
        if (status == TribeMissionStatus.ACTIVE && objective.isCompleted()) {
            status = TribeMissionStatus.READY_TO_TURN_IN;
        }
    }

    public void complete() {
        if (status != TribeMissionStatus.READY_TO_TURN_IN) {
            throw new IllegalStateException("mission is not ready to claim");
        }

        status = TribeMissionStatus.COMPLETED;
    }

    public void cancel() {
        if (status != TribeMissionStatus.ACTIVE && status != TribeMissionStatus.READY_TO_TURN_IN) {
            return;
        }

        status = TribeMissionStatus.CANCELLED;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Tribe getTribe() {
        return tribe;
    }

    public TribeMissionObjective getObjective() {
        return objective;
    }

    public TribeMissionReward getReward() {
        return reward;
    }

    public int getTotalTurns() {
        return totalTurns;
    }

    public int getRemainingTurns() {
        return remainingTurns;
    }

    public TribeMissionStatus getStatus() {
        return status;
    }
}
