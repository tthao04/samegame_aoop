package samegame;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import samegame.controller.MouseController;
import samegame.model.GameModel;
import samegame.view.CaptureView;
import samegame.view.ConsoleView;
import samegame.view.SwingView;
import samegame.controller.InputController;

/**
 * Application entry point for SameGame.
 *
 * This class starts the application and connects the model, views and controller.
 *
 * The general structure is:
 *
 * GameModel      = stores game state and game logic
 * SwingView      = graphical view
 * ConsoleView    = text/debug view
 * CaptureView    = records game snapshots
 * MouseController = sends mouse clicks to the model
 *
 * This shows a mix of MVC and Subject-Observer:
 * - GameModel is the model and also the subject.
 * - SwingView, ConsoleView and CaptureView are observers.
 * - MouseController works as the controller.
 */
public class Main {

    /**
     * Small helper class that stores the settings for one difficulty level.
     *
     * cols and rows decide the board size.
     * colors decides how many colors are used.
     * tileSize decides how large each tile is in the Swing window.
     */
    private static class Difficulty {
        int cols;
        int rows;
        int colors;
        int tileSize;

        Difficulty(int cols, int rows, int colors, int tileSize) {
            this.cols = cols;
            this.rows = rows;
            this.colors = colors;
            this.tileSize = tileSize;
        }
    }

    /**
     * Shows a small dialog before the game starts where the player chooses difficulty.
     *
     * Easy has fewer colors and a smaller board.
     * Normal uses the standard board and three colors.
     * Hard has more colors and a larger board.
     *
     * @return the selected difficulty settings.
     */
    private static Difficulty chooseDifficulty() {
        String[] options = {"Easy", "Normal", "Hard"};

        int choice = JOptionPane.showOptionDialog(
                null,
                "Choose difficulty:",
                "SameGame",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[1]
        );

        if (choice == 0) {
            // Easy: small board and only 2 colors.
            return new Difficulty(10, 8, 2, 50);
        } else if (choice == 2) {
            // Hard: larger board and 5 colors.
            return new Difficulty(18, 12, 5, 35);
        } else {
            // Normal is also used if the player closes the dialog.
            return new Difficulty(15, 10, 3, 42);
        }
    }

    /**
     * Program start.
     *
     * Swing code should run on the Event Dispatch Thread,
     * so we start the game with SwingUtilities.invokeLater().
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::startGame);
    }

    /**
     * Creates a new game.
     *
     * This method is also used when the player chooses "Play again"
     * after the game has ended. Because it starts with chooseDifficulty(),
     * the player can choose a new difficulty without restarting the program.
     */
    private static void startGame() {
        // 1. Ask the player which difficulty to use.
        Difficulty difficulty = chooseDifficulty();

        // 2. Create the model.
        // The model gets the board size and number of colors from the difficulty.
        GameModel model = new GameModel(
                difficulty.cols,
                difficulty.rows,
                difficulty.colors
        );

        // 3. Create the graphical view.
        // Main::startGame is sent to SwingView so it can start a new game
        // when the player clicks "Play again" in the final score dialog.
        SwingView swingView = new SwingView(
                difficulty.cols,
                difficulty.rows,
                difficulty.tileSize,
                Main::startGame
        );

        // 4. Create the other two views.
        // ConsoleView prints the game state in the terminal.
        // CaptureView saves the game states in a list for possible replay/analysis.
        ConsoleView consoleView = new ConsoleView(true);
        CaptureView captureView = new CaptureView();

        // 5. Register all views as observers.
        // After this, they will receive a GameSnapshot whenever the model changes.
        model.addObserver(swingView);
        model.addObserver(consoleView);
        model.addObserver(captureView);

        // 6. Create and attach the controller.
        // The controller listens for mouse clicks and calls model.removeTiles().
        // We store it as InputController so the input method can be changed later.
       InputController controller = new MouseController(model, swingView);
       controller.attach();

        // 7. Send the first snapshot so the views can show the starting board.
        model.notifyObservers();

        // 8. Show the graphical window.
        swingView.show();

        // 9. Demonstrate that observers can be removed at runtime.
        // After 10 seconds, the console view stops printing,
        // but SwingView and CaptureView still work.
        scheduleConsoleViewRemoval(model, consoleView, 10_000);
    }

    /**
     * Removes the ConsoleView after a short delay.
     *
     * This is only used to demonstrate that observers can be de-registered.
     *
     * @param model the game model.
     * @param consoleView the console view to remove.
     * @param delayMs delay in milliseconds before removing the console view.
     */
    private static void scheduleConsoleViewRemoval(GameModel model,
                                                   ConsoleView consoleView,
                                                   long delayMs) {
        Thread deregThread = new Thread(() -> {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                return;
            }

            model.removeObserver(consoleView);

            System.out.println(
                    "[Main] ConsoleView de-registered after " + delayMs / 1000 + " s. "
                            + "Console output will now stop; Swing + CaptureView still active."
            );
        }, "observer-deregister-demo");

        // Daemon means this thread will not keep the program alive by itself.
        deregThread.setDaemon(true);
        deregThread.start();
    }
}