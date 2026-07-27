package model;

public class Worker extends Unit {
    private Building stationedAt;

    public Worker(HexCoordinate pos) {
        super(pos, Constants.UnitType.WORKER);
        this.stationedAt = null;
    }

    public Building getStationedAt() { return stationedAt; }
    public boolean isStationed() { return stationedAt != null; }

    public boolean station(Building building) {
        if (building == null) return false;
        if (building.getWorkerCount() >= building.getWorkerCap()) return false;
        if (!spendAP(Constants.WORKER_STATION_AP_COST)) return false;
        if (stationedAt != null) stationedAt.removeWorker(this);
        if (!building.addWorker(this)) {
            return false;
        }
        stationedAt = building;
        state = Constants.UnitState.STATIONED;
        return true;
    }

    public void unstation() {
        if (stationedAt != null) {
            stationedAt.removeWorker(this);
            stationedAt = null;
            state = Constants.UnitState.IDLE;
        }
    }
}
