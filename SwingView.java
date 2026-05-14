package samegame.view;

import java.awt.*;
import javax.swing.*;
import samegame.model.GameObserver;
import samegame.model.GameSnapshot;
import samegame.model.GameStatus;
import samegame.model.TileColor;

/**
 * The graphical Swing view for SameGame.
 *
 * This class is a View in the MVC structure and an Observer in the
 * Subject-Observer pattern. It receives snapshots from the model and uses them
 * to draw the game board.
 *
 * Important: This class does not change the game model directly.
 * The controller handles user input and calls the model.
 */
public class SwingView implements GameObserver {

    // -------------------------------------------------------------------------
    // UI components
    // -------------------------------------------------------------------------

    /** The main game window. */
    private final JFrame frame;

    /** The custom panel that draws the tile grid. */
    private final BoardPanel boardPanel;

    /** Label that shows the player's score. */
    private final JLabel scoreLabel;

    /** Label that shows how many colors are used. */
    private final JLabel colorsLabel;

    /** Label that shows WON / LOST status at the bottom of the window. */
    private final JLabel statusLabel;

    /**
     * This action is called when the player chooses "Play again".
     * In Main.java we send Main::startGame here.
     */
    private final Runnable restartAction;

    /**
     * This prevents the final score dialog from appearing more than once.
     * Without this boolean, the view could show the dialog again after repaint/update.
     */
    private boolean endDialogShown;

    // -------------------------------------------------------------------------
    // Layout constants
    // -------------------------------------------------------------------------

    /** The size of one tile in pixels. */
    private final int tileSize;

    /** Small gap between tiles in pixels. */
    private static final int TILE_GAP = 2;

    /** Background color behind the board. */
    private static final Color BOARD_BG = new Color(40, 40, 40);

    // -------------------------------------------------------------------------
    // State cache
    // -------------------------------------------------------------------------

    /**
     * The latest game state received from the model.
     * The board is drawn using this snapshot.
     */
    private volatile GameSnapshot currentSnapshot;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Constructor used if we only want to create the view without restart logic.
     * It calls the other constructor and sends null as restartAction.
     *
     * @param cols number of columns on the board.
     * @param rows number of rows on the board.
     * @param tileSize size of each tile in pixels.
     */
    public SwingView(int cols, int rows, int tileSize) {
        this(cols, rows, tileSize, null);
    }

    /**
     * Main constructor used by Main.java.
     *
     * @param cols number of columns on the board.
     * @param rows number of rows on the board.
     * @param tileSize size of each tile in pixels.
     * @param restartAction action used to start a new game after the game ends.
     */
    public SwingView(int cols, int rows, int tileSize, Runnable restartAction) {
        this.tileSize = tileSize;

        // Save the restart action so this view can call it later.
        this.restartAction = restartAction;

        // A new game has just started, so no end dialog has been shown yet.
        this.endDialogShown = false;

        // Create the main window.
        frame = new JFrame("SameGame");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(4, 4));

        // Create and add the board panel.
        // This is where the colored tiles are drawn.
        boardPanel = new BoardPanel(cols, rows);
        frame.add(boardPanel, BorderLayout.CENTER);

        // Create the status bar at the bottom of the window.
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        statusBar.setBackground(new Color(30, 30, 30));

        // Create labels for score, colors and game status.
        scoreLabel = makeLabel("Score: 0");
        colorsLabel = makeLabel("Colors: ?");
        statusLabel = makeLabel("");

        // Make the game status text a bit more visible.
        statusLabel.setForeground(new Color(255, 180, 50));

        // Add labels to the status bar.
        statusBar.add(scoreLabel);
        statusBar.add(colorsLabel);
        statusBar.add(statusLabel);

        // Add the status bar to the bottom of the frame.
        frame.add(statusBar, BorderLayout.SOUTH);

        // Make the window fit its contents.
        frame.pack();

        // Open the window in the center of the screen.
        frame.setLocationRelativeTo(null);

        // Disable resizing to keep the grid layout simple.
        frame.setResizable(false);
    }

    // -------------------------------------------------------------------------
    // GameObserver implementation
    // -------------------------------------------------------------------------

    /**
     * This method is called by GameModel every time the game state changes.
     * The view receives a GameSnapshot and redraws itself based on it.
     *
     * @param snapshot the latest game state from the model.
     */
    @Override
    public void onGameStateChanged(GameSnapshot snapshot) {
        // Save the latest snapshot so BoardPanel can draw from it.
        this.currentSnapshot = snapshot;

        // Swing updates should happen on the Event Dispatch Thread.
        SwingUtilities.invokeLater(() -> {
            // Update the labels at the bottom of the window.
            scoreLabel.setText("Score: " + snapshot.getScore());
            colorsLabel.setText("Colors: " + snapshot.getNumColors());

            // Show a clear status message when the game is won or lost.
            switch (snapshot.getStatus()) {
                case WON -> statusLabel.setText("Status: WON - Board cleared! +1000 bonus");
                case LOST -> statusLabel.setText("Status: LOST - Game Over — "
                        + snapshot.getRemainingTiles() + " tiles left");
                case RUNNING -> statusLabel.setText("");
            }

            // Repaint the board after the labels and snapshot have been updated.
            boardPanel.repaint();

            // If the game ended, show the final score dialog.
            // The method itself checks that the dialog is only shown once.
            if (snapshot.isGameOver()) {
                showEndDialog(snapshot);
            }
        });
    }

    // -------------------------------------------------------------------------
    // Public methods used by Main and MouseController
    // -------------------------------------------------------------------------

    /**
     * Makes the game window visible.
     */
    public void show() {
        frame.setVisible(true);
    }

    /**
     * Returns the JFrame. This can be useful if another controller needs it.
     *
     * @return the main window.
     */
    public JFrame getFrame() {
        return frame;
    }

    /**
     * Returns the board panel. MouseController uses this to attach mouse input.
     *
     * @return the panel where the board is drawn.
     */
    public BoardPanel getBoardPanel() {
        return boardPanel;
    }

    /**
     * Converts a mouse x-position into a board column.
     *
     * @param pixelX x-position from the mouse event.
     * @return column index, or -1 if outside the board.
     */
    public int pixelToCol(int pixelX) {
        int col = pixelX / (tileSize + TILE_GAP);

        if (currentSnapshot == null || col < 0 || col >= currentSnapshot.getCols()) {
            return -1;
        }

        return col;
    }

    /**
     * Converts a mouse y-position into a board row.
     *
     * Swing counts y from the top of the window, but the model uses row 0
     * as the bottom row. Because of that, we flip the row index here.
     *
     * @param pixelY y-position from the mouse event.
     * @return row index, or -1 if outside the board.
     */
    public int pixelToRow(int pixelY) {
        if (currentSnapshot == null) {
            return -1;
        }

        int rows = currentSnapshot.getRows();

        // This row is based on Swing's top-to-bottom coordinate system.
        int paintRow = pixelY / (tileSize + TILE_GAP);

        // Convert to the model's bottom-to-top coordinate system.
        int row = (rows - 1) - paintRow;

        return (row >= 0 && row < rows) ? row : -1;
    }

    // -------------------------------------------------------------------------
    // Helper methods
    // -------------------------------------------------------------------------

    /**
     * Creates a label with the same style as the other labels in the status bar.
     *
     * @param text text to show in the label.
     * @return styled JLabel.
     */
    private static JLabel makeLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(Color.WHITE);
        label.setFont(new Font("SansSerif", Font.BOLD, 13));
        return label;
    }

    /**
     * Shows a small dialog when the game has ended.
     * The dialog shows the final score and lets the player choose what to do next.
     *
     * @param snapshot the latest game state.
     */
    private void showEndDialog(GameSnapshot snapshot) {
        // Stop here if the dialog has already been shown.
        if (endDialogShown) {
            return;
        }

        // Mark the dialog as shown so it will not appear again for this game.
        endDialogShown = true;

        // Build the text shown in the dialog.
        String message;
        if (snapshot.getStatus() == GameStatus.WON) {
            message = "You won!\nFinal score: " + snapshot.getScore();
        } else {
            message = "Game over!\nFinal score: " + snapshot.getScore()
                    + "\nTiles left: " + snapshot.getRemainingTiles();
        }

        // Buttons in the dialog.
        String[] options = {"Play again", "Exit"};

        // Show the dialog and save the player's choice.
        int choice = JOptionPane.showOptionDialog(
                frame,
                message,
                "Game finished",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                options,
                options[0]
        );

        if (choice == 0 && restartAction != null) {
            // Player clicked Play again.
            // Close this window and call Main.startGame() through restartAction.
            frame.dispose();
            restartAction.run();
        } else if (choice == 1) {
            // Player clicked Exit.
            System.exit(0);
        }
    }

    // =========================================================================
    // Inner class: BoardPanel
    // =========================================================================

    /**
     * Panel that draws the SameGame board.
     *
     * It reads the board from currentSnapshot and draws each tile.
     */
    public class BoardPanel extends JPanel {

        private final int cols;
        private final int rows;

        /**
         * Creates the board panel.
         *
         * @param cols number of board columns.
         * @param rows number of board rows.
         */
        public BoardPanel(int cols, int rows) {
            this.cols = cols;
            this.rows = rows;

            setBackground(BOARD_BG);

            // Set the size of the panel based on board size and tile size.
            setPreferredSize(new Dimension(
                    cols * (tileSize + TILE_GAP) + TILE_GAP,
                    rows * (tileSize + TILE_GAP) + TILE_GAP
            ));
        }

        /**
         * Draws all tiles on the board.
         *
         * @param g graphics object provided by Swing.
         */
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            // If no snapshot has been received yet, there is nothing to draw.
            if (currentSnapshot == null) {
                return;
            }

            Graphics2D g2 = (Graphics2D) g;

            // Makes rounded rectangles look smoother.
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            for (int c = 0; c < cols; c++) {
                for (int r = 0; r < rows; r++) {
                    TileColor tile = currentSnapshot.getTile(c, r);

                    // Convert model row to paint row.
                    // Model row 0 is bottom, Swing row 0 is top.
                    int paintRow = (rows - 1) - r;

                    int x = TILE_GAP + c * (tileSize + TILE_GAP);
                    int y = TILE_GAP + paintRow * (tileSize + TILE_GAP);

                    if (tile.isEmpty()) {
                        // Draw empty cells as dark grey squares.
                        g2.setColor(new Color(55, 55, 55));
                        g2.fillRoundRect(x, y, tileSize, tileSize, 6, 6);
                    } else {
                        Color base = tile.getAwtColor();

                        // Main tile color.
                        g2.setColor(base);
                        g2.fillRoundRect(x, y, tileSize, tileSize, 8, 8);

                        // Small top-left highlight, so the tile looks a bit nicer.
                        g2.setColor(base.brighter());
                        g2.drawLine(x + 2, y + 2, x + tileSize - 3, y + 2);
                        g2.drawLine(x + 2, y + 2, x + 2, y + tileSize - 3);

                        // Small bottom-right shadow.
                        g2.setColor(base.darker());
                        g2.drawLine(x + tileSize - 2, y + 2,
                                x + tileSize - 2, y + tileSize - 2);
                        g2.drawLine(x + 2, y + tileSize - 2,
                                x + tileSize - 2, y + tileSize - 2);
                    }
                }
            }
        }
    }
}