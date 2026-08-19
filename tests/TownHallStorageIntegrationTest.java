import java.nio.file.Files;

import model.Constants;
import model.GameState;
import model.ResourceAmount;
import model.save.SaveManager;
import model.save.SaveSlot;
import model.townhall.CommandStartResult;
import model.townhall.TownHallLevel;

/** Exercises both upgrades and persistence through the authoritative GameState flow. */
public final class TownHallStorageIntegrationTest {
    public static void main(String[] args) throws Exception {
        GameState state = new GameState(15, 13, 701L);
        assertCapacity(state, 100);

        state.getPlayer().addResources(ResourceAmount.of(0, 20, 40, 0));
        require(state.startTownHallUpgrade() == CommandStartResult.STARTED,
                "Settlement upgrade must start through the shared command slot");
        advance(state, 3);
        require(state.getTownHall().getLevel() == TownHallLevel.SETTLEMENT,
                "Settlement upgrade must complete");
        assertCapacity(state, 200);

        state.getPlayer().addResources(ResourceAmount.of(0, 0, 200, 200));
        require(state.startTownHallUpgrade() == CommandStartResult.STARTED,
                "Capital upgrade must start through the shared command slot");
        advance(state, 5);
        require(state.getTownHall().getLevel() == TownHallLevel.CAPITAL,
                "Capital upgrade must complete");
        assertCapacity(state, 350);

        SaveManager saves = new SaveManager(Files.createTempDirectory("hex-storage-round-trip"));
        require(saves.save(SaveSlot.MANUAL_1, "Capital storage", state),
                "upgraded state must save");
        GameState loaded = saves.load(SaveSlot.MANUAL_1).getGameState();
        require(loaded != null && loaded.getTownHall().getLevel() == TownHallLevel.CAPITAL,
                "Town Hall level must survive save/load");
        assertCapacity(loaded, 350);
        System.out.println("TownHallStorageIntegrationTest passed");
    }

    private static void advance(GameState state, int turns) {
        for (int i = 0; i < turns; i++) state.endTurn();
    }

    private static void assertCapacity(GameState state, int expected) {
        require(state.getTownHall().getStorageCapacity() == expected,
                "Town Hall capacity mismatch");
        for (Constants.ResourceType type : Constants.ResourceType.values()) {
            require(state.getPlayer().getResources().getCap(type) == expected,
                    type + " ResourceStorage capacity mismatch");
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
