package network.messages;

import network.Response;
import network.dto.GameStateSnapshotDto;

/** Versioned transport snapshot. The payload is produced only by the server. */
public final class GameStateUpdate extends Response {
    private final long revision;
    private final GameStateSnapshotDto snapshot;

    public GameStateUpdate(long revision, GameStateSnapshotDto snapshot) {
        this.revision = revision;
        this.snapshot = snapshot;
    }

    public long getRevision() { return revision; }
    public GameStateSnapshotDto getSnapshot() { return snapshot; }
}
