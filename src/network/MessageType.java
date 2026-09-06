package network;

/** Application messages carried as newline-delimited JSON over TCP. */
public enum MessageType {
    HELLO,
    START_GAME,
    MOVE_UNIT,
    END_TURN,
    BUILD,
    STATE_UPDATE,
    ERROR,
    PING,
    PONG
}
