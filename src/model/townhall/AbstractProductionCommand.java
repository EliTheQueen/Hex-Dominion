package model.townhall;

import model.ResourceAmount;

//منطق زمانی مشترک، لغو و جلوگیری از اجرای چندباره‌ی اثر را نگه می‌دارد
public abstract class AbstractProductionCommand implements ProductionCommand {

    private final ResourceAmount cost;
    private final int totalTurns;
    private int remainingTurns;
    private boolean cancelled;
    private boolean effectApplied;

    protected  AbstractProductionCommand(ResourceAmount cost, int totalTurns) {
        if (cost == null) {
            throw new IllegalArgumentException("cost cannot be null");
        }
        if (totalTurns <= 0) {
            throw new IllegalArgumentException("totalTurns cannot be negative");
        }
        this.cost = cost.copy();
        this.totalTurns = totalTurns;
        this.remainingTurns = totalTurns;
    }

    @Override
    public ResourceAmount getCost() {
        return cost.copy();
    }

    @Override
    public int getTotalTurns() {
        return totalTurns;
    }

    @Override
    public int getRemainingTurns() {
        return remainingTurns;
    }

    @Override
    public void advanceOneTurn() {
        if (remainingTurns > 0 && !cancelled) {
            remainingTurns--;
        }
    }

    @Override
    public boolean isCompleted() {
        return !cancelled && remainingTurns == 0;
    }

    @Override
    public final void complete() {
        if (!isCompleted() || effectApplied) {
            return;
        }

        executeEffect();
        effectApplied = true;
    }

    @Override
    public void cancel() {
        if (!effectApplied) {
            cancelled = true;
        }
    }

    public boolean isCancelled() {
        return cancelled;
    }

    protected abstract void executeEffect();
}
