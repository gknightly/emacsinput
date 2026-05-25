package net.woadwizard.emacs;

import net.woadwizard.UndoManager;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared event handling for Minecraft text input widgets.
 * Mixins adapt widget-specific events to this class, while editing behavior
 * stays in the common Emacs/text-operation layer.
 */
public final class TextInputEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(TextInputEventHandler.class);

    private TextInputEventHandler() {}

    /**
     * Handle a key press against a text field.
     * @return true when the caller should consume the key event
     */
    public static boolean handleKeyPress(TextFieldAdapter field, int keyCode, int modifiers) {
        EmacsKeyHandler.Result result = EmacsKeyHandler.handleKeyPress(field, keyCode, modifiers);
        return finishKeyPress(field, keyCode, result);
    }

    /**
     * Handle a typed character against a text field.
     * @return true when the caller should block the typed character
     */
    public static boolean handleCharTyped(TextFieldAdapter field, int codepoint, int modifiers) {
        boolean shouldBlock = EmacsKeyHandler.shouldBlockChar(modifiers);
        return finishCharTyped(field, codepoint, shouldBlock);
    }

    static boolean finishKeyPress(TextFieldAdapter field, int keyCode, EmacsKeyHandler.Result result) {
        if (result == EmacsKeyHandler.Result.HANDLED) {
            return true;
        }

        if (isDeleteKey(keyCode)) {
            UndoManager.recordStateForDelete(
                field.getWidget(), field.getState(), field.getText(), field.getCursor());
        }
        return false;
    }

    static boolean finishCharTyped(TextFieldAdapter field, int codepoint, boolean shouldBlock) {
        if (shouldBlock) {
            LOGGER.debug("Blocking modified character: codepoint={}", codepoint);
            return true;
        }

        UndoManager.recordStateForInsert(
            field.getWidget(), field.getState(), field.getText(), field.getCursor());
        return false;
    }

    private static boolean isDeleteKey(int keyCode) {
        return keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE;
    }
}
