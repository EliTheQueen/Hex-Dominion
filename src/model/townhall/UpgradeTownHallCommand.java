package model.townhall;

import model.ResourceAmount;

public class UpgradeTownHallCommand extends AbstractProductionCommand {

    private final TownHall townHall;
    private final TownHallLevel targetLevel;


    public UpgradeTownHallCommand(TownHall townHall, ResourceAmount cost, int totalTurns) {
        super(cost, totalTurns);
        if (townHall == null) {
            throw new IllegalArgumentException("townHall must not be null");
        }

        if (!townHall.canUpgrade()) {
            throw new IllegalStateException("Town Hall is already at the maximum level");
        }

        this.townHall = townHall;
        this.targetLevel = townHall.getLevel().next();
    }

    public TownHallLevel getTargetLevel() {
        return targetLevel;
    }

    @Override
    protected void executeEffect() {

        if (townHall.getLevel().next() != targetLevel) {
            throw new IllegalStateException("Town Hall level changed while upgrade was in progress");
        }

        townHall.upgrade();
    }
}
