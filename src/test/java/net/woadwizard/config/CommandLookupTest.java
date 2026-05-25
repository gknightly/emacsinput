package net.woadwizard.config;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class CommandLookupTest {

    private final List<String> failures = new ArrayList<>();

    public static List<String> run() {
        CommandLookupTest test = new CommandLookupTest();
        test.runTests();
        return test.failures;
    }

    private void runTests() {
        testSlashDistinguishesShift();
        testShiftFallsBackToSelectionCommand();
        testAltShiftFallsBackToSelectionCommand();
        testShiftDoesNotAliasEditingCommands();
    }

    private void testSlashDistinguishesShift() {
        assertEquals("C-/ resolves to undo",
            Command.CTRL_SLASH, Command.fromCtrlKey(GLFW.GLFW_KEY_SLASH, false));
        assertEquals("C-S-/ resolves to redo",
            Command.CTRL_SHIFT_SLASH, Command.fromCtrlKey(GLFW.GLFW_KEY_SLASH, true));
    }

    private void testShiftFallsBackToSelectionCommand() {
        assertEquals("C-S-f still resolves to forward-char for selection",
            Command.CTRL_F, Command.fromCtrlKey(GLFW.GLFW_KEY_F, true));
    }

    private void testAltShiftFallsBackToSelectionCommand() {
        assertEquals("M-S-f still resolves to forward-word for selection",
            Command.META_F, Command.fromAltKey(GLFW.GLFW_KEY_F, true));
    }

    private void testShiftDoesNotAliasEditingCommands() {
        assertEquals("C-S-k does not resolve to kill-line", null,
            Command.fromCtrlKey(GLFW.GLFW_KEY_K, true));
        assertEquals("C-S-y does not resolve to yank", null,
            Command.fromCtrlKey(GLFW.GLFW_KEY_Y, true));
        assertEquals("M-S-d does not resolve to kill-word", null,
            Command.fromAltKey(GLFW.GLFW_KEY_D, true));
        assertEquals("M-S-u does not resolve to uppercase-word", null,
            Command.fromAltKey(GLFW.GLFW_KEY_U, true));
    }

    private void assertEquals(String name, Object expected, Object actual) {
        if ((expected == null && actual != null) || (expected != null && !expected.equals(actual))) {
            failures.add(name + ": expected <" + expected + "> but was <" + actual + ">");
        }
    }
}
