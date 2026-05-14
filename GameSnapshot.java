package samegame.model;

/**
 * An immutable snapshot of the complete game state, passed to every
 * {@link GameObserver} when the model notifies them.
 *
 * <h3>Why a snapshot and not a reference to the model?</h3>
 * If observers received a live reference to the model they could either (a)
 * modify it, breaking encapsulation, or (b) read stale data if the model
 * changes between the notification and the rendering. A snapshot avoids both
 * problems: it is fully self-contained, immutable, and safe to pass across
 * threads.
 *
 * <h3>Defensive copies</h3>
 * The 2-D grid array is deep-copied in the constructor so that callers cannot
 * accidentally mutate a snapshot after creating it, and so that each observer
 * gets its own copy to iterate safely.
 */
public final class GameSnapshot {

    // -------------------------------------------------------------------------
    // Fields (all final – true immutability)
    // -------------------------------------------------------------------------

    /** Number of columns in the grid. */
    private final int cols;

    /** Number of rows in the grid. */
    private final int rows;

    /**
     * The tile grid: {@code grid[col][row]}.
     * (0,0) is the bottom-left cell; row increases upward.
     * A cell is {@link TileColor#EMPTY} when its tile has been removed.
     */
    private final TileColor[][] grid;

    /** The player's current score. */
    private final int score;

    /** Number of tile colors currently in play (controls difficulty). */
    private final int numColors;

    /** {@code true} when no further moves are possible. */
    private final boolean gameOver;

    /** {@code true} if the board is completely cleared (winning condition). */
    private final boolean boardCleared;

    /** Number of tiles still on the board (useful for views to show progress). */
    private final int remainingTiles;

    /** Current game status when this snapshot was created. */
    private final GameStatus status;
    
    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Creates an immutable snapshot from the current model state.
     *
     * @param cols           number of columns in the grid (≥ 1).
     * @param rows           number of rows in the grid (≥ 1).
     * @param grid           the live grid from the model — a defensive copy is
     *                       made, so the caller may safely mutate it afterwards.
     *                       Must be {@code grid[cols][rows]}.
     * @param score          the player's current score (≥ 0).
     * @param numColors      number of distinct tile colors on the board (1–5).
     * @param gameOver       {@code true} when no group of ≥ 2 tiles remains.
     * @param boardCleared   {@code true} when every cell is {@link TileColor#EMPTY}.
     * @param remainingTiles number of non-empty tiles still on the board.
     */
    public GameSnapshot(int cols, int rows, TileColor[][] grid,
                        int score, int numColors,
                        boolean gameOver, boolean boardCleared,
                        int remainingTiles, GameStatus status) {
        this.cols = cols;
        this.rows = rows;
        this.score = score;
        this.numColors = numColors;
        this.gameOver = gameOver;
        this.boardCleared = boardCleared;
        this.remainingTiles = remainingTiles;
        this.status = status;

        // Deep copy so the snapshot is truly immutable.
        this.grid = new TileColor[cols][rows];
        for (int c = 0; c < cols; c++) {
            System.arraycopy(grid[c], 0, this.grid[c], 0, rows);
        }
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    /** @return number of columns. */
    public int getCols() { return cols; }

    /** @return number of rows. */
    public int getRows() { return rows; }

    /**
     * Returns the tile color at the given grid cell.
     *
     * @param col column index (0 = left).
     * @param row row index    (0 = bottom).
     * @return the {@link TileColor} at that position; never {@code null}.
     */
    public TileColor getTile(int col, int row) {
        return grid[col][row];
    }

    /** @return the player's current score. */
    public int getScore() { return score; }

    /** @return number of distinct colors in play. */
    public int getNumColors() { return numColors; }

    /** @return {@code true} when the game is over (no valid moves left). */
    public boolean isGameOver() { return gameOver; }

    /** @return {@code true} if the board has been fully cleared (win!). */
    public boolean isBoardCleared() { return boardCleared; }

    /** @return number of non-empty tiles remaining on the board. */
    public int getRemainingTiles() { return remainingTiles; }

    /** @return current game status. */
    public GameStatus getStatus() {
    return status;
}
}
