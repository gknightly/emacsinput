package net.woadwizard.search;

import net.minecraft.client.gui.components.EditBox;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles key events during history search mode.
 * Extracted from ChatScreenMixin to reduce complexity.
 */
public final class SearchModeHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchModeHandler.class);

    private SearchModeHandler() {}

    /**
     * Result of handling a search mode key.
     */
    public enum Result {
        /** Key was handled, consume the event */
        HANDLED,
        /** Key was handled, but don't consume (let default processing continue) */
        ACCEPT_AND_CONTINUE,
        /** Key was not handled in search mode */
        NOT_HANDLED
    }

    /**
     * Handle a key press while in search mode.
     *
     * @param keyCode the GLFW key code
     * @param ctrlHeld whether Ctrl is held
     * @param altHeld whether Alt is held
     * @param editBox the EditBox to sync state to
     * @param search the active HistorySearch instance
     * @return result indicating how the key was handled
     */
    public static Result handleKey(
        int keyCode,
        boolean ctrlHeld,
        boolean altHeld,
        EditBox editBox,
        HistorySearch search
    ) {
        switch (keyCode) {
            case GLFW.GLFW_KEY_ESCAPE -> {
                LOGGER.debug("Escape: cancelling search");
                SearchController.cancelAndExit(editBox, search);
                return Result.HANDLED;
            }
            case GLFW.GLFW_KEY_TAB -> {
                LOGGER.debug("Tab: accepting search match");
                SearchController.acceptAndExit(editBox, search);
                return Result.HANDLED;
            }
            case GLFW.GLFW_KEY_J -> {
                if (ctrlHeld) {
                    LOGGER.debug("C-j: accepting search match");
                    SearchController.acceptAndExit(editBox, search);
                    return Result.HANDLED;
                }
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                LOGGER.debug("Enter: exiting search, letting normal handler submit");
                SearchController.acceptAndExit(editBox, search);
                // Don't consume - let normal Enter handling proceed
                return Result.ACCEPT_AND_CONTINUE;
            }
            case GLFW.GLFW_KEY_BACKSPACE -> {
                LOGGER.debug("Backspace: removing char from search query");
                search.deleteFromQuery();
                SearchController.syncToEditBox(editBox, search.getState());
                return Result.HANDLED;
            }
            case GLFW.GLFW_KEY_G -> {
                if (ctrlHeld) {
                    LOGGER.debug("C-g: cancelling search");
                    SearchController.cancelAndExit(editBox, search);
                    return Result.HANDLED;
                }
            }
            case GLFW.GLFW_KEY_R -> {
                if (ctrlHeld) {
                    if (search.getState().query().isEmpty() && search.hasLastQuery()) {
                        LOGGER.debug("C-r: reusing last search query");
                        search.reuseLastQuery();
                    } else {
                        LOGGER.debug("C-r: cycling to previous match");
                        search.cyclePrevious();
                    }
                    SearchController.syncToEditBox(editBox, search.getState());
                    return Result.HANDLED;
                }
            }
            case GLFW.GLFW_KEY_S -> {
                if (ctrlHeld) {
                    LOGGER.debug("C-s: cycling to next match");
                    search.cycleNext();
                    SearchController.syncToEditBox(editBox, search.getState());
                    return Result.HANDLED;
                }
            }
            // Navigation keys: accept match, then let navigation proceed
            case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_HOME, GLFW.GLFW_KEY_END -> {
                LOGGER.debug("Navigation key: accepting and exiting search");
                SearchController.acceptAndExit(editBox, search);
                return Result.ACCEPT_AND_CONTINUE;
            }
            // Emacs navigation keys
            case GLFW.GLFW_KEY_F, GLFW.GLFW_KEY_B, GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_E -> {
                if (ctrlHeld || altHeld) {
                    LOGGER.debug("Emacs nav key: accepting and exiting search");
                    SearchController.acceptAndExit(editBox, search);
                    return Result.ACCEPT_AND_CONTINUE;
                }
            }
            // Kill/edit keys: accept match, then let operation proceed
            case GLFW.GLFW_KEY_K, GLFW.GLFW_KEY_U, GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_D, GLFW.GLFW_KEY_Y, GLFW.GLFW_KEY_T -> {
                if (ctrlHeld || altHeld) {
                    LOGGER.debug("Kill/edit key: accepting and exiting search");
                    SearchController.acceptAndExit(editBox, search);
                    return Result.ACCEPT_AND_CONTINUE;
                }
            }
        }
        return Result.NOT_HANDLED;
    }

    /**
     * Check if search mode should exit and accept on this key.
     * Used to determine if we need to update historyPos after handling.
     */
    public static boolean isAcceptKey(int keyCode, boolean ctrlHeld, boolean altHeld) {
        return switch (keyCode) {
            case GLFW.GLFW_KEY_TAB -> true;
            case GLFW.GLFW_KEY_J -> ctrlHeld;
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> true;
            case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_HOME, GLFW.GLFW_KEY_END -> true;
            case GLFW.GLFW_KEY_F, GLFW.GLFW_KEY_B, GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_E -> ctrlHeld || altHeld;
            case GLFW.GLFW_KEY_K, GLFW.GLFW_KEY_U, GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_D, GLFW.GLFW_KEY_Y, GLFW.GLFW_KEY_T -> ctrlHeld || altHeld;
            default -> false;
        };
    }
}
