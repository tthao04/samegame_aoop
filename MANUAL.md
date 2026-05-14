# SameGame — Observer/MVC Layer Manual

**Author:** SameGame group project  
**Language:** Java 17 or later  
**Patterns used:** Subject-Observer, MVC, Strategy-like input handling

---

## 1. Architecture at a Glance

```text
┌──────────────────────────────────────────────────────────────────┐
│                        SameGame Architecture                      │
│                                                                    │
│   ┌──────────────┐  notifyObservers()  ┌──────────────────────┐  │
│   │  GameModel   │────────────────────►│   <<interface>>      │  │
│   │  (Subject)   │                     │   GameObserver       │  │
│   └──────┬───────┘                     └──────────┬───────────┘  │
│          │                                        │               │
│   addObserver(o)                        implements│               │
│   removeObserver(o)                              ╱│╲              │
│          │                            ┌──────────┼───────────┐   │
│          │                            ▼          ▼           ▼   │
│          │                       SwingView  ConsoleView CaptureView│
│          │                       (Swing GUI)(Debug log) (Recorder)│
│          │                                                        │
│   ┌──────▼──────────────┐                                         │
│   │   MouseController   │  left-click → model.removeTiles()       │
│   │  (input handling)   │  right-click → model.reset()            │
│   └─────────────────────┘                                         │
└──────────────────────────────────────────────────────────────────┘
```

### Data flow summary

1. The program starts in `Main`.
2. The player chooses a difficulty level: `Easy`, `Normal`, or `Hard`.
3. `Main` creates a `GameModel` with the chosen board size and number of colors.
4. `Main` creates the views and the mouse controller.
5. The player clicks the board.
6. `MouseController` translates the mouse position to `(col, row)`.
7. The controller calls `model.removeTiles(col, row)`.
8. The model updates the grid, score, game status and end-of-game flags.
9. The model calls `notifyObservers()` and builds a `GameSnapshot`.
10. Every registered `GameObserver` receives the snapshot.
11. Each observer independently re-renders itself or records the state.

---

## 2. Difficulty Selection

Before the game window opens, the player chooses a difficulty level.

The difficulty affects both the board size and the number of colors:

| Difficulty | Board size | Colors | Description |
|-----------|------------|--------|-------------|
| `Easy` | 10 x 8 | 2 colors | Smaller board and fewer colors |
| `Normal` | 15 x 10 | 3 colors | Standard game setting |
| `Hard` | 18 x 12 | 5 colors | Larger board and more colors |

The selected difficulty is stored in a small helper class in `Main.java`. Then the values are used when creating the model and the graphical view:

```java
GameModel model = new GameModel(
        difficulty.cols,
        difficulty.rows,
        difficulty.colors
);

SwingView swingView = new SwingView(
        difficulty.cols,
        difficulty.rows,
        difficulty.tileSize,
        Main::startGame
);
```

A lower number of colors usually makes the game easier because there are more connected groups. A higher number of colors makes the game harder because matching groups are less common.

---

## 3. Key Interfaces and State Classes

### `GameObserver` (Observer)

```java
public interface GameObserver {
    void onGameStateChanged(GameSnapshot snapshot);
}
```

Any class that wants to observe the game implements this method.

---

### `GameSubject` (Subject)

```java
public interface GameSubject {
    void addObserver(GameObserver observer);
    void removeObserver(GameObserver observer);
    void notifyObservers();
}
```

`GameModel` implements `GameSubject`.

This means the model can:

- register observers,
- remove observers,
- notify all observers when the game state changes.

---

### `GameStatus` (game state enum)

`GameStatus` is used by the model to make the current game state clearer.

```java
public enum GameStatus {
    RUNNING,
    WON,
    LOST
}
```

- `RUNNING` means that the player can still make moves.
- `WON` means that the board has been completely cleared.
- `LOST` means that there are tiles left, but no valid moves remain.

The status is stored in `GameModel` and included in every `GameSnapshot`, so observers can show the current state without changing the model.

---

### `GameSnapshot` (immutable state value object)

All game state is passed to the observers in a single immutable object.

| Method                 | Returns              | Description                                      |
|------------------------|----------------------|--------------------------------------------------|
| `getCols()`            | `int`                | Grid width                                       |
| `getRows()`            | `int`                | Grid height                                      |
| `getTile(col, row)`    | `TileColor`          | Tile at that cell                                |
| `getScore()`           | `int`                | Current score                                    |
| `getNumColors()`       | `int`                | Difficulty level, meaning colors in play         |
| `isGameOver()`         | `boolean`            | True when the game has ended                     |
| `isBoardCleared()`     | `boolean`            | True when every cell is empty                    |
| `getRemainingTiles()`  | `int`                | Number of non-empty tiles left                   |
| `getStatus()`          | `GameStatus`         | Current status: `RUNNING`, `WON`, or `LOST`      |

The snapshot makes the observer pattern safer because the views do not need to modify or directly depend on the live model state.

---

## 4. The Three Views

### `SwingView` — graphical primary view

`SwingView` is the main graphical view of the game.

It:

- creates a `JFrame` with a `BoardPanel` and a status bar,
- implements `GameObserver`,
- repaints the board after every model update,
- shows score, number of colors and current game status,
- shows a final score dialog when the game ends,
- lets the player choose `Play again` or `Exit`,
- starts a new game if the player chooses `Play again`.

The graphical view does not change the model directly. It only displays the latest `GameSnapshot`.

The constructor used by `Main` is:

```java
SwingView swingView = new SwingView(
        difficulty.cols,
        difficulty.rows,
        difficulty.tileSize,
        Main::startGame
);
```

The last argument, `Main::startGame`, is used when the player clicks `Play again`. The old window is closed, and `startGame()` runs again. This also shows the difficulty dialog again.

### Pixel to grid mapping

The controller receives mouse positions in pixels. `SwingView` helps convert these positions into board coordinates:

```java
int col = view.pixelToCol(mouseEvent.getX());
int row = view.pixelToRow(mouseEvent.getY());
```

This is needed because Swing draws from top to bottom, while the model uses row `0` as the bottom row.

### Final score dialog

When the game is over, `SwingView` shows a dialog.

If the player wins:

```text
You won!
Final score: 2302
```

If the player loses:

```text
Game over!
Final score: 1583
Tiles left: 1
```

The player can then choose:

- `Play again`
- `Exit`

---

### `ConsoleView` — text/debug view

`ConsoleView` is a text-based view of the game.

It:

- prints the current game state after every update,
- can print the full ASCII grid or only the score/status line,
- shows the score, number of colors, remaining tiles and status.

It uses simple letter codes:

- `.` = EMPTY
- `R` = RED
- `B` = BLUE
- `G` = GREEN
- `Y` = YELLOW
- `P` = PURPLE

Example:

```text
──────────────────────────────
[ConsoleView] Score: 9 | Colors: 3 | Tiles left: 142 | Status: RUNNING
|R B G R G B G R B G R B G R B|
|G R B G B R B G R G B G R G R|
...
 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4
```

When the game ends, it can show:

```text
[ConsoleView] Score: 721 | Colors: 3 | Tiles left: 3 | Status: LOST
[ConsoleView] *** GAME OVER ***
```

---

### `CaptureView` — replay/analysis recorder

`CaptureView` is also an observer, but it does not display anything during normal play.

It:

- receives every `GameSnapshot`,
- saves each snapshot in a list,
- can be used for replay or later analysis.

Example usage:

```java
CaptureView capture = new CaptureView();
model.addObserver(capture);

List<GameSnapshot> history = capture.getHistory();
capture.replay(anotherView, 500);
capture.getLatest();
capture.size();
capture.clear();
```

This view is useful because it shows that observers do not have to be graphical. An observer can also record or analyze game states.

---

## 5. Registering and De-registering Observers

The model can register several observers:

```java
GameModel model = new GameModel(15, 10, 3);

SwingView gui = new SwingView(15, 10, 42);
ConsoleView console = new ConsoleView();
CaptureView capture = new CaptureView();

model.addObserver(gui);
model.addObserver(console);
model.addObserver(capture);
```

Observers can also be removed:

```java
model.removeObserver(console);
```

After removing `console`, the `SwingView` and `CaptureView` still work normally.

This works because:

- `GameModel` stores observers in an `ArrayList<GameObserver>`,
- `addObserver` checks for duplicates before adding,
- `removeObserver` removes only the selected observer,
- the model does not need to know which concrete view class is observing it.

This demonstrates the Subject-Observer pattern.

---

## 6. Writing a New Observer

To write a new observer, implement `GameObserver`.

Example:

```java
public class HighScoreLogger implements GameObserver {
    private int best = 0;

    @Override
    public void onGameStateChanged(GameSnapshot snapshot) {
        if (snapshot.isGameOver() && snapshot.getScore() > best) {
            best = snapshot.getScore();
            System.out.println("New best: " + best);
        }
    }
}
```

Register it:

```java
model.addObserver(new HighScoreLogger());
```

No changes are needed in `GameModel`, `SwingView`, `ConsoleView`, or `CaptureView`.

---

## 7. Controller and Input

The current controller is `MouseController`.

It listens for mouse clicks on the board and calls model methods.

Example:

```java
model.removeTiles(col, row);
```

This means the controller does not remove tiles itself. It only sends the player action to the model.

A different controller could use keyboard input instead:

```java
public class KeyboardController {
    private final GameModel model;
    private int cursorCol = 0;
    private int cursorRow = 0;

    public KeyboardController(GameModel model, JFrame frame) {
        this.model = model;

        frame.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_ENTER -> model.removeTiles(cursorCol, cursorRow);
                    case KeyEvent.VK_RIGHT -> cursorCol = Math.min(cursorCol + 1, model.getCols() - 1);
                    case KeyEvent.VK_LEFT -> cursorCol = Math.max(cursorCol - 1, 0);
                    case KeyEvent.VK_UP -> cursorRow = Math.min(cursorRow + 1, model.getRows() - 1);
                    case KeyEvent.VK_DOWN -> cursorRow = Math.max(cursorRow - 1, 0);
                }
            }
        });
    }
}
```

The model and views do not need to know how the input arrives. Only the controller changes.

---

## 8. Game Model Quick Reference

```java
GameModel model = new GameModel(cols, rows, numColors);

// Query state
model.getCols();          // grid width
model.getRows();          // grid height
model.getScore();         // current score
model.getNumColors();     // difficulty level, number of colors in play
model.isGameOver();       // true if the game has ended
model.isBoardCleared();   // true if every cell is empty
model.getStatus();        // RUNNING, WON, or LOST
model.hasValidMoves();    // true if at least one removable group exists
model.getTile(c, r);      // TileColor at (col, row)

// Actions called by controllers
boolean moved = model.removeTiles(col, row);
model.reset();
List<int[]> preview = model.previewGroup(col, row);

// Scoring formula
// Score gained from a removed group: (n - 2)^2
// where n is the number of removed tiles.
// Board clear bonus: +1000 points
```

### Game model responsibility

The model is responsible for the main game logic:

1. It stores the board in a `TileColor[][]` grid.
2. It stores the difficulty level using `numColors`.
3. It stores score, remaining tiles, game-over flags and `GameStatus`.
4. It accepts controlling events through `removeTiles(col, row)`.
5. It updates the board by removing groups, applying gravity and collapsing empty columns.
6. It checks whether the player has won or lost.
7. It notifies all registered observers after a successful state change.

### End condition logic

The model checks the end condition after every successful move:

- If no tiles remain, the status becomes `WON`.
- If tiles remain but `hasValidMoves()` returns `false`, the status becomes `LOST`.
- Otherwise, the status stays `RUNNING`.

---

## 9. Restart Flow

When the game ends, `SwingView` shows a final score dialog.

If the player clicks `Play again`:

1. The old game window is closed with `frame.dispose()`.
2. `restartAction.run()` is called.
3. In this program, `restartAction` is `Main::startGame`.
4. `startGame()` runs again.
5. The difficulty dialog is shown again.
6. A new `GameModel`, new views and a new controller are created.

This lets the player start a new game without restarting the whole program.

---

## 10. File Structure

```text
samegame/
├── Main.java                        Entry point — chooses difficulty and wires model, views, controller
├── MANUAL.md                        This manual
├── model/
│   ├── GameObserver.java            Observer interface
│   ├── GameSubject.java             Subject interface
│   ├── GameSnapshot.java            Immutable state value object
│   ├── GameStatus.java              Game state enum: RUNNING, WON, LOST
│   ├── TileColor.java               Tile color enum with AWT colors
│   └── GameModel.java               Game logic and observer list
├── view/
│   ├── SwingView.java               Graphical Swing observer and final score dialog
│   ├── ConsoleView.java             Text/debug console observer
│   └── CaptureView.java             Capture/replay recorder observer
└── controller/
    └── MouseController.java         Mouse input to model operations
```

---

## 11. Relation to the Project Requirements

The project required a design based on MVC, Subject-Observer or a mixture of both. This implementation uses both ideas.

- `GameModel` is the model and also the subject.
- `SwingView`, `ConsoleView` and `CaptureView` are observers.
- `MouseController` receives user input and calls model methods.
- The model notifies observers with an immutable `GameSnapshot`.

The project also required a game model that keeps track of difficulty, current game state, input events, end-game conditions and observer notifications.

| Requirement | Implementation |
|------------|----------------|
| Difficulty level | selected at startup as `Easy`, `Normal`, or `Hard`, then stored as `numColors` in `GameModel` |
| Current state | `grid`, `score`, `gameOver`, `boardCleared`, `GameStatus` |
| Controlling events | `removeTiles(col, row)` called by `MouseController` |
| Update state | remove group, update score, apply gravity, collapse columns |
| Winning condition | `GameStatus.WON` when all tiles are removed |
| Losing condition | `GameStatus.LOST` when no valid moves remain |
| Notify observers | `notifyObservers()` sends a `GameSnapshot` |
| Multiple views | `SwingView`, `ConsoleView`, and `CaptureView` |
| Register/de-register observers | `addObserver()` and `removeObserver()` in `GameModel` |
| Final score | `SwingView` shows a dialog when the game status becomes `WON` or `LOST` |
| Restart | `Play again` closes the old frame and calls `Main.startGame()` again |