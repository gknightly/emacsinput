package net.woadwizard.emacs;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class SignLineNavigationTest {

    private final List<String> failures = new ArrayList<>();

    public static List<String> run() {
        SignLineNavigationTest test = new SignLineNavigationTest();
        test.runTests();
        return test.failures;
    }

    private void runTests() {
        testPreviousWrapsToLastLine();
        testNextWrapsToFirstLine();
        testIgnoresKeysWithoutControl();
        testRespectsDisabledCommands();
        testIgnoresOtherControlKeys();
    }

    private void testPreviousWrapsToLastLine() {
        SignLineNavigation.Result result = SignLineNavigation.handle(
            GLFW.GLFW_KEY_P, GLFW.GLFW_MOD_CONTROL, 0, () -> true, () -> true);

        assertTrue("C-p is handled", result.handled());
        assertEquals("C-p wraps from first line to last", 3, result.line());
    }

    private void testNextWrapsToFirstLine() {
        SignLineNavigation.Result result = SignLineNavigation.handle(
            GLFW.GLFW_KEY_N, GLFW.GLFW_MOD_CONTROL, 3, () -> true, () -> true);

        assertTrue("C-n is handled", result.handled());
        assertEquals("C-n wraps from last line to first", 0, result.line());
    }

    private void testIgnoresKeysWithoutControl() {
        SignLineNavigation.Result result = SignLineNavigation.handle(
            GLFW.GLFW_KEY_N, 0, 2, () -> true, () -> true);

        assertFalse("N without control is not handled", result.handled());
        assertEquals("N without control preserves line", 2, result.line());
    }

    private void testRespectsDisabledCommands() {
        SignLineNavigation.Result previous = SignLineNavigation.handle(
            GLFW.GLFW_KEY_P, GLFW.GLFW_MOD_CONTROL, 1, () -> false, () -> true);
        SignLineNavigation.Result next = SignLineNavigation.handle(
            GLFW.GLFW_KEY_N, GLFW.GLFW_MOD_CONTROL, 1, () -> true, () -> false);

        assertFalse("disabled C-p is not handled", previous.handled());
        assertEquals("disabled C-p preserves line", 1, previous.line());
        assertFalse("disabled C-n is not handled", next.handled());
        assertEquals("disabled C-n preserves line", 1, next.line());
    }

    private void testIgnoresOtherControlKeys() {
        SignLineNavigation.Result result = SignLineNavigation.handle(
            GLFW.GLFW_KEY_F, GLFW.GLFW_MOD_CONTROL, 2, () -> true, () -> true);

        assertFalse("other control key is not handled", result.handled());
        assertEquals("other control key preserves line", 2, result.line());
    }

    private void assertTrue(String name, boolean actual) {
        if (!actual) {
            failures.add(name + ": expected true");
        }
    }

    private void assertFalse(String name, boolean actual) {
        if (actual) {
            failures.add(name + ": expected false");
        }
    }

    private void assertEquals(String name, Object expected, Object actual) {
        if ((expected == null && actual != null) || (expected != null && !expected.equals(actual))) {
            failures.add(name + ": expected <" + expected + "> but was <" + actual + ">");
        }
    }
}
