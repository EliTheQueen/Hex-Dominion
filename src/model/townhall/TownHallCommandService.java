package model.townhall;

import model.ResourceAmount;
import model.ResourceStorage;

//بین منابع بازیکن، Town Hall و slot هماهنگی ایجاد می‌کند.
public class TownHallCommandService {
    private final TownHall townHall;
    private final ResourceStorage resourceStorage;

    public TownHallCommandService(TownHall townHall, ResourceStorage resourceStorage) {
        if (townHall == null || resourceStorage == null) {
            throw new IllegalArgumentException("Town Hall or Resource Storage cannot be null");
        }

        this.townHall = townHall;
        this.resourceStorage = resourceStorage;
    }

    public CommandStartResult startCommand(ProductionCommand command) {
        if (command == null) {
            return CommandStartResult.INVALID_COMMAND;
        }

        SingleCommandSlot commandSlot = townHall.getCommandSlot();

        if (commandSlot.isBusy()) {
            return CommandStartResult.TOWN_HALL_BUSY;
        }

        ResourceAmount cost = command.getCost();

        if (!resourceStorage.canAfford(cost)) {
            return CommandStartResult.INSUFFICIENT_RESOURCES;
        }

        boolean paymentSuccessful = resourceStorage.spend(cost);

        if (!paymentSuccessful) {
            return CommandStartResult.INSUFFICIENT_RESOURCES;
        }

        boolean commandStarted = commandSlot.start(command);

        if (!commandStarted) {
            resourceStorage.addResources(cost);
            return CommandStartResult.TOWN_HALL_BUSY;
        }

        return CommandStartResult.STARTED;
    }

    public boolean cancelActiveCommand() {
        return townHall.getCommandSlot().cancel();
    }

    public void advanceOneTurn() {
        townHall.getCommandSlot().advanceOneTurn();
    }
}
