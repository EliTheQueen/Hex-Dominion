package model;

/** Save-relevant simulation phase. Only STABLE may be persisted. */
public enum GamePhase {
    STABLE,
    END_TURN,
    COMBAT_PRESENTATION
}
