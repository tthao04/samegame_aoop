package samegame.controller;

/**
 * General interface for input controllers in SameGame.
 *
 * This makes it possible to change the input method without changing
 * the model or the view.
 *
 * For example:
 * - MouseController can use mouse clicks.
 * - KeyboardController can use keyboard buttons.
 * - Another controller could use network input later.
 */
public interface InputController {

 /**
 * Starts listening for input.
     */
    void attach();

/**
  * Stops listening for input.
     */
    void detach();
}