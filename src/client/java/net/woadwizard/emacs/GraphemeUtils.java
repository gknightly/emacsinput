package net.woadwizard.emacs;

import java.text.BreakIterator;
import java.util.Locale;

/**
 * Utility class for grapheme cluster-aware text operations.
 * Handles multi-codepoint emoji, combining characters, and surrogate pairs correctly.
 */
public final class GraphemeUtils {

    private GraphemeUtils() {}

    /**
     * Find the char index of the next grapheme cluster boundary after pos.
     * @param text the text to analyze
     * @param pos the current char position
     * @return the position after the next grapheme cluster, or text.length() if at end
     */
    public static int nextGraphemeBoundary(String text, int pos) {
        if (text == null || pos >= text.length()) {
            return text == null ? 0 : text.length();
        }
        BreakIterator iterator = BreakIterator.getCharacterInstance(Locale.getDefault());
        iterator.setText(text);
        int boundary = iterator.following(pos);
        return boundary == BreakIterator.DONE ? text.length() : boundary;
    }

    /**
     * Find the char index of the previous grapheme cluster boundary before pos.
     * @param text the text to analyze
     * @param pos the current char position
     * @return the position before the previous grapheme cluster, or 0 if at start
     */
    public static int previousGraphemeBoundary(String text, int pos) {
        if (text == null || pos <= 0) {
            return 0;
        }
        BreakIterator iterator = BreakIterator.getCharacterInstance(Locale.getDefault());
        iterator.setText(text);
        int boundary = iterator.preceding(pos);
        return boundary == BreakIterator.DONE ? 0 : boundary;
    }
}
