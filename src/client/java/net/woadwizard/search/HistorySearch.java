package net.woadwizard.search;

import net.woadwizard.config.ConfigHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Emacs-style incremental history search (C-r / C-s).
 *
 * This class manages search state only - no display logic.
 * Use SearchController to sync state to UI components.
 *
 * Each ChatScreen should have its own instance.
 */
public class HistorySearch {
    private static final Logger LOGGER = LoggerFactory.getLogger(HistorySearch.class);

    // Search state
    private boolean active = false;
    private final StringBuilder query = new StringBuilder();
    private String lastQuery = "";  // Remembered for C-r reuse
    private String originalInput = "";
    private List<String> history = List.of();

    // Match tracking
    private List<Match> matches = new ArrayList<>();
    private int selectedIndex = -1;

    /**
     * Represents a match: the history entry text, its index in history,
     * and the position of the query within the text.
     */
    public record Match(String text, int historyIndex, int queryPosition) {}

    /**
     * Enter search mode.
     * Takes a List directly to avoid unnecessary copying.
     */
    public void enter(String currentInput, List<String> historyList) {
        active = true;
        query.setLength(0);
        originalInput = currentInput;
        history = historyList != null ? historyList : List.of();
        matches.clear();
        selectedIndex = -1;
        LOGGER.debug("Entered search mode with {} history entries", history.size());
    }

    /**
     * Exit search mode, remembering query for potential reuse.
     */
    public void exit() {
        if (!active) {
            return;
        }
        if (query.length() > 0) {
            lastQuery = query.toString();
            LOGGER.debug("Exited search mode, saved query: '{}'", lastQuery);
        } else {
            LOGGER.debug("Exited search mode");
        }
        active = false;
        query.setLength(0);
        originalInput = "";
        history = List.of();
        matches.clear();
        selectedIndex = -1;
    }

    /**
     * Check if search mode is active.
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Append a character to the search query.
     */
    public void appendToQuery(char c) {
        query.append(c);
        rebuildMatches();
        selectFirstMatch();
    }

    /**
     * Delete the last character from the search query.
     */
    public void deleteFromQuery() {
        if (query.length() > 0) {
            query.setLength(query.length() - 1);
            rebuildMatches();
            selectFirstMatch();
        }
    }

    /**
     * Cycle to the previous match (C-r direction).
     */
    public void cyclePrevious() {
        if (matches.isEmpty()) return;

        if (selectedIndex < 0) {
            selectedIndex = 0;
        } else {
            selectedIndex++;
            if (selectedIndex >= matches.size()) {
                selectedIndex = 0;
            }
        }
    }

    /**
     * Cycle to the next match (C-s direction).
     */
    public void cycleNext() {
        if (matches.isEmpty()) return;

        if (selectedIndex < 0) {
            selectedIndex = matches.size() - 1;
        } else {
            selectedIndex--;
            if (selectedIndex < 0) {
                selectedIndex = matches.size() - 1;
            }
        }
    }

    /**
     * Reuse the last search query (bash behavior).
     * Returns true if there was a last query to reuse.
     */
    public boolean reuseLastQuery() {
        if (lastQuery.isEmpty()) {
            return false;
        }
        query.setLength(0);
        query.append(lastQuery);
        rebuildMatches();
        selectFirstMatch();
        return true;
    }

    /**
     * Check if there's a last query that can be reused.
     */
    public boolean hasLastQuery() {
        return !lastQuery.isEmpty();
    }

    /**
     * Get an immutable snapshot of the current search state.
     */
    public SearchState getState() {
        Match selected = (selectedIndex >= 0 && selectedIndex < matches.size())
            ? matches.get(selectedIndex)
            : null;

        int historyIndex = (selected != null) ? selected.historyIndex() : -1;

        return new SearchState(
            active,
            query.toString(),
            originalInput,
            selected,
            !matches.isEmpty(),
            historyIndex
        );
    }

    /**
     * Rebuild the matches list based on current query.
     */
    private void rebuildMatches() {
        matches.clear();

        if (query.length() == 0) {
            return;
        }

        String queryStr = query.toString();
        boolean caseSensitive = ConfigHelper.isHistorySearchCaseSensitive();
        String searchQuery = caseSensitive ? queryStr : queryStr.toLowerCase();

        // Iterate history from newest to oldest
        for (int i = history.size() - 1; i >= 0; i--) {
            String entry = history.get(i);
            String searchEntry = caseSensitive ? entry : entry.toLowerCase();

            // Find all occurrences, right to left
            List<Integer> positions = new ArrayList<>();
            int pos = searchEntry.lastIndexOf(searchQuery);
            while (pos >= 0) {
                positions.add(pos);
                pos = (pos > 0) ? searchEntry.lastIndexOf(searchQuery, pos - 1) : -1;
            }

            for (int queryPos : positions) {
                matches.add(new Match(entry, i, queryPos));
            }
        }
        LOGGER.debug("Search query '{}' found {} matches", query, matches.size());
    }

    private void selectFirstMatch() {
        selectedIndex = matches.isEmpty() ? -1 : 0;
    }

    // ========== Current Instance Tracking ==========
    // These static methods provide convenient access to the currently active
    // HistorySearch instance. ChatScreenMixin sets/clears this when screens open/close.

    private static HistorySearch current = null;

    /**
     * Get the current HistorySearch instance (if any screen has one active).
     */
    public static HistorySearch getCurrent() {
        return current;
    }

    /**
     * Set the current HistorySearch instance (called by ChatScreenMixin).
     */
    public static void setCurrent(HistorySearch instance) {
        current = instance;
    }

    /**
     * Check if any search is currently active.
     */
    public static boolean isSearchActive() {
        return current != null && current.isActive();
    }

    /**
     * Get the current search state, or an inactive state if no search is active.
     */
    public static SearchState getCurrentState() {
        return current != null ? current.getState() : SearchState.INACTIVE;
    }
}
