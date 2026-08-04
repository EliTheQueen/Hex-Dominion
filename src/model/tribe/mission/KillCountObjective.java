package model.tribe.mission;

public class KillCountObjective implements TribeMissionObjective {

    private final int requiredKills;
    private int currentKills;

    public KillCountObjective(int requiredKills) {
        if (requiredKills <= 0) {
            throw new IllegalArgumentException("requiredKills must be greater than zero");
        }

        this.requiredKills = requiredKills;
    }

    public void recordKill() {
        if (!isCompleted()) {
            currentKills++;
        }
    }

    @Override
    public boolean isCompleted() {
        return currentKills >= requiredKills;
    }

    @Override
    public String getDescription() {
        return "Defeat " + requiredKills + " enemy units";
    }

    public int getRequiredKills() {
        return requiredKills;
    }

    public int getCurrentKills() {
        return currentKills;
    }
}