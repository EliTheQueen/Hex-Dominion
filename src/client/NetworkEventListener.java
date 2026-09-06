package client;

import model.GameState;

/** Events delivered on Swing's EDT so views can safely refresh themselves. */
public interface NetworkEventListener {
    default void onHello(int clientId, boolean controller) {}
    default void onStateUpdate(GameState gameState, long revision) {}
    default void onError(String code, String message) {}
    default void onHeartbeat(boolean reachable) {}
    default void onDisconnected(String reason) {}
}
