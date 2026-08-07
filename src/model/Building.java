package model;

import java.util.ArrayList;
import java.util.List;

public class Building {
    private final HexCoordinate position;
    private final Constants.BuildingType type;
    private final List<Worker> workers;
    private final int maxHp;
    private int currentHp;

    private int unpaidTurns = 0;
    private boolean ruined = false;

    public Building(HexCoordinate position, Constants.BuildingType type) {
        this.position = position;
        this.type = type;
        this.workers = new ArrayList<>();

        if (type == Constants.BuildingType.TOWN_HALL) {
            this.maxHp = 200;
        } else {
            this.maxHp = 100;
        }

        this.currentHp = maxHp;
    }

    public HexCoordinate getPosition() { return position; }
    public Constants.BuildingType getType() { return type; }
    public List<Worker> getWorkers() { return workers; }
    public int getWorkerCount() { return workers.size(); }
    public int getWorkerCap() { return Constants.WORKER_CAP.getOrDefault(type, 0); }

    public int getMaxHp() {
        return maxHp;
    }

    public int getCurrentHp() {
        return currentHp;
    }

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

    public void payUpkeep() { unpaidTurns = 0; }

    public boolean missUpkeep() {
        unpaidTurns++;
        if (unpaidTurns >= Constants.UPKEEP_GRACE_TURNS && !ruined) {
            ruin();
            return true;
        }
        return false;
    }
    /*
    یک نکته‌ی فنیِ مهم:
     روی new ArrayList<>(workers) حلقه می‌زند، نه مستقیم روی workers.
      چرا؟ چون w.unstation()
       داخل خودش stationedAt.removeWorker(this) را صدا می‌زند که از همین لیست workers حذف می‌کند.
      اگر همزمان روی یک لیست حلقه بزنی و از آن حذف کنی، خطای `ConcurrentModificationException` می‌گیری.
       ساختنِ یک کپی، روی نسخه‌ی کپی حلقه می‌زند و حذف روی نسخه‌ی
        اصلی انجام می‌شود — امن. این یک تله‌ی کلاسیک جاواست؛ خوب در ذهن نگه‌دار.
     */
    public void ruin() {
        if (ruined) {
            return;
        }

        ruined = true;

        for (Worker w : new ArrayList<>(workers)) {
            w.unstation();
        }

        workers.clear();
    }

    public ResourceAmount produce(boolean professionalTools) {
        if (ruined || workers.isEmpty() || type == Constants.BuildingType.TOWN_HALL) {
            return ResourceAmount.zero();
        }
        Constants.ResourceType resType = Constants.PRODUCES.get(type);
        if (resType == null) return ResourceAmount.zero();

        int baseRate = Constants.BASE_RATE.getOrDefault(type, 0);
        int workerCount = workers.size();

        //only for iron and stone
        boolean boosted = professionalTools
                && (type == Constants.BuildingType.STONE_MINE || type == Constants.BuildingType.IRON_MINE);
        double multiplier = boosted ? 1.5 : 1.0;
        int total = (int) (baseRate * workerCount * multiplier);

        ResourceAmount result = ResourceAmount.zero();
        result.set(resType, total);
        return result;
    }

    public ResourceAmount getUpkeepCost() {
        if (ruined)
            return ResourceAmount.zero();
        return Constants.UPKEEP.getOrDefault(type, ResourceAmount.zero());
    }

    public void takeDamage(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("damage must not be negative");
        }

        currentHp = Math.max(0, currentHp - amount);

        if (currentHp == 0) {
            ruin();
        }
    }
}
