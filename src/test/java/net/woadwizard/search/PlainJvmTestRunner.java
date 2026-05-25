package net.woadwizard.search;

import net.woadwizard.UndoManager;
import net.woadwizard.config.CommandLookupTest;
import net.woadwizard.emacs.GraphemeUtils;
import net.woadwizard.emacs.SignLineNavigationTest;
import net.woadwizard.emacs.TextFieldAdapter;
import net.woadwizard.emacs.TextInputEventHandlerTest;
import net.woadwizard.emacs.TextOperations;
import net.woadwizard.emacs.WidgetState;
import net.woadwizard.emacs.WordBoundary;

import java.util.ArrayList;
import java.util.List;

public final class PlainJvmTestRunner {

    private final List<String> failures = new ArrayList<>();

    public static void main(String[] args) {
        PlainJvmTestRunner runner = new PlainJvmTestRunner();
        runner.run();
    }

    private void run() {
        testWordBoundaryOffsets();
        testGraphemeBoundaries();
        testUndoAmalgamationAndRedo();
        testTransposeCharacters();
        testCaseConversion();
        testHistorySearchCaseInsensitiveMatching();
        testHistorySearchCaseSensitiveMatching();
        testHistorySearchReusesLastQuery();
        failures.addAll(ChatKeyHandlerTest.run());
        failures.addAll(ChatSearchUiTest.run());
        failures.addAll(CommandLookupTest.run());
        failures.addAll(SignLineNavigationTest.run());
        failures.addAll(TextInputEventHandlerTest.run());

        if (!failures.isEmpty()) {
            failures.forEach(System.err::println);
            throw new AssertionError(failures.size() + " plain JVM test(s) failed");
        }

        System.out.println("Plain JVM tests passed: " + getClass().getSimpleName());
    }

    private void testWordBoundaryOffsets() {
        String text = "alpha  beta\tgamma";

        assertEquals("forward word at start", 5, WordBoundary.findOffset(text, 0, 1));
        assertEquals("forward skips whitespace", 6, WordBoundary.findOffset(text, 5, 1));
        assertEquals("backward word at end", -5, WordBoundary.findOffset(text, text.length(), -1));
        assertEquals("backward skips whitespace", -5, WordBoundary.findOffset(text, 12, -1));
        assertEquals("empty text has no offset", 0, WordBoundary.findOffset("", 0, 1));
    }

    private void testGraphemeBoundaries() {
        String text = "a\uD83D\uDE00e\u0301";

        assertEquals("ascii next boundary", 1, GraphemeUtils.nextGraphemeBoundary(text, 0));
        assertEquals("surrogate pair next boundary", 3, GraphemeUtils.nextGraphemeBoundary(text, 1));
        assertEquals("combining sequence next boundary", text.length(), GraphemeUtils.nextGraphemeBoundary(text, 3));
        assertEquals("surrogate pair previous boundary", 1, GraphemeUtils.previousGraphemeBoundary(text, 3));
        assertEquals("combining sequence previous boundary", 3, GraphemeUtils.previousGraphemeBoundary(text, text.length()));
    }

    private void testUndoAmalgamationAndRedo() {
        Object widget = new Object();
        WidgetState state = new WidgetState();

        UndoManager.recordStateForInsert(widget, state, "", 0);
        UndoManager.recordStateForInsert(widget, state, "a", 1);
        UndoManager.recordStateForInsert(widget, state, "ab", 2);

        UndoManager.UndoState undo = UndoManager.undo(widget, state, "abc", 3);
        assertNotNull("amalgamated insert can undo", undo);
        assertEquals("amalgamated insert restores boundary text", "", undo.text);
        assertEquals("amalgamated insert restores boundary cursor", 0, undo.cursorPos);

        UndoManager.UndoState redo = UndoManager.redo(widget, state, "", 0);
        assertNotNull("redo restores undone insert", redo);
        assertEquals("redo text", "abc", redo.text);
        assertEquals("redo cursor", 3, redo.cursorPos);

        UndoManager.clear(widget);
    }

    private void testTransposeCharacters() {
        FakeTextField field = new FakeTextField("ab\uD83D\uDE00", 1);

        TextOperations.transposeCharacters(field);

        assertEquals("transpose swaps adjacent graphemes", "ba\uD83D\uDE00", field.getText());
        assertEquals("transpose moves cursor after swapped pair", 2, field.getCursor());
    }

    private void testCaseConversion() {
        FakeTextField field = new FakeTextField("say hello-world", 4);

        TextOperations.uppercaseWord(field);
        assertEquals("uppercase converts through word boundary", "say HELLO-WORLD", field.getText());
        assertEquals("uppercase moves cursor to word end", field.getText().length(), field.getCursor());

        field.setText("MIXED case");
        field.setCursor(0);
        TextOperations.lowercaseWord(field);
        assertEquals("lowercase converts first word", "mixed case", field.getText());

        field.setText("say hELLO");
        field.setCursor(4);
        TextOperations.capitalizeWord(field);
        assertEquals("capitalize uppercases first letter and lowers rest", "say Hello", field.getText());
    }

    private void testHistorySearchCaseInsensitiveMatching() {
        HistorySearch search = new HistorySearch(() -> false);
        search.enter("", List.of("/Give stone", "/time set day", "/give dirt"));

        search.appendToQuery('g');
        search.appendToQuery('i');

        SearchState state = search.getState();
        assertTrue("case-insensitive search finds matches", state.hasMatches());
        assertEquals("newest match selected first", "/give dirt", state.displayText());
        assertEquals("history index comes from original history", 2, state.historyIndex());
        assertEquals("cursor moves to match start", 1, state.cursorPosition());

        search.cyclePrevious();
        assertEquals("cycle previous moves to older match", "/Give stone", search.getState().displayText());
    }

    private void testHistorySearchCaseSensitiveMatching() {
        HistorySearch search = new HistorySearch(() -> true);
        search.enter("draft", List.of("/Give stone", "/give dirt"));

        search.appendToQuery('G');

        SearchState state = search.getState();
        assertTrue("case-sensitive search finds exact-case match", state.hasMatches());
        assertEquals("case-sensitive selected text", "/Give stone", state.displayText());

        search.appendToQuery('I');
        assertFalse("case-sensitive search rejects wrong case", search.getState().hasMatches());
        assertEquals("failed search leaves original input visible", "draft", search.getState().displayText());
    }

    private void testHistorySearchReusesLastQuery() {
        HistorySearch search = new HistorySearch(() -> false);
        search.enter("", List.of("first", "second"));
        search.appendToQuery('s');
        search.exit();

        search.enter("", List.of("first", "second"));
        assertTrue("last query can be reused", search.reuseLastQuery());
        assertEquals("reuse restores last matching text", "second", search.getState().displayText());
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
