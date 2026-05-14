package samegame.controller;

import samegame.model.GameModel;
import samegame.view.SwingView;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * A mouse-based input controller for SameGame.
 *
 * <h3>Pattern roles</h3>
 * <ul>
 *   <li><b>Controller</b> (MVC): Sits between the view and the model. Translates
 *       raw mouse events from the {@link SwingView} into {@link GameModel}
 *       operations ({@code removeTiles}, {@code reset}).</li>
 *   <li><b>Strategy</b>: One concrete input strategy. The controller can be
 *       swapped for a keyboard controller or a network controller without
 *       changing the model or the views.</li>
 * </ul>
 *
 * <h3>Wiring</h3>
 * <pre>
 *   SwingView view   = new SwingView(cols, rows, tileSize);
 *   GameModel model  = new GameModel(cols, rows, numColors);
 *   MouseController mc = new MouseController(model, view);
 *   mc.attach();   // installs the mouse listener
 *   view.show();
 * </pre>
 *
 * <h3>Detaching</h3>
 * Call {@link #detach()} to remove the listener, e.g. when switching input
 * methods or when the game ends and a replay is about to start.
 */
public class MouseController implements InputController {

    private final GameModel model;
    private final SwingView view;

    /** The listener instance we install, kept so we can remove it later. */
    private final MouseAdapter listener;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Creates a MouseController linking the given model and view.
     * Call {@link #attach()} to start receiving events.
     *
     * @param model the game model to send moves to; must not be {@code null}.
     * @param view  the Swing view whose board panel to listen on; must not be
     *              {@code null}.
     */
    public MouseController(GameModel model, SwingView view) {
        this.model = model;
        this.view  = view;

        this.listener = new MouseAdapter() {
            /**
             * Translates a mouse click on the board panel into a tile-removal
             * request on the model.
             *
             * Left-click  → attempt to remove the connected group at that cell.
             * Right-click → reset the game (new board).
             */
            @Override
            public void mouseClicked(MouseEvent e) {
                if (model.isGameOver()) {
                    if (e.getButton() == MouseEvent.BUTTON3) {
                        model.reset(); // right-click to restart when game is over
                    }
                    return;
                }

                int col = view.pixelToCol(e.getX());
                int row = view.pixelToRow(e.getY());

                if (col < 0 || row < 0) return; // click was outside the grid

                if (e.getButton() == MouseEvent.BUTTON1) {
                    // Left-click: attempt move.
                    model.removeTiles(col, row);
                } else if (e.getButton() == MouseEvent.BUTTON3) {
                    // Right-click: new game.
                    model.reset();
                }
            }
        };
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /**
     * Attaches the mouse listener to the board panel.
     * Safe to call multiple times (subsequent calls have no effect).
     */
    @Override
    public void attach() {
        view.getBoardPanel().addMouseListener(listener);
    }

    /**
     * Removes the mouse listener from the board panel.
     * After this call, clicks no longer affect the model.
     */
    @Override
    public void detach() {
        view.getBoardPanel().removeMouseListener(listener);
    }
}
