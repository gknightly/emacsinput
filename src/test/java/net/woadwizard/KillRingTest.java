package net.woadwizard;

import net.woadwizard.emacs.TextFieldAdapter;
import net.woadwizard.emacs.TextOperations;
import net.woadwizard.emacs.WidgetState;
import net.woadwizard.emacs.WordBoundary;

import java.util.ArrayList;
import java.util.List;

public final class KillRingTest {

    private final List<String> failures = new ArrayList<>();

    public static List<String> run() {
        KillRingTest test = new KillRingTest();
        test.runTests();
        return test.failures;
    }

    private void runTests() {
        testYankPopRequiresSameWidget();
        testYankPopRequiresOriginalYankedText();
    }

    private void testYankPopRequiresSameWidget() {
        KillRing.clearForTesting();
        KillRing.kill("one");
        KillRing.kill("two");

        FakeTextField first = new FakeTextField("say ", 4);
        TextOperations.yank(first);
        assertEquals("first yank inserts latest kill", "say two", first.getText());

        FakeTextField second = new FakeTextField("abc two", 7);
        TextOperations.yankPop(second);
        assertEquals("different widget with same cursor is unchanged", "abc two", second.getText());

        TextOperations.yankPop(first);
        assertEquals("original widget can still yank-pop", "say one", first.getText());
    }

    private void testYankPopRequiresOriginalYankedText() {
        KillRing.clearForTesting();
        KillRing.kill("one");
        KillRing.kill("two");

        FakeTextField field = new FakeTextField("say ", 4);
        TextOperations.yank(field);
        field.setText("say too");
        field.setCursor(7);

        TextOperations.yankPop(field);

        assertEquals("changed yank span is not replaced", "say too", field.getText());
    }

    private void assertEquals(String name, Object expected, Object actual) {
        if ((expected == null && actual != null) || (expected != null && !expected.equals(actual))) {
            failures.add(name + ": expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static final class FakeTextField implements TextFieldAdapter {
        private final WidgetState state = new WidgetState();
        private final Object widget = new Object();
        private String text;
        private int cursor;
        private int selectionStart;

        FakeTextField(String text, int cursor) {
            this.text = text;
            this.cursor = cursor;
            this.selectionStart = cursor;
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
        public void setText(String text) {
            this.text = text;
            this.cursor = Math.min(cursor, text.length());
            this.selectionStart = Math.min(selectionStart, text.length());
        }

        @Override
        public int getCursor() {
            return cursor;
        }

        @Override
        public void setCursor(int pos) {
            cursor = clamp(pos);
        }

        @Override
        public int getSelectionStart() {
            return selectionStart;
        }

        @Override
        public void setSelectionStart(int pos) {
            selectionStart = clamp(pos);
        }

        @Override
        public boolean hasSelection() {
            return cursor != selectionStart;
        }

        @Override
        public String getSelectedText() {
            int start = Math.min(cursor, selectionStart);
            int end = Math.max(cursor, selectionStart);
            return text.substring(start, end);
        }

        @Override
        public void insertText(String insertedText) {
            int start = Math.min(cursor, selectionStart);
            int end = Math.max(cursor, selectionStart);
            text = text.substring(0, start) + insertedText + text.substring(end);
            cursor = start + insertedText.length();
            selectionStart = cursor;
        }

        @Override
        public void deleteChars(int count) {
            if (count == 0) {
                return;
            }

            int start = count > 0 ? cursor : cursor + count;
            int end = count > 0 ? cursor + count : cursor;
            start = clamp(start);
            end = clamp(end);
            text = text.substring(0, start) + text.substring(end);
            cursor = start;
            selectionStart = cursor;
        }

        @Override
        public void moveChar(int direction, boolean selecting) {
            setCursor(cursor + direction);
            if (!selecting) {
                collapseSelection();
            }
        }

        @Override
        public void moveWord(int direction, boolean selecting) {
            setCursor(cursor + getWordOffset(direction));
            if (!selecting) {
                collapseSelection();
            }
        }

        @Override
        public void moveToStart(boolean selecting) {
            setCursor(0);
            if (!selecting) {
                collapseSelection();
            }
        }

        @Override
        public void moveToEnd(boolean selecting) {
            setCursor(text.length());
            if (!selecting) {
                collapseSelection();
            }
        }

        @Override
        public int getWordOffset(int direction) {
            return WordBoundary.findOffset(text, cursor, direction);
        }

        @Override
        public Object getWidget() {
            return widget;
        }

        private int clamp(int pos) {
            return Math.max(0, Math.min(pos, text.length()));
        }
    }
}
