package net.woadwizard.emacs;

import net.woadwizard.KillRing;
import net.woadwizard.UndoManager;
import net.woadwizard.config.ConfigHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

import static net.woadwizard.emacs.WordBoundary.*;

/**
 * Shared text operations for Emacs-style editing.
 * Centralizes kill/yank/transpose/undo operations used by commands.
 */
public final class TextOperations {

    private static final Logger LOGGER = LoggerFactory.getLogger(TextOperations.class);

    private TextOperations() {}

    // ========== Kill Operations ==========

    /**
     * Common helper for kill operations: records undo, adds to kill ring, executes delete action.
     */
    private static void killText(TextFieldAdapter field, String killed, Runnable deleteAction) {
        if (killed != null && !killed.isEmpty()) {
            UndoManager.recordState(field.getWidget(), field.getState(), field.getText(), field.getCursor());
            KillRing.kill(killed);
            deleteAction.run();
        }
        field.getState().deactivateMark();
    }

    /**
     * Kill text from cursor to end of line (C-k).
     */
    public static void killToLineEnd(TextFieldAdapter field) {
        Objects.requireNonNull(field, "field must not be null");
        int cursor = field.getCursor();
        int lineEnd = field.getLineEnd();
        String killed = cursor < lineEnd ? field.getText().substring(cursor, lineEnd) : null;
        killText(field, killed, () -> field.deleteChars(lineEnd - cursor));
        if (ConfigHelper.isEnabled()) {
            LOGGER.trace("C-k: killed {} chars", killed != null ? killed.length() : 0);
        }
    }

    /**
     * Kill text from cursor to beginning of line (C-u).
     */
    public static void killToLineStart(TextFieldAdapter field) {
        Objects.requireNonNull(field, "field must not be null");
        int cursor = field.getCursor();
        int lineStart = field.getLineStart();
        String killed = cursor > lineStart ? field.getText().substring(lineStart, cursor) : null;
        killText(field, killed, () -> field.deleteChars(lineStart - cursor));
        if (ConfigHelper.isEnabled()) {
            LOGGER.trace("C-u: killed {} chars", killed != null ? killed.length() : 0);
        }
    }

    /**
     * Kill the selected region (C-w).
     * If no selection and killWordOnCw is enabled, kills backward to whitespace.
     */
    public static void killRegion(TextFieldAdapter field) {
        Objects.requireNonNull(field, "field must not be null");
        if (field.hasSelection()) {
            String killed = field.getSelectedText();
            killText(field, killed, () -> field.insertText(""));
        } else if (ConfigHelper.isKillWordOnCw()) {
            killWordBackwardToWhitespace(field);
        } else {
            field.getState().deactivateMark();
        }
    }

    /**
     * Kill backward to whitespace (alternative C-w behavior).
     */
    private static void killWordBackwardToWhitespace(TextFieldAdapter field) {
        String text = field.getText();
        int cursor = field.getCursor();
        int lineStart = field.getLineStart();

        if (cursor <= lineStart) {
            field.getState().deactivateMark();
            return;
        }

        // Skip whitespace, then skip non-whitespace
        int pos = cursor;
        while (pos > lineStart && Character.isWhitespace(text.charAt(pos - 1))) {
            pos--;
        }
        while (pos > lineStart && !Character.isWhitespace(text.charAt(pos - 1))) {
            pos--;
        }

        final int deleteOffset = pos - cursor;
        String killed = pos < cursor ? text.substring(pos, cursor) : null;
        killText(field, killed, () -> field.deleteChars(deleteOffset));
    }

    /**
     * Kill word in the specified direction (M-d forward, M-Backspace backward).
     */
    public static void killWord(TextFieldAdapter field, int direction) {
        Objects.requireNonNull(field, "field must not be null");
        String text = field.getText();
        int cursor = field.getCursor();
        int offset = field.getWordOffset(direction);
        if (offset == 0) {
            field.getState().deactivateMark();
            return;
        }
        int start = direction > 0 ? cursor : cursor + offset;
        int end = direction > 0 ? cursor + offset : cursor;
        killText(field, text.substring(start, end), () -> field.deleteChars(offset));
    }

    // ========== Copy/Yank Operations ==========

    /**
     * Copy the selected region to kill ring without deleting (M-w).
     */
    public static void copyRegion(TextFieldAdapter field) {
        Objects.requireNonNull(field, "field must not be null");
        if (field.hasSelection()) {
            String selected = field.getSelectedText();
            if (selected != null && !selected.isEmpty()) {
                KillRing.kill(selected);
            }
        }
        field.getState().deactivateMark();
        field.collapseSelection();
    }

    /**
     * Yank (paste) from kill ring (C-y).
     */
    public static void yank(TextFieldAdapter field) {
        Objects.requireNonNull(field, "field must not be null");
        String text = KillRing.yank();
        if (text != null && !text.isEmpty()) {
            int cursorBefore = field.getCursor();
            UndoManager.recordState(field.getWidget(), field.getState(), field.getText(), cursorBefore);
            field.insertText(text);
            int cursorAfter = field.getCursor();
            KillRing.recordYank(cursorAfter, text.length());
            LOGGER.trace("Yank: inserted {} chars, cursor {} -> {}", text.length(), cursorBefore, cursorAfter);
        }
        field.getState().deactivateMark();
    }

    /**
     * Cycle through kill ring entries (M-y after C-y).
     */
    public static void yankPop(TextFieldAdapter field) {
        Objects.requireNonNull(field, "field must not be null");
        int cursor = field.getCursor();
        int lastLen = KillRing.getLastYankLength();
        String nextText = KillRing.yankPop(cursor);
        if (nextText != null && !nextText.isEmpty()) {
            UndoManager.recordState(field.getWidget(), field.getState(), field.getText(), cursor);
            // Select the previously yanked text and replace it
            field.setSelectionStart(cursor - lastLen);
            field.insertText(nextText);
            int newCursor = field.getCursor();
            KillRing.updateYankPosition(newCursor, nextText.length());
            LOGGER.trace("Yank-pop: replaced {} chars with {} chars", lastLen, nextText.length());
        }
    }

    // ========== Transpose Operations ==========

    /**
     * Transpose the two grapheme clusters around the cursor (C-t).
     * Handles multi-codepoint emoji and combining characters correctly.
     */
    public static void transposeCharacters(TextFieldAdapter field) {
        Objects.requireNonNull(field, "field must not be null");
        String text = field.getText();
        int cursor = field.getCursor();

        if (text == null || text.length() < 2) return;

        UndoManager.recordState(field.getWidget(), field.getState(), text, cursor);

        int pos1Start, pos1End, pos2Start, pos2End;

        if (cursor == 0) {
            // At start: transpose first two graphemes
            pos1Start = 0;
            pos1End = GraphemeUtils.nextGraphemeBoundary(text, 0);
            if (pos1End >= text.length()) return;
            pos2Start = pos1End;
            pos2End = GraphemeUtils.nextGraphemeBoundary(text, pos2Start);
        } else if (cursor >= text.length()) {
            // At end: transpose last two graphemes
            pos2End = text.length();
            pos2Start = GraphemeUtils.previousGraphemeBoundary(text, pos2End);
            if (pos2Start == 0) return;
            pos1End = pos2Start;
            pos1Start = GraphemeUtils.previousGraphemeBoundary(text, pos1End);
        } else {
            // Middle: transpose grapheme before and at/after cursor
            pos1End = cursor;
            pos1Start = GraphemeUtils.previousGraphemeBoundary(text, cursor);
            pos2Start = cursor;
            pos2End = GraphemeUtils.nextGraphemeBoundary(text, cursor);
        }

        String grapheme1 = text.substring(pos1Start, pos1End);
        String grapheme2 = text.substring(pos2Start, pos2End);

        String newText = text.substring(0, pos1Start) + grapheme2 + grapheme1 + text.substring(pos2End);
        field.setText(newText);
        field.setCursor(pos2End);
        field.collapseSelection();
        field.getState().deactivateMark();
    }

    /**
     * Transpose the two words around the cursor (M-t).
     */
    public static void transposeWords(TextFieldAdapter field) {
        Objects.requireNonNull(field, "field must not be null");
        String text = field.getText();
        int cursor = field.getCursor();

        if (text == null || text.isEmpty()) return;

        UndoManager.recordState(field.getWidget(), field.getState(), text, cursor);

        // Find word1 (before or at cursor)
        int word1End = findWordEndBefore(text, cursor);
        if (word1End == 0) return;
        int word1Start = findWordStartBefore(text, word1End);

        // Find word2 (after cursor)
        int word2Start = findWordStartAfter(text, cursor);

        String newText;
        int newCursor;

        if (word2Start >= text.length()) {
            // No word after cursor: transpose the two words before cursor
            int word0End = findWordEndBefore(text, word1Start);
            if (word0End == 0) return;
            int word0Start = findWordStartBefore(text, word0End);

            String word0 = text.substring(word0Start, word0End);
            String between = text.substring(word0End, word1Start);
            String word1 = text.substring(word1Start, word1End);

            newText = text.substring(0, word0Start) + word1 + between + word0 + text.substring(word1End);
            newCursor = word0Start + word1.length() + between.length() + word0.length();
        } else {
            int word2End = findWordEndAfter(text, word2Start);

            String word1 = text.substring(word1Start, word1End);
            String between = text.substring(word1End, word2Start);
            String word2 = text.substring(word2Start, word2End);

            newText = text.substring(0, word1Start) + word2 + between + word1 + text.substring(word2End);
            newCursor = word1Start + word2.length() + between.length() + word1.length();
        }

        field.setText(newText);
        field.setCursor(newCursor);
        field.collapseSelection();
        field.getState().deactivateMark();
    }

    // ========== Undo ==========

    /**
     * Perform undo operation (C-/).
     */
    public static void performUndo(TextFieldAdapter field) {
        Objects.requireNonNull(field, "field must not be null");
        String currentText = field.getText();
        int currentCursor = field.getCursor();
        WidgetState widgetState = field.getState();

        UndoManager.UndoState state = UndoManager.undo(field.getWidget(), widgetState, currentText, currentCursor);
        if (state != null) {
            field.setText(state.text);
            field.setCursor(Math.min(state.cursorPos, state.text.length()));
            field.collapseSelection();
        }
        widgetState.deactivateMark();
    }

    /**
     * Perform redo operation (C-S-/).
     */
    public static void performRedo(TextFieldAdapter field) {
        Objects.requireNonNull(field, "field must not be null");
        String currentText = field.getText();
        int currentCursor = field.getCursor();
        WidgetState widgetState = field.getState();

        UndoManager.UndoState state = UndoManager.redo(field.getWidget(), widgetState, currentText, currentCursor);
        if (state != null) {
            field.setText(state.text);
            field.setCursor(Math.min(state.cursorPos, state.text.length()));
            field.collapseSelection();
        }
        widgetState.deactivateMark();
    }

    // ========== Case Conversion Operations ==========

    /**
     * Uppercase word from cursor to end of word (M-u).
     */
    public static void uppercaseWord(TextFieldAdapter field) {
        Objects.requireNonNull(field, "field must not be null");
        convertWordCase(field, String::toUpperCase);
    }

    /**
     * Lowercase word from cursor to end of word (M-l).
     */
    public static void lowercaseWord(TextFieldAdapter field) {
        Objects.requireNonNull(field, "field must not be null");
        convertWordCase(field, String::toLowerCase);
    }

    /**
     * Capitalize word: uppercase first letter, lowercase rest (M-c).
     */
    public static void capitalizeWord(TextFieldAdapter field) {
        Objects.requireNonNull(field, "field must not be null");
        convertWordCase(field, TextOperations::capitalize);
    }

    /**
     * Helper to capitalize a string (first letter uppercase, rest lowercase).
     */
    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        int firstLetter = -1;
        for (int i = 0; i < s.length(); i++) {
            if (Character.isLetter(s.charAt(i))) {
                firstLetter = i;
                break;
            }
        }
        if (firstLetter < 0) return s.toLowerCase();
        return s.substring(0, firstLetter)
             + Character.toUpperCase(s.charAt(firstLetter))
             + s.substring(firstLetter + 1).toLowerCase();
    }

    /**
     * Common helper for case conversion operations.
     * Operates on text from cursor to word end, then moves cursor past the word.
     */
    private static void convertWordCase(TextFieldAdapter field, java.util.function.UnaryOperator<String> converter) {
        String text = field.getText();
        int cursor = field.getCursor();

        if (text == null || text.isEmpty() || cursor >= text.length()) {
            field.getState().deactivateMark();
            return;
        }

        int wordEnd = WordBoundary.findWordEndAfter(text, cursor);
        if (wordEnd <= cursor) {
            field.getState().deactivateMark();
            return;
        }

        UndoManager.recordState(field.getWidget(), field.getState(), text, cursor);

        String wordPart = text.substring(cursor, wordEnd);
        String converted = converter.apply(wordPart);

        String newText = text.substring(0, cursor) + converted + text.substring(wordEnd);
        field.setText(newText);
        field.setCursor(wordEnd);
        field.collapseSelection();
        field.getState().deactivateMark();

        LOGGER.trace("Case conversion: '{}' -> '{}', cursor at {}", wordPart, converted, wordEnd);
    }
}
