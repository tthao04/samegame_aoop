package samegame.view;

import samegame.model.GameObserver;
import samegame.model.GameSnapshot;
import samegame.model.TileColor;

/**
 * A plain-text console view for SameGame.
 *
 * <h3>Pattern roles</h3>
 * <ul>
 *   <li><b>Observer</b>: Implements {@link GameObserver}; registered/de-registered
 *       with the model just like {@link SwingView}, but outputs to {@link System#out}
 *       instead of a window.</li>
 *   <li><b>View</b>: Read-only; never touches the model.</li>
 * </ul>
 *
 * <h3>Purpose</h3>
 * Useful for debugging the game engine without a display, running automated
 * tests, or logging moves to a terminal. The view prints the grid as a
 * character matrix after every state change, using single-letter colour codes.
 *
 * <h3>Colour codes used in output</h3>
 * <pre>
 *   .  = EMPTY
 *   R  = RED
 *   B  = BLUE
 *   G  = GREEN
 *   Y  = YELLOW
 *   P  = PURPLE
 * </pre>
 *
 * <h3>Usage</h3>
 * <pre>
 *   ConsoleView console = new ConsoleView();
 *   model.addObserver(console);
 *
 *   // Disable it later (e.g. in release builds):
 *   model.removeObserver(console);
 * </pre>
 */
public class ConsoleView implements GameObserver {

    // -------------------------------------------------------------------------
    // Configuration
    // -------------------------------------------------------------------------

    /**
     * If {@code true}, this view prints a separator line and the full grid
     * after every notification. Set to {@code false} to suppress grid output
     * and only print the score/status line (useful for high-frequency updates).
     */
    private boolean printGrid;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Creates a ConsoleView that prints the full grid on every update.
     */
    public ConsoleView() {
        this(true);
    }

    /**
     * Creates a ConsoleView with configurable verbosity.
     *
     * @param printGrid {@code true} to print the full character grid each update;
     *                  {@code false} to print only the score / status line.
     */
    public ConsoleView(boolean printGrid) {
        this.printGrid = printGrid;
    }

    // -------------------------------------------------------------------------
    // GameObserver implementation
    // -------------------------------------------------------------------------

    /**
     * Called by the model after every state change.
     *
     * Prints a separator, the current game state, and optionally the full
     * ASCII grid to {@link System#out}.
     *
     * @param snapshot the new game state; never {@code null}.
     */
    @Override
    public void onGameStateChanged(GameSnapshot snapshot) {
        System.out.println("─".repeat(snapshot.getCols() * 2 + 2));
        System.out.printf("[ConsoleView] Score: %d | Colors: %d | Tiles left: %d | Status: %s%n",
        snapshot.getScore(),
        snapshot.getNumColors(),
        snapshot.getRemainingTiles(),
        snapshot.getStatus());

        if (snapshot.isBoardCleared()) {
            System.out.println("[ConsoleView] *** BOARD CLEARED — +1000 bonus! ***");
        } else if (snapshot.isGameOver()) {
            System.out.println("[ConsoleView] *** GAME OVER ***");
        }

        if (printGrid) {
            printGrid(snapshot);
        }
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Enables or disables full-grid output.
     *
     * @param printGrid {@code true} to print the grid; {@code false} for
     *                  score-only output.
     */
    public void setPrintGrid(boolean printGrid) {
        this.printGrid = printGrid;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Renders the tile grid as a 2-D character matrix, row 0 (bottom) last
     * so the board reads visually the same way it appears on screen.
     *
     * @param snapshot the snapshot to render.
     */
    private void printGrid(GameSnapshot snapshot) {
        int cols = snapshot.getCols();
        int rows = snapshot.getRows();

        // Print from top row down so the visual matches the Swing view.
        for (int r = rows - 1; r >= 0; r--) {
            StringBuilder sb = new StringBuilder("|");
            for (int c = 0; c < cols; c++) {
                sb.append(toChar(snapshot.getTile(c, r)));
                sb.append(' ');
            }
            sb.setCharAt(sb.length() - 1, '|'); // replace trailing space
            System.out.println(sb);
        }

        // Column index ruler for debugging.
        StringBuilder ruler = new StringBuilder(" ");
        for (int c = 0; c < cols; c++) {
            ruler.append(c % 10).append(' ');
        }
        System.out.println(ruler);
    }

    /**
     * Maps a {@link TileColor} enum value to its single-character console code.
     *
     * @param tile the tile color to convert.
     * @return a single character representing the tile.
     */
    private static char toChar(TileColor tile) {
        return switch (tile) {
            case EMPTY  -> '.';
            case RED    -> 'R';
            case BLUE   -> 'B';
            case GREEN  -> 'G';
            case YELLOW -> 'Y';
            case PURPLE -> 'P';
        };
    }
}
