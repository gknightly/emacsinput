package net.woadwizard.emacs;

import net.woadwizard.UndoManager;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class TextInputEventHandlerTest {

    private final List<String> failures = new ArrayList<>();

    public static List<String> run() {
        TextInputEventHandlerTest test = new TextInputEventHandlerTest();
        test.runTests();
        return test.failures;
    }

    private void runTests() {
        testHandledKeyIsConsumedWithoutUndoRecording();
        testDeleteKeyRecordsUndoWhenNotHandled();
        testNonDeleteKeyDoesNotRecordUndoWhenNotHandled();
        testBlockedCharIsConsumedWithoutUndoRecording();
        testAllowedCharRecordsInsertUndo();
    }

    private void testHandledKeyIsConsumedWithoutUndoRecording() {
        FakeTextField field = new FakeTextField("abc", 3);

        boolean consumed = TextInputEventHandler.finishKeyPress(
            field, GLFW.GLFW_KEY_BACKSPACE, EmacsKeyHandler.Result.HANDLED);

        assertTrue("handled key is consumed", consumed);
        assertNull("handled key does not record vanilla delete undo",
            UndoManager.undo(field.getWidget(), field.getState(), "ab", 2));
        UndoManager.clear(field.getWidget());
    }

    private void testDeleteKeyRecordsUndoWhenNotHandled() {
        FakeTextField field = new FakeTextField("abc", 3);

        boolean consumed = TextInputEventHandler.finishKeyPress(
            field, GLFW.GLFW_KEY_BACKSPACE, EmacsKeyHandler.Result.NOT_HANDLED);

        assertFalse("unhandled delete is not consumed", consumed);
        UndoManager.UndoState undo = UndoManager.undo(field.getWidget(), field.getState(), "ab", 2);
        assertNotNull("unhandled delete records undo state", undo);
        assertEquals("delete undo text", "abc", undo.text);
        assertEquals("delete undo cursor", 3, undo.cursorPos);
        UndoManager.clear(field.getWidget());
    }

    private void testNonDeleteKeyDoesNotRecordUndoWhenNotHandled() {
        FakeTextField field = new FakeTextField("abc", 1);

        boolean consumed = TextInputEventHandler.finishKeyPress(
            field, GLFW.GLFW_KEY_LEFT, EmacsKeyHandler.Result.PASS_THROUGH);

        assertFalse("pass-through key is not consumed", consumed);
        assertNull("non-delete pass-through key does not record undo",
            UndoManager.undo(field.getWidget(), field.getState(), "abc", 1));
        UndoManager.clear(field.getWidget());
    }

    private void testBlockedCharIsConsumedWithoutUndoRecording() {
        FakeTextField field = new FakeTextField("a", 1);

        boolean blocked = TextInputEventHandler.finishCharTyped(field, 'b', true);

        assertTrue("blocked char is consumed", blocked);
        assertNull("blocked char does not record insert undo",
            UndoManager.undo(field.getWidget(), field.getState(), "a", 1));
        UndoManager.clear(field.getWidget());
    }

    private void testAllowedCharRecordsInsertUndo() {
        FakeTextField field = new FakeTextField("a", 1);

        boolean blocked = TextInputEventHandler.finishCharTyped(field, 'b', false);

        assertFalse("allowed char is not blocked", blocked);
        UndoManager.UndoState undo = UndoManager.undo(field.getWidget(), field.getState(), "ab", 2);
        assertNotNull("allowed char records insert undo", undo);
        assertEquals("insert undo text", "a", undo.text);
        assertEquals("insert undo cursor", 1, undo.cursorPos);
        UndoManager.clear(field.getWidget());
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

    private void assertNull(String name, Object actual) {
        if (actual != null) {
            failures.add(name + ": expected null but was <" + actual + ">");
        }
    }

    private void assertNotNull(String name, Object actual) {
        if (actual == null) {
            failures.add(name + ": expected non-null value");
        }
    }

    private void assertEquals(String name, Object expected, Object actual) {
        if ((expected == null && actual != null) || (expected != null && !expected.equals(actual))) {
            failures.add(name + ": expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static final class FakeTextField implements TextFieldAdapter {
        private final WidgetState state = new WidgetState();
        private final Object widget = new Object();
        private final String text;
        private final int cursor;

        FakeTextField(String text, int cursor) {
            this.text = text;
            this.cursor = cursor;
        }

        @Override
        public WidgetState getState() {
            return state;
        }

        @Override
        public String getText() {
            return text;
        }

        @Override
        public void setText(String text) {}

        @Override
        public int getCursor() {
            return cursor;
        }

        @Override
        public void setCursor(int pos) {}

        @Override
        public int getSelectionStart() {
            return cursor;
        }

        @Override
        public void setSelectionStart(int pos) {}

        @Override
        public boolean hasSelection() {
            return false;
        }

        @Override
        public String getSelectedText() {
            return "";
        }

        @Override
        public void insertText(String text) {}

        @Override
        public void deleteChars(int count) {}

        @Override
        public void moveChar(int direction, boolean selecting) {}

        @Override
        public void moveWord(int direction, boolean selecting) {}

        @Override
        public void moveToStart(boolean selecting) {}

        @Override
        public void moveToEnd(boolean selecting) {}

        @Override
        public int getWordOffset(int direction) {
            return 0;
        }

        @Override
        public Object getWidget() {
            return widget;
        }
    }
}
