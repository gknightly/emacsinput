package net.woadwizard.search;

import net.woadwizard.mixin.client.EditBoxAccessor;
import net.minecraft.client.gui.components.EditBox;

import java.util.Objects;

/**
 * Single point of coordination between HistorySearch state and EditBox UI.
 * All EditBox updates during search mode should go through this class.
 */
public final class SearchController {

    private SearchController() {}

    /**
     * Sync the EditBox display to match the current search state.
     * Call this after any search state change.
     */
    public static void syncToEditBox(EditBox editBox, SearchState state) {
        Objects.requireNonNull(editBox, "editBox must not be null");
        Objects.requireNonNull(state, "state must not be null");

        if (!state.active()) {
            return;
        }

        String text = state.displayText();
        int cursorPos = state.cursorPosition();

        editBox.setValue(text);
        editBox.setCursorPosition(cursorPos);
        ((EditBoxAccessor) editBox).setHighlightPos(cursorPos);
    }

    /**
     * Accept the current match and exit search mode.
     * Returns the history index for syncing with ChatScreen's historyPos.
     */
    public static int acceptAndExit(EditBox editBox, HistorySearch historySearch) {
        Objects.requireNonNull(editBox, "editBox must not be null");
        Objects.requireNonNull(historySearch, "historySearch must not be null");

        SearchState state = historySearch.getState();
        int historyIndex = state.historyIndex();

        String text = state.displayText();
        int cursorPos = state.cursorPosition();

        historySearch.exit();

        editBox.setValue(text);
        editBox.setCursorPosition(cursorPos);
        ((EditBoxAccessor) editBox).setHighlightPos(cursorPos);

        return historyIndex;
    }

    /**
     * Cancel search and restore original input.
     */
    public static void cancelAndExit(EditBox editBox, HistorySearch historySearch) {
        Objects.requireNonNull(editBox, "editBox must not be null");
        Objects.requireNonNull(historySearch, "historySearch must not be null");

        SearchState state = historySearch.getState();
        String original = state.originalInput();

        historySearch.exit();

        editBox.setValue(original);
        editBox.moveCursorToEnd(false);
    }
}
