package net.woadwizard.search;

/**
 * Immutable snapshot of the current search state.
 * Used to pass search information to UI components without coupling them to HistorySearch.
 */
public record SearchState(
    boolean active,
    String query,
    String originalInput,
    HistorySearch.Match selectedMatch,  // null if no matches
    boolean hasMatches,
    int historyIndex                    // for syncing with ChatScreen's historyPos
) {
    /** An inactive search state, used as a default when no search is active. */
    public static final SearchState INACTIVE = new SearchState(false, "", "", null, false, -1);
    /**
     * Get the text to display in the EditBox.
     * Returns match text if available, otherwise original input.
     */
    public String displayText() {
        return (selectedMatch != null) ? selectedMatch.text() : originalInput;
    }

    /**
     * Get the cursor position (start of matched query in display text).
     * Returns end of text if no match.
     */
    public int cursorPosition() {
        if (selectedMatch != null) {
            return selectedMatch.queryPosition();
        }
        return originalInput.length();
    }
}
