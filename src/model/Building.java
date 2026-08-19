package model;

import java.util.ArrayList;
import java.util.List;
import model.townhall.TownHall;
import model.townhall.TownHallLevel;

public class Building implements java.io.Serializable {
    private final HexCoordinate position;
    private final Constants.BuildingType type;
    private final List<Worker> workers;
    private int maxHp;
    private int currentHp;

    /*
     * A TOWN_HALL Building is only the map/read-model projection.  The Phase 2
     * TownHall aggregate owns its level and health once this reference is bound.
     */
    private TownHall authoritativeTownHall;

    private int unpaidTurns = 0;
    private boolean ruined = false;
    private int productionBlockedTurns = 0;

    private int lastTradeTurn = -1;

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

    public int getMaxHp() { return authoritativeTownHall == null
            ? maxHp : authoritativeTownHall.getMaxHp(); }

    public int getCurrentHp() { return authoritativeTownHall == null
            ? currentHp : authoritativeTownHall.getCurrentHp(); }

    public boolean isRuined() { return ruined
            || authoritativeTownHall != null && authoritativeTownHall.isDestroyed(); }
    public boolean isActive() { return !isRuined(); }
    public int getUnpaidTurns() { return unpaidTurns; }

    public boolean addWorker(Worker w) {
        if (isRuined()) return false;
        int cap = Constants.WORKER_CAP.getOrDefault(type, 0);
        if (workers.size() >= cap) return false;
        workers.add(w);
        return true;
    }

    public void removeWorker(Worker w) { workers.remove(w); }

    public void payUpkeep() { unpaidTurns = 0; }

    public boolean missUpkeep() {
        unpaidTurns++;
        if (unpaidTurns >= Constants.UPKEEP_GRACE_TURNS && !isRuined()) {
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
    void ruin() {
        if (ruined) {
            return;
        }

        ruined = true;

        if (authoritativeTownHall != null && !authoritativeTownHall.isDestroyed()) {
            authoritativeTownHall.takeDamage(authoritativeTownHall.getCurrentHp());
        }

        for (Worker w : new ArrayList<>(workers)) {
            w.unstation();
        }

        workers.clear();
    }

    public ResourceAmount produce(boolean professionalTools) {
        if (isRuined()
                || productionBlockedTurns > 0
                || workers.isEmpty()
                || type == Constants.BuildingType.TOWN_HALL) {
            return ResourceAmount.zero();
        }

        Constants.ResourceType resType = Constants.PRODUCES.get(type);
        if (resType == null)
            return ResourceAmount.zero();

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
        if (isRuined())
            return ResourceAmount.zero();
        return Constants.UPKEEP.getOrDefault(type, ResourceAmount.zero());
    }

    public void takeDamage(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("damage must not be negative");
        }

        if (authoritativeTownHall != null) {
            authoritativeTownHall.takeDamage(amount);
        } else {
            currentHp = Math.max(0, currentHp - amount);
        }

        if (getCurrentHp() == 0) {
            ruin();
        }
    }

    public void synchronizeHealth(int hp, int maximum) {
        if (maximum <= 0 || hp < 0 || hp > maximum) throw new IllegalArgumentException("invalid health");
        if (authoritativeTownHall != null) {
            if (hp != authoritativeTownHall.getCurrentHp()
                    || maximum != authoritativeTownHall.getMaxHp()) {
                throw new IllegalStateException("Town Hall projection cannot overwrite authoritative state");
            }
            return;
        }
        this.maxHp = maximum; this.currentHp = hp;
        if (hp == 0) ruin();
    }

    public void bindTownHallProjection(TownHall townHall) {
        if (type != Constants.BuildingType.TOWN_HALL || townHall == null) {
            throw new IllegalArgumentException("only a Town Hall building can bind the Town Hall aggregate");
        }
        authoritativeTownHall = townHall;
    }

    public TownHallLevel getTownHallLevel() {
        return authoritativeTownHall == null ? null : authoritativeTownHall.getLevel();
    }

    public void blockProductionForTurns(int turns) {
        if (turns < 0) {
            throw new IllegalArgumentException("turns must not be negative");
        }

        productionBlockedTurns = Math.max(productionBlockedTurns, turns);
    }

    public boolean isProductionBlocked() {
        return productionBlockedTurns > 0;
    }

    public void advanceTurnStatus() {
        if (productionBlockedTurns > 0) {
            productionBlockedTurns--;
        }
    }

    public boolean canTradeAt(int turn) { return lastTradeTurn != turn; }
    public void markTradedAt(int turn) { lastTradeTurn = turn; }
    public int getLastTradeTurn() { return lastTradeTurn; }
}
