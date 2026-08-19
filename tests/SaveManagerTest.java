import java.nio.file.Files;
import java.nio.file.Path;

import model.GameState;
import model.ResourceAmount;
import model.save.*;

public final class SaveManagerTest {
    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("hex-dominion-save-test");
        SaveManager manager = new SaveManager(directory);
        GameState original = new GameState(15, 13);
        original.getPlayer().addResources(ResourceAmount.of(5, 7, 9, 11));
        require(manager.save(SaveSlot.MANUAL_1, "Round trip", original), "atomic save");
        SavePreview preview = manager.preview(SaveSlot.MANUAL_1);
        require(preview.isPresent() && !preview.isCorrupted(), "valid preview");
        SaveLoadResult loaded = manager.load(SaveSlot.MANUAL_1);
        require(loaded.isSuccessful(), loaded.getMessage());
        require(loaded.getGameState().getCurrentTurn() == original.getCurrentTurn(), "turn preserved");
        require(manager.save(SaveSlot.MANUAL_2, "Second generation", loaded.getGameState()),
                "save-load-save round trip");

        Files.write(directory.resolve(SaveSlot.MANUAL_3.getFileName()), new byte[] {1, 2, 3});
        require(manager.preview(SaveSlot.MANUAL_3).isCorrupted(), "corruption preview");
        GameState live = original;
        SaveLoadResult corrupt = manager.load(SaveSlot.MANUAL_3);
        require(!corrupt.isSuccessful() && live == original, "failed load cannot replace live state");
        System.out.println("SaveManagerTest passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
