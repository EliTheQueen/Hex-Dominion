package model;

import java.util.ArrayList;
import java.util.List;

public class Building {
    private final HexCoordinate position;
    private final Constants.BuildingType type;
    private final List<Worker> workers;

    private int unpaidTurns = 0;
    private boolean ruined = false;

    public Building(HexCoordinate position, Constants.BuildingType type) {
        this.position = position;
        this.type = type;
        this.workers = new ArrayList<>();
    }

    public HexCoordinate getPosition() { return position; }
    public Constants.BuildingType getType() { return type; }
    public List<Worker> getWorkers() { return workers; }
    public int getWorkerCount() { return workers.size(); }
    public int getWorkerCap() { return Constants.WORKER_CAP.getOrDefault(type, 0); }

    public boolean isRuined() { return ruined; }
    public boolean isActive() { return !ruined; }
    public int getUnpaidTurns() { return unpaidTurns; }

    public boolean addWorker(Worker w) {
        if (ruined) return false;
        int cap = Constants.WORKER_CAP.getOrDefault(type, 0);
        if (workers.size() >= cap) return false;
        workers.add(w);
        return true;
    }

    public void removeWorker(Worker w) { workers.remove(w); }

    /** Records that upkeep was paid this turn, resetting the decay counter. */
    public void payUpkeep() { unpaidTurns = 0; }

    /**
     * Records that upkeep could not be paid this turn. After {@code UPKEEP_GRACE_TURNS}
     * consecutive misses the building falls into ruin.
     * @return true if the building became ruined as a result of this miss.
     */
    public boolean missUpkeep() {
        unpaidTurns++;
        if (unpaidTurns >= Constants.UPKEEP_GRACE_TURNS && !ruined) {
            ruin();
            return true;
        }
        return false;
    }

    /** Destroys the building: it stops producing and ejects all stationed workers. */
    public void ruin() {
        ruined = true;
        for (Worker w : new ArrayList<>(workers)) {
            w.unstation();
        }
        workers.clear();
    }

    /** Resource yield for this turn, scaled by stationed worker count and tools bonus. */
    public ResourceAmount produce(boolean professionalTools) {
        if (ruined || workers.isEmpty() || type == Constants.BuildingType.TOWN_HALL) {
            return ResourceAmount.zero();
        }
        Constants.ResourceType resType = Constants.PRODUCES.get(type);
        if (resType == null) return ResourceAmount.zero();

        int baseRate = Constants.BASE_RATE.getOrDefault(type, 0);
        int workerCount = workers.size();
        // Professional tools only boost stone/iron mining (per the tech description).
        boolean boosted = professionalTools
                && (type == Constants.BuildingType.STONE_MINE || type == Constants.BuildingType.IRON_MINE);
        double multiplier = boosted ? 1.5 : 1.0;
        int total = (int) (baseRate * workerCount * multiplier);

        ResourceAmount result = ResourceAmount.zero();
        result.set(resType, total);
        return result;
    }

    public ResourceAmount getUpkeepCost() {
        if (ruined) return ResourceAmount.zero();
        return Constants.UPKEEP.getOrDefault(type, ResourceAmount.zero());
    }
}
