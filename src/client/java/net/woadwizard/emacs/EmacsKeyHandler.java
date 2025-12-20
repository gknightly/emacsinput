package net.woadwizard.emacs;

import net.woadwizard.config.Command;
import net.woadwizard.config.ConfigHelper;
import net.woadwizard.config.ModConfig;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Shared Emacs-style key handling logic.
 * Works with any TextFieldAdapter implementation.
 *
 * Most command actions are defined in the Command enum.
 * This handler manages special cases like C-x prefix and Escape.
 */
public final class EmacsKeyHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmacsKeyHandler.class);

    private EmacsKeyHandler() {}

    /**
     * Result of key handling.
     */
    public enum Result {
        HANDLED,
        NOT_HANDLED,
        PASS_THROUGH,
    }

    /**
     * Handle a key press event.
     * @return Result indicating whether the key was handled
     */
    public static Result handleKeyPress(
        TextFieldAdapter field,
        int keyCode,
        int modifiers
    ) {
        Objects.requireNonNull(field, "field must not be null");

        if (!ConfigHelper.isEnabled()) {
            return Result.NOT_HANDLED;
        }

        WidgetState state = field.getState();
        boolean ctrlHeld = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        boolean altHeld = (modifiers & GLFW.GLFW_MOD_ALT) != 0;
        boolean shiftHeld = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
        boolean selecting = shiftHeld || state.isMarkActive();

        // Handle C-x prefix for C-x C-x sequence
        if (state.isCxPrefixActive()) {
            state.resetCxPrefix();
            if (ctrlHeld && keyCode == GLFW.GLFW_KEY_X && Command.CTRL_X_CTRL_X.isEnabled()) {
                LOGGER.trace("C-x C-x: exchanging point and mark");
                int curPos = field.getCursor();
                int selectPos = field.getSelectionStart();
                field.setCursor(selectPos);
                field.setSelectionStart(curPos);
                state.setMark();
                return Result.HANDLED;
            }
        }

        // Handle Escape: clear selection/mark if active, otherwise pass through
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (state.isMarkActive() || field.hasSelection()) {
                LOGGER.trace("Escape: clearing selection/mark");
                state.deactivateMark();
                field.collapseSelection();
                return Result.HANDLED;
            }
            return Result.NOT_HANDLED;
        }

        // Handle C-x prefix initiation
        if (ctrlHeld && keyCode == GLFW.GLFW_KEY_X && Command.CTRL_X_CTRL_X.isEnabled()) {
            LOGGER.trace("C-x: waiting for next key");
            state.setCxPrefix(true);
            return Result.HANDLED;
        }

        // Try Ctrl commands
        if (ctrlHeld && ConfigHelper.isCtrlEnabled()) {
            Command cmd = Command.fromCtrlKey(keyCode);
            if (cmd != null && cmd.isEnabled() && cmd.hasAction()) {
                LOGGER.trace("{}: executing", cmd.getName());
                Command.Result cmdResult = cmd.execute(field, selecting);
                return toHandlerResult(cmdResult);
            }
        }

        // Try Alt commands
        if (altHeld && ConfigHelper.isAltEnabled()) {
            Command cmd = Command.fromAltKey(keyCode);
            if (cmd != null && cmd.isEnabled() && cmd.hasAction()) {
                LOGGER.trace("{}: executing", cmd.getName());
                Command.Result cmdResult = cmd.execute(field, selecting);
                return toHandlerResult(cmdResult);
            }
        }

        return Result.NOT_HANDLED;
    }

    /**
     * Convert Command.Result to EmacsKeyHandler.Result.
     */
    private static Result toHandlerResult(Command.Result cmdResult) {
        if (cmdResult == null) {
            return Result.NOT_HANDLED;
        }
        return switch (cmdResult) {
            case HANDLED -> Result.HANDLED;
            case PASS_THROUGH -> Result.PASS_THROUGH;
        };
    }

    /**
     * Handle character typed - blocks Alt-modified characters based on config.
     * Use this version when keyCode is not available (e.g., in charTyped events).
     * @return true if the character should be blocked
     */
    public static boolean shouldBlockChar(int modifiers) {
        return shouldBlockChar(modifiers, -1);
    }

    /**
     * Handle character typed - blocks Alt-modified characters based on config.
     * @param keyCode the key code, or -1 if not available
     * @return true if the character should be blocked
     */
    public static boolean shouldBlockChar(int modifiers, int keyCode) {
        if (!ConfigHelper.isEnabled()) {
            return false;
        }

        boolean altHeld = (modifiers & GLFW.GLFW_MOD_ALT) != 0;
        if (!altHeld) {
            return false;
        }

        ModConfig.AltKeyBehavior behavior = ConfigHelper.getAltKeyBehavior();
        return switch (behavior) {
            case BLOCK_ALL -> true;
            case BLOCK_WHEN_BOUND -> {
                if (keyCode < 0) {
                    yield true;
                }
                yield Command.isAltKeyBound(keyCode);
            }
        };
    }
}
