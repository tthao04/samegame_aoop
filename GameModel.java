package samegame.model;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

/**
 * The game model for SameGame — the "M" in MVC and the "Subject" in the
 * Subject-Observer pattern.
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Stores the tile grid and all game state (score, game-over flag, …).</li>
 *   <li>Implements {@link GameSubject}: maintains the observer list and notifies
 *       all registered observers after every state change.</li>
 *   <li>Exposes business-logic operations ({@link #removeTiles}, {@link #reset})
 *       that controllers call in response to user input.</li>
 * </ul>
 *
 * <h3>Grid convention</h3>
 * {@code grid[col][row]} where {@code col} goes left→right and {@code row}
 * goes bottom→top. When tiles are removed, the tiles above them fall down
 * (gravity), and when an entire column is empty it is shifted left.
 *
 * <h3>Observer registration</h3>
 * <pre>
 *   GameModel model = new GameModel(15, 10, 3);
 *   SwingView  gui  = new SwingView(model);
 *   ConsoleView con = new ConsoleView();
 *   model.addObserver(gui);
 *   model.addObserver(con);
 *
 *   // Close the debug console later:
 *   model.removeObserver(con);
 * </pre>
 */
public class GameModel implements GameSubject {

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------

    /** Minimum number of tiles in a connected group to be removable. */
    public static final int MIN_GROUP_SIZE = 2;

    /**
     * Bonus score awarded when the board is completely cleared.
     * Formula follows the classic SameGame convention.
     */
    public static final int BOARD_CLEAR_BONUS = 1000;

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    /** Width of the grid in tiles. */
    private final int cols;

    /** Height of the grid in tiles. */
    private final int rows;

    /** Number of distinct colors used (controls difficulty; 2–5). */
    private final int numColors;

    /**
     * The live tile grid. {@code grid[c][r] == TileColor.EMPTY} means the
     * cell has been cleared.
     */
    private TileColor[][] grid;

    /** Current player score. */
    private int score;

    /** {@code true} once no group of ≥ {@link #MIN_GROUP_SIZE} tiles remains. */
    private boolean gameOver;

    /** {@code true} if every cell on the board is empty. */
    private boolean boardCleared;

    /** Current status of the game: running, won, or lost. */
    private GameStatus status = GameStatus.RUNNING;

    /** Registered observers (views, loggers, recorders, …). */
    private final List<GameObserver> observers = new ArrayList<>();

    /** Shared random instance for board generation. */
    private final Random random = new Random();

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Creates and initialises a new SameGame model.
     *
     * @param cols      number of tile columns (recommended: 10–20).
     * @param rows      number of tile rows    (recommended:  8–15).
     * @param numColors number of distinct tile colors in play (2 = easy, 5 = hard).
     *                  Must be between 2 and {@code TileColor.values().length - 1}.
     */
    public GameModel(int cols, int rows, int numColors) {
        if (numColors < 2 || numColors > TileColor.values().length - 1) {
            throw new IllegalArgumentException(
                "numColors must be 2–" + (TileColor.values().length - 1)
                + ", got " + numColors);
        }
        this.cols = cols;
        this.rows = rows;
        this.numColors = numColors;
        this.grid = new TileColor[cols][rows];
        fillBoard();
    }

    // -------------------------------------------------------------------------
    // GameSubject implementation
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * Duplicate registrations are silently ignored.
     */
    @Override
    public void addObserver(GameObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
        }
    }

    /**
     * {@inheritDoc}
     *
     * Removing an observer that was not registered is silently ignored.
     */
    @Override
    public void removeObserver(GameObserver observer) {
        observers.remove(observer);
    }

    /**
     * {@inheritDoc}
     *
     * Builds an immutable {@link GameSnapshot} from the current model state
     * and delivers it to every registered observer.
     */
    @Override
    public void notifyObservers() {
        GameSnapshot snapshot = buildSnapshot();
        for (GameObserver observer : observers) {
            observer.onGameStateChanged(snapshot);
        }
    }

    // -------------------------------------------------------------------------
    // Game operations (called by controllers)
    // -------------------------------------------------------------------------

    /**
     * Attempts to remove the connected group of same-colored tiles that
     * contains the tile at {@code (col, row)}.
     *
     * <p>Rules:
     * <ol>
     *   <li>The cell must be non-empty.</li>
     *   <li>The connected group (flood-fill, 4-connectivity) must have at
     *       least {@link #MIN_GROUP_SIZE} tiles.</li>
     *   <li>On success: tiles are cleared, gravity applied, empty columns
     *       collapsed, score updated, game-over / board-clear flags checked.</li>
     *   <li>On failure: the model is unchanged.</li>
     * </ol>
     *
     * @param col column of the clicked/selected tile (0-based, left = 0).
     * @param row row    of the clicked/selected tile (0-based, bottom = 0).
     * @return {@code true} if a group was successfully removed; {@code false}
     *         if the move was illegal (empty cell or group too small).
     */
        public boolean removeTiles(int col, int row) {
        if (gameOver || status != GameStatus.RUNNING) return false;
        if (!isValid(col, row) || grid[col][row].isEmpty()) return false;

        List<int[]> group = findConnectedGroup(col, row);
        if (group.size() < MIN_GROUP_SIZE) return false;

        // Clear the tiles.
        for (int[] cell : group) {
            grid[cell[0]][cell[1]] = TileColor.EMPTY;
        }

        // Scoring: (n-2)^2 where n is group size (standard SameGame formula).
        int n = group.size();
        score += (n - 2) * (n - 2);

        applyGravity();
        collapseEmptyColumns();
        checkEndConditions();
        notifyObservers();
        return true;
    }

    /**
     * Resets the model to a new game with the same dimensions and color count.
     * All observers are notified of the fresh state.
     */
    public void reset() {
        score = 0;
        gameOver = false;
        boardCleared = false;
        status = GameStatus.RUNNING;
        grid = new TileColor[cols][rows];
        fillBoard();
        notifyObservers();
    }

    /**
     * Returns the set of cells that form the connected group around (col, row),
     * without modifying the board. Views can use this to highlight a potential
     * move before the player confirms it.
     *
     * @param col target column.
     * @param row target row.
     * @return list of {@code [col, row]} pairs in the group; empty if the cell
     *         is empty or the group has fewer than {@link #MIN_GROUP_SIZE} tiles.
     */
    public List<int[]> previewGroup(int col, int row) {
        if (!isValid(col, row) || grid[col][row].isEmpty()) return List.of();
        List<int[]> group = findConnectedGroup(col, row);
        return group.size() >= MIN_GROUP_SIZE ? group : List.of();
    }

    // -------------------------------------------------------------------------
    // Accessors (read-only view into the model for views that need direct access)
    // -------------------------------------------------------------------------

    /** @return grid width in tiles. */
    public int getCols() { return cols; }

    /** @return grid height in tiles. */
    public int getRows() { return rows; }

    /** @return current score. */
    public int getScore() { return score; }

    /** @return number of tile colors in play. */
    public int getNumColors() { return numColors; }

    /** @return {@code true} if the game has ended. */
    public boolean isGameOver() { return gameOver; }

    /** @return {@code true} if the board is fully cleared. */
    public boolean isBoardCleared() { return boardCleared; }

    /** @return the current game status. */
    public GameStatus getStatus() { return status; }
    
    
    /**
     * Returns the color at a specific cell (for controllers that need it).
     *
     * @param col column index.
     * @param row row index.
     * @return the {@link TileColor} at that position.
     */
    public TileColor getTile(int col, int row) { return grid[col][row]; }

        /**
     * Checks if the board still contains at least one valid move.
     * A valid move is a connected group with at least MIN_GROUP_SIZE tiles.
     *
     * @return true if the player can still remove at least one group.
     */
    public boolean hasValidMoves() {
        for (int c = 0; c < cols; c++) {
            for (int r = 0; r < rows; r++) {
                if (!grid[c][r].isEmpty()
                        && findConnectedGroup(c, r).size() >= MIN_GROUP_SIZE) {
                    return true;
                }
            }
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /** Fills the board with random tile colors chosen from the active palette. */
    private void fillBoard() {
        // TileColor.values()[0] is EMPTY; active colors start at index 1.
        TileColor[] palette = new TileColor[numColors];
        for (int i = 0; i < numColors; i++) {
            palette[i] = TileColor.values()[i + 1];
        }
        for (int c = 0; c < cols; c++) {
            for (int r = 0; r < rows; r++) {
                grid[c][r] = palette[random.nextInt(numColors)];
            }
        }
    }

    /**
     * BFS flood-fill: finds all tiles connected (4-way) to {@code (startCol, startRow)}
     * that share the same color.
     *
     * @return list of {@code [col, row]} int arrays; never {@code null}.
     */
    private List<int[]> findConnectedGroup(int startCol, int startRow) {
        TileColor target = grid[startCol][startRow];
        boolean[][] visited = new boolean[cols][rows];
        List<int[]> group = new ArrayList<>();

        Queue<int[]> queue = new LinkedList<>();
        queue.add(new int[]{startCol, startRow});
        visited[startCol][startRow] = true;

        int[][] directions = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};

        while (!queue.isEmpty()) {
            int[] cell = queue.poll();
            group.add(cell);
            for (int[] d : directions) {
                int nc = cell[0] + d[0];
                int nr = cell[1] + d[1];
                if (isValid(nc, nr) && !visited[nc][nr] && grid[nc][nr] == target) {
                    visited[nc][nr] = true;
                    queue.add(new int[]{nc, nr});
                }
            }
        }
        return group;
    }

    /**
     * Applies gravity: in each column, tiles fall down to fill empty cells.
     * After this, all empty cells in a column are at the top.
     */
    private void applyGravity() {
        for (int c = 0; c < cols; c++) {
            int writeRow = 0;
            for (int r = 0; r < rows; r++) {
                if (!grid[c][r].isEmpty()) {
                    grid[c][writeRow++] = grid[c][r];
                }
            }
            // Fill remaining top cells with EMPTY.
            while (writeRow < rows) {
                grid[c][writeRow++] = TileColor.EMPTY;
            }
        }
    }

    /**
     * Removes completely empty columns by shifting non-empty columns to the left.
     */
    private void collapseEmptyColumns() {
        int writeCol = 0;
        for (int c = 0; c < cols; c++) {
            if (!grid[c][0].isEmpty()) {          // column has at least one tile
                if (writeCol != c) {
                    System.arraycopy(grid[c], 0, grid[writeCol], 0, rows);
                }
                writeCol++;
            }
        }
        // Fill remaining right columns with EMPTY.
        while (writeCol < cols) {
            for (int r = 0; r < rows; r++) {
                grid[writeCol][r] = TileColor.EMPTY;
            }
            writeCol++;
        }
    }

    /**
     * Checks if the game has been won or lost after a move.
     *
     * The player wins if the board is completely empty.
     * The player loses if there are tiles left but no valid moves remain.
     */
    private void checkEndConditions() {
        int remaining = countRemainingTiles();

        if (remaining == 0) {
            boardCleared = true;
            gameOver = true;
            status = GameStatus.WON;
            score += BOARD_CLEAR_BONUS;
            return;
        }

        boardCleared = false;

        if (!hasValidMoves()) {
            gameOver = true;
            status = GameStatus.LOST;
        } else {
            gameOver = false;
            status = GameStatus.RUNNING;
        }
    }

    /**
     * Counts how many non-empty tiles are still on the board.
     *
     * @return number of remaining tiles.
     */
    private int countRemainingTiles() {
        int remaining = 0;
        for (int c = 0; c < cols; c++) {
            for (int r = 0; r < rows; r++) {
                if (!grid[c][r].isEmpty()) {
                    remaining++;
                }
            }
        }
        return remaining;
    }

    /** @return {@code true} if (col, row) is within grid bounds. */
    private boolean isValid(int col, int row) {
        return col >= 0 && col < cols && row >= 0 && row < rows;
    }

    /**
     * Builds and returns an immutable {@link GameSnapshot} from the current
     * model state. Called by {@link #notifyObservers()}.
     */
    private GameSnapshot buildSnapshot() {
    int remaining = countRemainingTiles();
    return new GameSnapshot(cols, rows, grid, score, numColors,
                                gameOver, boardCleared, remaining, status);
    }
}
