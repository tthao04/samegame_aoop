package samegame.model;

/**
 * The Subject interface in the Subject-Observer pattern.
 *
 * A class implementing {@code GameSubject} maintains a list of registered
 * {@link GameObserver}s and notifies them whenever the game state changes.
 *
 * Typical usage from application code:
 * <pre>
 *   GameModel model = new GameModel(cols, rows, numColors);
 *   model.addObserver(new SwingView());
 *   model.addObserver(new ConsoleView());
 *
 *   // Later, to remove a view (e.g. close the debug console):
 *   model.removeObserver(consoleView);
 * </pre>
 *
 * The separation of Subject and Observer into interfaces means that views
 * depend only on these interfaces, never on concrete model classes. This
 * makes it easy to swap model implementations or add new view types without
 * touching existing code.
 */
public interface GameSubject {

    /**
     * Registers an observer to receive future state-change notifications.
     *
     * If the observer is already registered, this call has no effect (no
     * duplicate notifications are sent).
     *
     * @param observer the observer to add; must not be {@code null}.
     */
    void addObserver(GameObserver observer);

    /**
     * Removes a previously registered observer.
     *
     * After this call the observer will no longer receive notifications.
     * If the observer was not registered, this call has no effect.
     *
     * @param observer the observer to remove; must not be {@code null}.
     */
    void removeObserver(GameObserver observer);

    /**
     * Sends the current game state to all currently registered observers.
     *
     * Called internally by the model after every state change. Application
     * code normally does not need to call this directly.
     */
    void notifyObservers();
}
