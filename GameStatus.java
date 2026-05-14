package samegame.model;

/**
 * Represents the current status of the SameGame model.
 *
 * RUNNING means that the player can still make moves.
 * WON means that the board was completely cleared.
 * LOST means that there are tiles left, but no valid moves remain.
 */
public enum GameStatus {
    RUNNING,
    WON,
    LOST
}