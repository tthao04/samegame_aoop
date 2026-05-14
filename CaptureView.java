package samegame.view;

import samegame.model.GameObserver;
import samegame.model.GameSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A "capture" observer that silently records every game state for later
 * replay or analysis.
 *
 * <h3>Pattern roles</h3>
 * <ul>
 *   <li><b>Observer</b>: Implements {@link GameObserver}. Registered and
 *       de-registered just like any other view, but produces no visible output
 *       during normal play.</li>
 *   <li><b>Recorder / Replay buffer</b>: Maintains an ordered list of every
 *       {@link GameSnapshot} the model has ever published. Callers can retrieve
 *       the full history with {@link #getHistory()}.</li>
 * </ul>
 *
 * <h3>Possible uses</h3>
 * <ul>
 *   <li>Replay mode: iterate through the captured list and feed each snapshot
 *       to a view manually.</li>
 *   <li>Post-game analysis: inspect which moves were made and how the score
 *       evolved.</li>
 *   <li>Automated testing: run a scripted game and assert on the captured
 *       snapshots without requiring a display.</li>
 *   <li>Undo/redo: expose {@link #getSnapshot(int)} to restore a previous
 *       state into the model.</li>
 * </ul>
 *
 * <h3>Usage</h3>
 * <pre>
 *   CaptureView capture = new CaptureView();
 *   model.addObserver(capture);
 *
 *   // … play the game …
 *
 *   // Later: replay every captured state through a fresh Swing window.
 *   for (GameSnapshot snap : capture.getHistory()) {
 *       replayView.onGameStateChanged(snap);
 *       Thread.sleep(500);
 *   }
 * </pre>
 */
public class CaptureView implements GameObserver {

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    /**
     * Ordered list of every snapshot received from the model.
     * Index 0 is the initial game state (if the model notified at start),
     * with later entries corresponding to subsequent moves.
     */
    private final List<GameSnapshot> history = new ArrayList<>();

    // -------------------------------------------------------------------------
    // GameObserver implementation
    // -------------------------------------------------------------------------

    /**
     * Called by the model after every state change.
     *
     * Appends the snapshot to the history list. Because {@link GameSnapshot}
     * is immutable, no defensive copy is needed here.
     *
     * @param snapshot the new game state; never {@code null}.
     */
    @Override
    public void onGameStateChanged(GameSnapshot snapshot) {
        history.add(snapshot);
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Returns an unmodifiable view of the full capture history.
     *
     * The list is ordered from oldest (index 0) to newest (last index).
     * Callers may iterate over it or pass individual snapshots to any
     * {@link GameObserver} for replay.
     *
     * @return unmodifiable list of all captured snapshots; never {@code null},
     *         may be empty if no notifications have been received yet.
     */
    public List<GameSnapshot> getHistory() {
        return Collections.unmodifiableList(history);
    }

    /**
     * Returns the snapshot at a specific position in the history.
     *
     * @param index 0-based position (0 = first captured state).
     * @return the snapshot at {@code index}.
     * @throws IndexOutOfBoundsException if {@code index} is out of range.
     */
    public GameSnapshot getSnapshot(int index) {
        return history.get(index);
    }

    /**
     * Returns the number of snapshots recorded so far.
     *
     * @return size of the capture history (0 if nothing has been recorded).
     */
    public int size() {
        return history.size();
    }

    /**
     * Returns the most recently captured snapshot, or {@code null} if the
     * history is empty.
     *
     * @return latest snapshot, or {@code null}.
     */
    public GameSnapshot getLatest() {
        return history.isEmpty() ? null : history.get(history.size() - 1);
    }

    /**
     * Clears the capture history, freeing memory.
     * Useful between games when starting a fresh session.
     */
    public void clear() {
        history.clear();
    }

    /**
     * Replays the entire captured history through the given observer.
     *
     * <p>Feeds each recorded snapshot to {@code target} in order, pausing
     * {@code delayMs} milliseconds between each frame.</p>
     *
     * @param target  the observer (view) to feed the replay to; not {@code null}.
     * @param delayMs pause between frames in milliseconds (use 0 for instant).
     * @throws InterruptedException if the thread is interrupted during replay.
     */
    public void replay(GameObserver target, long delayMs) throws InterruptedException {
        for (GameSnapshot snap : history) {
            target.onGameStateChanged(snap);
            if (delayMs > 0) {
                Thread.sleep(delayMs);
            }
        }
    }
}
