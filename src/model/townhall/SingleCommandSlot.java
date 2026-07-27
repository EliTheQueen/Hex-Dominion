package model.townhall;

//مالک command فعال است و تضمین می‌کند Town Hall هم‌زمان بیشتر از یک کار نداشته باشه
public class SingleCommandSlot {

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
        if (!isBusy() || activeCommand == null) {
            return;
        }

        activeCommand.advanceOneTurn();

        if (activeCommand.isCompleted()) {
            activeCommand.complete();
            activeCommand = null;
        }
    }

    public boolean cancel() {
        if (!isBusy()) {
            return false;
        }

        activeCommand.cancel();
        activeCommand = null;
        return true;
    }
}
