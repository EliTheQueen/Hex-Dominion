package model;

import java.util.ArrayList;
import java.util.List;

public class Building {
    private final HexCoordinate position;
    private final Constants.BuildingType type;
    private final List<Worker> workers;

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

    public boolean addWorker(Worker w) {
        int cap = Constants.WORKER_CAP.getOrDefault(type, 0);
        if (workers.size() >= cap) return false;
        workers.add(w);
        return true;
    }

    public void removeWorker(Worker w) { workers.remove(w); }

    /** Resource yield for this turn, scaled by stationed worker count and tools bonus. */
    public ResourceAmount produce(boolean professionalTools) {
        if (workers.isEmpty() || type == Constants.BuildingType.TOWN_HALL) {
            return ResourceAmount.zero();
        }
        Constants.ResourceType resType = Constants.PRODUCES.get(type);
        if (resType == null) return ResourceAmount.zero();

        int baseRate = Constants.BASE_RATE.getOrDefault(type, 0);
        int workerCount = workers.size();
        double multiplier = professionalTools ? 1.5 : 1.0;
        int total = (int) (baseRate * workerCount * multiplier);

        ResourceAmount result = ResourceAmount.zero();
        result.set(resType, total);
        return result;
    }

    public ResourceAmount getUpkeepCost() {
        return Constants.UPKEEP.getOrDefault(type, ResourceAmount.zero());
    }
}
