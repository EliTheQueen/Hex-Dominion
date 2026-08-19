package model.townhall;

//مالک command فعال است و تضمین می‌کند Town Hall هم‌زمان بیشتر از یک کار نداشته باشه
public class SingleCommandSlot implements java.io.Serializable {

    private ProductionCommand activeCommand;

    public boolean isBusy() {
        return activeCommand != null;
    }

    public  ProductionCommand getActiveCommand() {
        return activeCommand;
    }

    public boolean start(ProductionCommand command) {
        if (command == null || isBusy()) {
            return false;
        }

        activeCommand = command;
        try {
            command.onStarted();
            return true;
        }
        catch (Exception e) {
            activeCommand = null;
            throw e;
        }
    }

    public void advanceOneTurn() {
        if (!isBusy()) {
            return;
        }

        activeCommand.advanceOneTurn();

        if (!activeCommand.isCompleted()) {
            return;
        }

        ProductionCommand completedCommand = activeCommand;

        completedCommand.complete();

        if (activeCommand == completedCommand) {
            activeCommand = null;
        }
    }

    public boolean cancel() {
        if (!isBusy()) {
            return false;
        }

        ProductionCommand cancelledCommand = activeCommand;

        cancelledCommand.cancel();

        if (activeCommand == cancelledCommand) {
            activeCommand = null;
        }
        return true;
    }
}
