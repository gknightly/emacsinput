package net.woadwizard.emacs;

import net.woadwizard.config.Command;
import org.lwjgl.glfw.GLFW;

import java.util.function.BooleanSupplier;

/**
 * Sign editors use C-p/C-n to move between the four sign lines.
 */
public final class SignLineNavigation {

    private static final int SIGN_LINE_COUNT = 4;

    private SignLineNavigation() {}

    public static Result handle(int keyCode, int modifiers, int currentLine) {
        return handle(keyCode, modifiers, currentLine, Command.CTRL_P::isEnabled, Command.CTRL_N::isEnabled);
    }

    static Result handle(
        int keyCode,
        int modifiers,
        int currentLine,
        BooleanSupplier previousEnabled,
        BooleanSupplier nextEnabled
    ) {
        if ((modifiers & GLFW.GLFW_MOD_CONTROL) == 0) {
            return Result.notHandled(currentLine);
        }

        if (keyCode == GLFW.GLFW_KEY_P && previousEnabled.getAsBoolean()) {
            return Result.handled(Math.floorMod(currentLine - 1, SIGN_LINE_COUNT));
        }

        if (keyCode == GLFW.GLFW_KEY_N && nextEnabled.getAsBoolean()) {
            return Result.handled((currentLine + 1) % SIGN_LINE_COUNT);
        }

        return Result.notHandled(currentLine);
    }

    public record Result(boolean handled, int line) {
        private static Result handled(int line) {
            return new Result(true, line);
        }

        private static Result notHandled(int line) {
            return new Result(false, line);
        }
    }
}
