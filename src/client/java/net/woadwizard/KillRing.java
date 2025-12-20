package net.woadwizard;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.font.TextFieldHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/**
 * Emacs-style kill ring for storing killed (cut) text.
 * Supports cycling through previous kills with M-y after C-y.
 *
 * Yank-pop (M-y) tracking uses cursor position instead of a boolean flag.
 * This is more robust because it doesn't require tracking state across
 * multiple event handlers that might accidentally reset it.
 */
public class KillRing {
    private static final Logger LOGGER = LoggerFactory.getLogger(KillRing.class);
    private static final int MAX_RING_SIZE = 60;
    private static final Deque<String> ring = new ArrayDeque<>();
    private static int yankIndex = 0;

    // Track yank position for M-y support
    // expectedCursorAfterYank is where the cursor should be if the last action was yank/yank-pop
    private static int expectedCursorAfterYank = -1;
    private static int lastYankLength = 0;

    /**
     * Add text to the kill ring.
     */
    public static void kill(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        // Add to front of ring (O(1) with ArrayDeque)
        ring.addFirst(text);
        // Trim ring if too large (O(1) removal from end)
        while (ring.size() > MAX_RING_SIZE) {
            ring.removeLast();
        }
        // Also copy to system clipboard
        try {
            TextFieldHelper.setClipboardContents(Minecraft.getInstance(), text);
        } catch (Exception e) {
            LOGGER.warn("Failed to copy to system clipboard", e);
        }
        // Reset yank tracking - new kills invalidate yank-pop
        expectedCursorAfterYank = -1;
        yankIndex = 0;
        LOGGER.debug("Killed text, ring size: {}", ring.size());
    }

    /**
     * Get the current kill ring entry for yanking.
     * Call recordYank() after inserting the text to enable yank-pop.
     */
    public static String yank() {
        if (ring.isEmpty()) {
            // Fall back to system clipboard
            try {
                String clipboard = TextFieldHelper.getClipboardContents(Minecraft.getInstance());
                LOGGER.debug("Yank from clipboard: {} chars", clipboard != null ? clipboard.length() : 0);
                return clipboard;
            } catch (Exception e) {
                LOGGER.warn("Failed to read from system clipboard", e);
                return "";
            }
        }
        // Ensure yankIndex is valid (ring may have changed)
        if (yankIndex >= ring.size()) {
            yankIndex = 0;
        }
        String text = getAtIndex(yankIndex);
        lastYankLength = text.length();
        LOGGER.debug("Yank from ring[{}]: {} chars", yankIndex, text.length());
        return text;
    }

    /**
     * Get element at index by iteration (ArrayDeque doesn't support random access).
     * This is O(n) but acceptable for a small ring size and infrequent access.
     */
    private static String getAtIndex(int index) {
        Iterator<String> it = ring.iterator();
        for (int i = 0; i < index && it.hasNext(); i++) {
            it.next();
        }
        return it.hasNext() ? it.next() : ring.getFirst();
    }

    /**
     * Record that a yank just completed at the given cursor position.
     * This enables yank-pop (M-y) to work.
     */
    public static void recordYank(int cursorAfterYank, int length) {
        expectedCursorAfterYank = cursorAfterYank;
        lastYankLength = length;
        LOGGER.debug("Recorded yank: cursor={}, length={}", cursorAfterYank, length);
    }

    /**
     * Check if yank-pop can be performed at the given cursor position.
     * Returns true if the cursor is exactly where the last yank left it.
     */
    public static boolean canYankPop(int currentCursor) {
        boolean can = expectedCursorAfterYank >= 0
                   && currentCursor == expectedCursorAfterYank
                   && !ring.isEmpty();
        LOGGER.debug("canYankPop: current={}, expected={}, result={}",
                     currentCursor, expectedCursorAfterYank, can);
        return can;
    }

    /**
     * Cycle to the next entry in the kill ring (for M-y after C-y).
     * Returns null if yank-pop is not valid at the current cursor position.
     */
    public static String yankPop(int currentCursor) {
        if (!canYankPop(currentCursor)) {
            return null;
        }
        yankIndex = (yankIndex + 1) % ring.size();
        String text = getAtIndex(yankIndex);
        lastYankLength = text.length();
        LOGGER.debug("Yank-pop to ring[{}]: {} chars", yankIndex, text.length());
        return text;
    }

    /**
     * Get the length of the last yanked text (for replacement in yank-pop).
     */
    public static int getLastYankLength() {
        return lastYankLength;
    }

    /**
     * Update the expected cursor position after a yank-pop replacement.
     */
    public static void updateYankPosition(int newCursor, int newLength) {
        expectedCursorAfterYank = newCursor;
        lastYankLength = newLength;
        LOGGER.debug("Updated yank position: cursor={}, length={}", newCursor, newLength);
    }

    /**
     * Clear yank-pop tracking (called when non-yank operations happen).
     * This is optional - position-based tracking handles most cases automatically.
     */
    public static void clearYankTracking() {
        expectedCursorAfterYank = -1;
    }
}
