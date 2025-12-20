package net.woadwizard.emacs;

/**
 * Utility class for finding word boundaries in text.
 * Uses whitespace-based logic matching Minecraft's internal implementation.
 */
public final class WordBoundary {

    private WordBoundary() {}

    /**
     * Find the offset to the next word boundary from the given cursor position.
     *
     * @param text the text to search
     * @param cursor current cursor position
     * @param direction positive for forward, negative for backward
     * @return offset from cursor to word boundary (can be 0 if at boundary)
     */
    public static int findOffset(String text, int cursor, int direction) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int target = (direction > 0)
            ? findWordEndAfter(text, cursor)
            : findWordStartBefore(text, cursor);
        return target - cursor;
    }

    /**
     * Find the position of the word start before (or at) the given position.
     * Skips trailing whitespace first, then skips word characters.
     */
    public static int findWordStartBefore(String text, int pos) {
        pos = skipBackward(text, pos, true);   // Skip whitespace
        pos = skipBackward(text, pos, false);  // Skip word chars
        return pos;
    }

    /**
     * Find the position of the word end before (or at) the given position.
     * Only skips trailing whitespace.
     */
    public static int findWordEndBefore(String text, int pos) {
        return skipBackward(text, pos, true);
    }

    /**
     * Find the position of the word start after (or at) the given position.
     * Only skips leading whitespace.
     */
    public static int findWordStartAfter(String text, int pos) {
        return skipForward(text, pos, true);
    }

    /**
     * Find the position of the word end after (or at) the given position.
     * Skips leading whitespace first, then skips word characters.
     */
    public static int findWordEndAfter(String text, int pos) {
        pos = skipForward(text, pos, true);   // Skip whitespace
        pos = skipForward(text, pos, false);  // Skip word chars
        return pos;
    }

    /**
     * Skip characters forward while they match the whitespace condition.
     * @param skipWhitespace if true, skip whitespace; if false, skip non-whitespace
     */
    private static int skipForward(String text, int pos, boolean skipWhitespace) {
        while (pos < text.length() && Character.isWhitespace(text.charAt(pos)) == skipWhitespace) {
            pos++;
        }
        return pos;
    }

    /**
     * Skip characters backward while they match the whitespace condition.
     * @param skipWhitespace if true, skip whitespace; if false, skip non-whitespace
     */
    private static int skipBackward(String text, int pos, boolean skipWhitespace) {
        while (pos > 0 && Character.isWhitespace(text.charAt(pos - 1)) == skipWhitespace) {
            pos--;
        }
        return pos;
    }
}
