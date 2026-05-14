package samegame.model;

import java.awt.Color;

/**
 * Enum representing each possible tile color in SameGame.
 *
 * The number of colors actively used is controlled by the difficulty setting;
 * only the first {@code numColors} values of this enum are put on the board.
 * {@link #EMPTY} is a special sentinel meaning "no tile at this cell".
 *
 * Each constant holds a corresponding AWT {@link Color} used by the Swing view.
 */
public enum TileColor {

    EMPTY  (Color.LIGHT_GRAY),   // cell is empty (tile was removed)
    RED    (new Color(220, 50,  50)),
    BLUE   (new Color(50,  100, 220)),
    GREEN  (new Color(50,  180, 80)),
    YELLOW (new Color(230, 200, 40)),
    PURPLE (new Color(150, 60,  200));

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    /** The AWT color used when painting this tile in the graphical view. */
    private final Color awtColor;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * @param awtColor the AWT color associated with this tile type.
     */
    TileColor(Color awtColor) {
        this.awtColor = awtColor;
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    /**
     * Returns the AWT {@link Color} for use in the Swing rendering code.
     *
     * @return the AWT color; never {@code null}.
     */
    public Color getAwtColor() {
        return awtColor;
    }

    /**
     * Returns {@code true} if this constant represents an empty/removed cell.
     *
     * @return {@code true} for {@link #EMPTY}, {@code false} for all others.
     */
    public boolean isEmpty() {
        return this == EMPTY;
    }
}
