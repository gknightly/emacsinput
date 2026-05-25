package net.woadwizard.search;

import net.woadwizard.config.Command;
import org.lwjgl.glfw.GLFW;

import java.util.function.BooleanSupplier;

/**
 * Classifies chat-screen Ctrl key actions.
 * The mixin keeps Minecraft side effects; this helper owns the decision tree.
 */
public final class ChatKeyHandler {

    private ChatKeyHandler() {}

    public static Result handleCtrlKey(int keyCode, int modifiers, boolean commandSuggestionsVisible) {
        return handleCtrlKey(
            keyCode,
            modifiers,
            commandSuggestionsVisible,
            Command.CTRL_P::isEnabled,
            Command.CTRL_N::isEnabled,
            Command.CTRL_R::isEnabled,
            Command.CTRL_S::isEnabled,
            Command.CTRL_G::isEnabled
        );
    }

    static Result handleCtrlKey(
        int keyCode,
        int modifiers,
        boolean commandSuggestionsVisible,
        BooleanSupplier previousEnabled,
        BooleanSupplier nextEnabled,
        BooleanSupplier searchBackwardEnabled,
        BooleanSupplier searchForwardEnabled,
        BooleanSupplier cancelEnabled
    ) {
        if ((modifiers & GLFW.GLFW_MOD_CONTROL) == 0) {
            return Result.none();
        }

        return switch (keyCode) {
            case GLFW.GLFW_KEY_P -> previousEnabled.getAsBoolean()
                ? Result.of(commandSuggestionsVisible ? Action.PREVIOUS_SUGGESTION : Action.PREVIOUS_HISTORY)
                : Result.none();
            case GLFW.GLFW_KEY_N -> nextEnabled.getAsBoolean()
                ? Result.of(commandSuggestionsVisible ? Action.NEXT_SUGGESTION : Action.NEXT_HISTORY)
                : Result.none();
            case GLFW.GLFW_KEY_R -> searchBackwardEnabled.getAsBoolean()
                ? Result.of(Action.ENTER_SEARCH_BACKWARD)
                : Result.none();
            case GLFW.GLFW_KEY_S -> searchForwardEnabled.getAsBoolean()
                ? Result.of(Action.ENTER_SEARCH_FORWARD)
                : Result.none();
            case GLFW.GLFW_KEY_G -> cancelEnabled.getAsBoolean()
                ? Result.of(Action.CANCEL_OR_CLOSE)
                : Result.none();
            default -> Result.none();
        };
    }

    public enum Action {
        NONE,
        PREVIOUS_SUGGESTION,
        NEXT_SUGGESTION,
        PREVIOUS_HISTORY,
        NEXT_HISTORY,
        ENTER_SEARCH_BACKWARD,
        ENTER_SEARCH_FORWARD,
        CANCEL_OR_CLOSE
    }

    public record Result(Action action) {
        private static Result of(Action action) {
            return new Result(action);
        }

        private static Result none() {
            return of(Action.NONE);
        }

        public boolean handled() {
            return action != Action.NONE;
        }
    }
}
