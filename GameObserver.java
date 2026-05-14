package samegame.model;

/**
 * The Observer interface in the Subject-Observer (publish-subscribe) pattern.
 *
 * Any class that wants to receive updates about the game state must implement
 * this interface and register itself with a {@link GameSubject}.
 *
 * Architecture note:
 *   GameSubject (model) ──notifies──► GameObserver (views / loggers / recorders)
 *
 * When the model changes (e.g. tiles removed, score updated, game over), it
 * calls {@link #onGameStateChanged(GameSnapshot)} on every registered observer,
 * handing them an immutable snapshot of the new state so each view can render
 * itself independently.
 */
public interface GameObserver {

    /**
     * Called by the subject (model) whenever the game state changes.
     *
     * Implementations should read all information they need from the supplied
     * snapshot and update their representation accordingly. The snapshot is
     * immutable, so observers do not need to worry about concurrent modification.
     *
     * @param snapshot an immutable description of the current game state,
     *                 including the tile grid, score, and end-of-game flags.
     *                 Never {@code null}.
     */
    void onGameStateChanged(GameSnapshot snapshot);
}
