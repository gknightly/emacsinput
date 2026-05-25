package net.woadwizard.search;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class ChatKeyHandlerTest {

    private final List<String> failures = new ArrayList<>();

    public static List<String> run() {
        ChatKeyHandlerTest test = new ChatKeyHandlerTest();
        test.runTests();
        return test.failures;
    }

    private void runTests() {
        testIgnoresNonControlKeys();
        testPreviousChoosesSuggestionsWhenVisible();
        testPreviousChoosesHistoryWhenSuggestionsHidden();
        testNextChoosesSuggestionsWhenVisible();
        testNextChoosesHistoryWhenSuggestionsHidden();
        testSearchEntryActions();
        testCancelAction();
        testRespectsDisabledCommands();
    }

    private void testIgnoresNonControlKeys() {
        ChatKeyHandler.Result result = handle(GLFW.GLFW_KEY_P, 0, true, true, true, true, true, true);

        assertFalse("non-control key is not handled", result.handled());
        assertEquals("non-control key has no action", ChatKeyHandler.Action.NONE, result.action());
    }

    private void testPreviousChoosesSuggestionsWhenVisible() {
        ChatKeyHandler.Result result = handle(GLFW.GLFW_KEY_P, GLFW.GLFW_MOD_CONTROL, true, true, true, true, true, true);

        assertEquals("C-p chooses previous suggestion",
            ChatKeyHandler.Action.PREVIOUS_SUGGESTION, result.action());
    }

    private void testPreviousChoosesHistoryWhenSuggestionsHidden() {
        ChatKeyHandler.Result result = handle(GLFW.GLFW_KEY_P, GLFW.GLFW_MOD_CONTROL, false, true, true, true, true, true);

        assertEquals("C-p chooses previous history",
            ChatKeyHandler.Action.PREVIOUS_HISTORY, result.action());
    }

    private void testNextChoosesSuggestionsWhenVisible() {
        ChatKeyHandler.Result result = handle(GLFW.GLFW_KEY_N, GLFW.GLFW_MOD_CONTROL, true, true, true, true, true, true);

        assertEquals("C-n chooses next suggestion",
            ChatKeyHandler.Action.NEXT_SUGGESTION, result.action());
    }

    private void testNextChoosesHistoryWhenSuggestionsHidden() {
        ChatKeyHandler.Result result = handle(GLFW.GLFW_KEY_N, GLFW.GLFW_MOD_CONTROL, false, true, true, true, true, true);

        assertEquals("C-n chooses next history",
            ChatKeyHandler.Action.NEXT_HISTORY, result.action());
    }

    private void testSearchEntryActions() {
        ChatKeyHandler.Result backward = handle(GLFW.GLFW_KEY_R, GLFW.GLFW_MOD_CONTROL, false, true, true, true, true, true);
        ChatKeyHandler.Result forward = handle(GLFW.GLFW_KEY_S, GLFW.GLFW_MOD_CONTROL, false, true, true, true, true, true);

        assertEquals("C-r enters backward search",
            ChatKeyHandler.Action.ENTER_SEARCH_BACKWARD, backward.action());
        assertEquals("C-s enters forward search",
            ChatKeyHandler.Action.ENTER_SEARCH_FORWARD, forward.action());
    }

    private void testCancelAction() {
        ChatKeyHandler.Result result = handle(GLFW.GLFW_KEY_G, GLFW.GLFW_MOD_CONTROL, false, true, true, true, true, true);

        assertEquals("C-g cancels or closes chat",
            ChatKeyHandler.Action.CANCEL_OR_CLOSE, result.action());
    }

    private void testRespectsDisabledCommands() {
        assertEquals("disabled C-p is ignored", ChatKeyHandler.Action.NONE,
            handle(GLFW.GLFW_KEY_P, GLFW.GLFW_MOD_CONTROL, false, false, true, true, true, true).action());
        assertEquals("disabled C-n is ignored", ChatKeyHandler.Action.NONE,
            handle(GLFW.GLFW_KEY_N, GLFW.GLFW_MOD_CONTROL, false, true, false, true, true, true).action());
        assertEquals("disabled C-r is ignored", ChatKeyHandler.Action.NONE,
            handle(GLFW.GLFW_KEY_R, GLFW.GLFW_MOD_CONTROL, false, true, true, false, true, true).action());
        assertEquals("disabled C-s is ignored", ChatKeyHandler.Action.NONE,
            handle(GLFW.GLFW_KEY_S, GLFW.GLFW_MOD_CONTROL, false, true, true, true, false, true).action());
        assertEquals("disabled C-g is ignored", ChatKeyHandler.Action.NONE,
            handle(GLFW.GLFW_KEY_G, GLFW.GLFW_MOD_CONTROL, false, true, true, true, true, false).action());
    }

    private ChatKeyHandler.Result handle(
        int keyCode,
        int modifiers,
        boolean commandSuggestionsVisible,
        boolean previousEnabled,
        boolean nextEnabled,
        boolean searchBackwardEnabled,
        boolean searchForwardEnabled,
        boolean cancelEnabled
    ) {
        return ChatKeyHandler.handleCtrlKey(
            keyCode,
            modifiers,
            commandSuggestionsVisible,
            () -> previousEnabled,
            () -> nextEnabled,
            () -> searchBackwardEnabled,
            () -> searchForwardEnabled,
            () -> cancelEnabled
        );
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
