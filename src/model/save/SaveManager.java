package model.save;

import model.GameState;

import java.io.*;
import java.nio.file.*;
import java.time.Instant;

/** Versioned, atomic slot persistence. A failed load never exposes a partial GameState. */
public final class SaveManager {
    public static final int SAVE_VERSION = 2;
    private static final String MAGIC = "HEX-DOMINION";
    private final Path directory;

    public SaveManager(Path directory) { this.directory = directory; }

    public boolean save(SaveSlot slot, String name, GameState state) {
        if (slot == null || state == null || state.isGameOver()) return false;
        try {
            Files.createDirectories(directory);
            Path target = path(slot);
            Path temporary = Files.createTempFile(directory, slot.getFileName(), ".tmp");
            try {
                SaveEnvelope envelope = new SaveEnvelope(name, state);
                try (ObjectOutputStream output = new ObjectOutputStream(
                        new BufferedOutputStream(Files.newOutputStream(temporary)))) {
                    output.writeObject(envelope);
                }
                // Validate the complete temporary file before it can replace a good save.
                readEnvelope(temporary);
                try {
                    Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException ex) {
                    Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
                }
                return true;
            } finally {
                Files.deleteIfExists(temporary);
            }
        } catch (IOException | ClassNotFoundException | RuntimeException ex) {
            return false;
        }
    }

    public SaveLoadResult load(SaveSlot slot) {
        if (slot == null || !Files.isRegularFile(path(slot))) return SaveLoadResult.failure("Save slot is empty");
        try {
            SaveEnvelope envelope = readEnvelope(path(slot));
            validateState(envelope.state);
            return SaveLoadResult.success(envelope.state);
        } catch (IOException | ClassNotFoundException | RuntimeException ex) {
            return SaveLoadResult.failure("Save is corrupted or incompatible");
        }
    }

    public SavePreview preview(SaveSlot slot) {
        if (!Files.isRegularFile(path(slot))) return new SavePreview(slot, false, false, "Empty", 0, "", 0, null);
        try {
            SaveEnvelope e = readEnvelope(path(slot));
            validateState(e.state);
            return new SavePreview(slot, true, false, e.name, e.turn, e.season, e.townHallLevel, e.savedAt);
        } catch (Exception ex) {
            return new SavePreview(slot, true, true, "Corrupted save", 0, "", 0, null);
        }
    }

    public boolean delete(SaveSlot slot) {
        try { return Files.deleteIfExists(path(slot)); }
        catch (IOException ex) { return false; }
    }

    private Path path(SaveSlot slot) { return directory.resolve(slot.getFileName()); }

    private SaveEnvelope readEnvelope(Path file) throws IOException, ClassNotFoundException {
        try (ObjectInputStream input = new ObjectInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
            Object value = input.readObject();
            if (!(value instanceof SaveEnvelope)) throw new InvalidObjectException("Not a Hex Dominion save");
            SaveEnvelope envelope = (SaveEnvelope) value;
            if (!MAGIC.equals(envelope.magic) || envelope.version != SAVE_VERSION)
                throw new InvalidObjectException("Unsupported save version");
            return envelope;
        }
    }

    private void validateState(GameState state) throws InvalidObjectException {
        if (state == null || state.getMap() == null || state.getPlayer() == null
                || state.getCurrentTurn() < 1 || state.getTownHall() == null)
            throw new InvalidObjectException("Invalid game state");
    }

    private static final class SaveEnvelope implements Serializable {
        private final String magic = MAGIC;
        private final int version = SAVE_VERSION;
        private final String name;
        private final Instant savedAt;
        private final int turn;
        private final String season;
        private final int townHallLevel;
        private final GameState state;
        private SaveEnvelope(String name, GameState state) {
            this.name = name == null || name.trim().isEmpty() ? "Hex Dominion" : name.trim();
            this.savedAt = Instant.now();
            this.turn = state.getCurrentTurn();
            this.season = state.getSeasonCycle().getCurrentSeason().name();
            this.townHallLevel = state.getTownHall().getLevel().getLevelNumber();
            this.state = state;
        }
    }
}
