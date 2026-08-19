package model.save;

import java.io.InvalidObjectException;

import model.GameState;

/** Ordered, explicit migrations for compatible historical save envelopes. */
public final class SaveMigrationRegistry {
    public static final int OLDEST_COMPATIBLE_VERSION = 2;

    public GameState restore(
            int sourceVersion,
            int targetVersion,
            SaveSnapshot snapshot,
            GameState legacyState
    ) throws java.io.IOException {
        if (sourceVersion < OLDEST_COMPATIBLE_VERSION || sourceVersion > targetVersion) {
            throw new InvalidObjectException("Unsupported save version " + sourceVersion);
        }

        GameState state = sourceVersion >= 3
                ? requireSnapshot(snapshot)
                : requireLegacyState(legacyState);

        for (int version = sourceVersion; version < targetVersion; version++) {
            if (version == 2) {
                state.migratePersistentState(2);
            } else {
                throw new InvalidObjectException("Missing migration from version " + version);
            }
        }
        return state;
    }

    private GameState requireSnapshot(SaveSnapshot snapshot) throws java.io.IOException {
        if (snapshot == null) throw new InvalidObjectException("Save snapshot is missing");
        return snapshot.restoreVerified();
    }

    private GameState requireLegacyState(GameState state) throws InvalidObjectException {
        if (state == null) throw new InvalidObjectException("Legacy game state is missing");
        return state;
    }
}
