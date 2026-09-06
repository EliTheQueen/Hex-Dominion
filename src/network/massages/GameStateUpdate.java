package network.massages;

import network.Response;

/** Versioned transport snapshot. The payload is produced only by the server. */
public final class GameStateUpdate extends Response {
    private final long revision;
    private final String state;

    public GameStateUpdate(long revision, String state) {
        this.revision = revision;
        this.state = state;
    }

    public long getRevision() { return revision; }
    public String getState() { return state; }
}
