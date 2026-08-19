package model.save;

import model.GameState;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;

/** Versioned, validated and atomic slot persistence. */
public final class SaveManager {
    public static final int SAVE_VERSION = 3;
    private static final String MAGIC = "HEX-DOMINION";

    private final Path directory;
    private final SaveMigrationRegistry migrations = new SaveMigrationRegistry();

    public SaveManager(Path directory) {
        if (directory == null) throw new IllegalArgumentException("save directory is required");
        this.directory = directory;
    }

    /** Compatibility facade for existing callers. */
    public boolean save(SaveSlot slot, String name, GameState state) {
        return saveWithResult(slot, name, state).isSuccessful();
    }

    public SaveWriteResult saveWithResult(SaveSlot slot, String name, GameState state) {
        if (slot == null || state == null) {
            return SaveWriteResult.failure("A save slot and active game are required");
        }
        SaveAvailability availability = state.getSaveAvailability();
        if (!availability.isEnabled()) {
            return SaveWriteResult.failure(availability.getReason());
        }

        Path temporary = null;
        try {
            state.validatePersistentState();
            Files.createDirectories(directory);
            Path target = path(slot);
            temporary = Files.createTempFile(directory, slot.getFileName(), ".tmp");
            SaveEnvelope envelope = SaveEnvelope.current(name, state);
            try (ObjectOutputStream output = new ObjectOutputStream(
                    new BufferedOutputStream(Files.newOutputStream(temporary)))) {
                output.writeObject(envelope);
            }

            // Read, migrate, checksum, normalize and deeply validate the whole
            // temporary graph before it can replace a healthy target.
            restore(readEnvelope(temporary));
            replacePreservingHealthyTarget(temporary, target);
            temporary = null;
            return SaveWriteResult.success("Saved to " + slot.getDisplayName());
        } catch (IOException | ClassNotFoundException | RuntimeException ex) {
            return SaveWriteResult.failure(
                    "Save failed; the previous " + slot.getDisplayName() + " was preserved");
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); }
                catch (IOException ignored) { }
            }
        }
    }

    public SaveLoadResult load(SaveSlot slot) {
        if (slot == null || !Files.isRegularFile(path(slot))) {
            return SaveLoadResult.failure("Save slot is empty");
        }
        try {
            GameState candidate = restore(readEnvelope(path(slot)));
            return SaveLoadResult.success(candidate);
        } catch (IOException | ClassNotFoundException | RuntimeException ex) {
            return SaveLoadResult.failure("Save is corrupted or incompatible");
        }
    }

    public SavePreview preview(SaveSlot slot) {
        if (slot == null || !Files.isRegularFile(path(slot))) {
            return new SavePreview(slot, false, false, "Empty", 0, "", 0, null);
        }
        try {
            SaveEnvelope envelope = readEnvelope(path(slot));
            restore(envelope);
            return new SavePreview(slot, true, false, envelope.name, envelope.turn,
                    envelope.season, envelope.townHallLevel, envelope.savedAt);
        } catch (Exception ex) {
            return new SavePreview(slot, true, true, "Corrupted save", 0, "", 0, null);
        }
    }

    public boolean delete(SaveSlot slot) {
        if (slot == null) return false;
        try { return Files.deleteIfExists(path(slot)); }
        catch (IOException ex) { return false; }
    }

    /** Non-mutating digest used to detect unsaved model progress. */
    public String fingerprint(GameState state) {
        try { return SaveSnapshot.fingerprint(state); }
        catch (IOException | RuntimeException ex) { return null; }
    }

    private Path path(SaveSlot slot) {
        return directory.resolve(slot.getFileName());
    }

    private SaveEnvelope readEnvelope(Path file) throws IOException, ClassNotFoundException {
        try (ObjectInputStream input = new ObjectInputStream(
                new BufferedInputStream(Files.newInputStream(file)))) {
            Object value = input.readObject();
            if (!(value instanceof SaveEnvelope)) {
                throw new InvalidObjectException("Not a Hex Dominion save");
            }
            SaveEnvelope envelope = (SaveEnvelope) value;
            if (!MAGIC.equals(envelope.magic)
                    || envelope.version < SaveMigrationRegistry.OLDEST_COMPATIBLE_VERSION
                    || envelope.version > SAVE_VERSION) {
                throw new InvalidObjectException("Unsupported save version");
            }
            return envelope;
        }
    }

    private GameState restore(SaveEnvelope envelope) throws IOException {
        GameState state = migrations.restore(envelope.version, SAVE_VERSION,
                envelope.snapshot, envelope.state);

        // Migration and derived projection repair operate on this detached
        // candidate. The live controller state is not replaced until all checks pass.
        state.prepareAfterLoad();
        state.validatePersistentState();
        if (state.getCurrentTurn() != envelope.turn
                || !state.getSeasonCycle().getCurrentSeason().name().equals(envelope.season)
                || state.getTownHall().getLevel().getLevelNumber() != envelope.townHallLevel) {
            throw new InvalidObjectException("Save summary does not match model state");
        }
        return state;
    }

    private void replacePreservingHealthyTarget(Path temporary, Path target) throws IOException {
        try {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
            return;
        } catch (AtomicMoveNotSupportedException unsupported) {
            // Same-directory atomic moves are expected on normal desktop file
            // systems. This guarded fallback restores the old bytes on failure.
        }

        Path backup = null;
        if (Files.isRegularFile(target)) {
            backup = Files.createTempFile(directory, target.getFileName().toString(), ".healthy");
            Files.copy(target, backup, StandardCopyOption.REPLACE_EXISTING);
        }
        boolean retainRecoveryBackup = false;
        try {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException failure) {
            if (backup != null && Files.isRegularFile(backup)) {
                try {
                    Files.copy(backup, target, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException restoreFailure) {
                    retainRecoveryBackup = true;
                    failure.addSuppressed(restoreFailure);
                }
            }
            throw failure;
        } finally {
            if (backup != null && !retainRecoveryBackup) Files.deleteIfExists(backup);
        }
    }

    /**
     * Keeps the historical binary identity and legacy state field so compatible
     * version-2 files can be read and migrated explicitly.
     */
    private static final class SaveEnvelope implements Serializable {
        private static final long serialVersionUID = 5896635689638271634L;

        private final String magic = MAGIC;
        private final int version;
        private final String name;
        private final Instant savedAt;
        private final int turn;
        private final String season;
        private final int townHallLevel;
        private final GameState state;       // version 2
        private final SaveSnapshot snapshot; // version 3+

        private SaveEnvelope(String name, GameState state, int version) throws IOException {
            this.version = version;
            this.name = name == null || name.trim().isEmpty() ? "Hex Dominion" : name.trim();
            this.savedAt = Instant.now();
            this.turn = state.getCurrentTurn();
            this.season = state.getSeasonCycle().getCurrentSeason().name();
            this.townHallLevel = state.getTownHall().getLevel().getLevelNumber();
            this.state = version == 2 ? state : null;
            this.snapshot = version >= 3 ? SaveSnapshot.capture(state) : null;
        }

        private static SaveEnvelope current(String name, GameState state) throws IOException {
            return new SaveEnvelope(name, state, SAVE_VERSION);
        }
    }
}
