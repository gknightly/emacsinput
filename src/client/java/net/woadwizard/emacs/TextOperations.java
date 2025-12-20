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
     * Transpose the two characters around the cursor (C-t).
     */
    public static void transposeCharacters(TextFieldAdapter field) {
        Objects.requireNonNull(field, "field must not be null");
        String text = field.getText();
        int cursor = field.getCursor();

        if (text == null || text.length() < 2) return;

        UndoManager.recordState(field.getWidget(), field.getState(), text, cursor);

        int pos1, pos2;
        if (cursor == 0) {
            pos1 = 0;
            pos2 = 1;
        } else if (cursor >= text.length()) {
            pos1 = text.length() - 2;
            pos2 = text.length() - 1;
        } else {
            pos1 = cursor - 1;
            pos2 = cursor;
        }

        char c1 = text.charAt(pos1);
        char c2 = text.charAt(pos2);

        String newText = text.substring(0, pos1) + c2 + c1 + text.substring(pos2 + 1);
        field.setText(newText);
        field.setCursor(pos2 + 1);
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
}
