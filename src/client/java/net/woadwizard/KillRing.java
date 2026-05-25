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
 * Yank-pop (M-y) tracking validates the widget and exact inserted text span,
 * so cycling only replaces the yank that was just inserted.
 */
public class KillRing {
    private static final Logger LOGGER = LoggerFactory.getLogger(KillRing.class);
    private static final int MAX_RING_SIZE = 60;
    private static final Deque<String> ring = new ArrayDeque<>();
    private static int yankIndex = 0;

    private static YankSession yankSession = null;

    private record YankSession(Object widget, int start, int end, String text) {}

    public record YankPopReplacement(int start, int end, String text) {}

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
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft != null) {
                TextFieldHelper.setClipboardContents(minecraft, text);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to copy to system clipboard", e);
        }
        // Reset yank tracking - new kills invalidate yank-pop
        clearYankTracking();
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
                Minecraft minecraft = Minecraft.getInstance();
                if (minecraft == null) {
                    return "";
                }
                String clipboard = TextFieldHelper.getClipboardContents(minecraft);
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
     * Record that a yank just completed in the given widget and text span.
     * This enables yank-pop (M-y) to work.
     */
    public static void recordYank(Object widget, int start, int end, String text) {
        if (widget == null || text == null || text.isEmpty() || start < 0 || end < start) {
            clearYankTracking();
            return;
        }
        yankSession = new YankSession(widget, start, end, text);
        LOGGER.debug("Recorded yank: start={}, end={}, length={}", start, end, text.length());
    }

    /**
     * Check if yank-pop can be performed against the current widget text.
     * Returns true only when the active widget still contains the previous
     * yank at the exact span where it was inserted.
     */
    public static boolean canYankPop(Object widget, String currentText, int currentCursor) {
        boolean can = isValidYankSession(widget, currentText, currentCursor);
        LOGGER.debug("canYankPop: cursor={}, result={}", currentCursor, can);
        return can;
    }

    /**
     * Cycle to the next entry in the kill ring (for M-y after C-y).
     * Returns null if yank-pop is not valid at the current cursor position.
     */
    public static YankPopReplacement yankPop(Object widget, String currentText, int currentCursor) {
        if (!canYankPop(widget, currentText, currentCursor)) {
            return null;
        }
        yankIndex = (yankIndex + 1) % ring.size();
        String text = getAtIndex(yankIndex);
        LOGGER.debug("Yank-pop to ring[{}]: {} chars", yankIndex, text.length());
        return new YankPopReplacement(yankSession.start(), yankSession.end(), text);
    }

    /**
     * Update the tracked replacement span after a yank-pop replacement.
     */
    public static void updateYankPosition(Object widget, int start, int end, String text) {
        recordYank(widget, start, end, text);
        LOGGER.debug("Updated yank position: start={}, end={}, length={}", start, end, text.length());
    }

    /**
     * Clear yank-pop tracking (called when non-yank operations happen).
     * This is optional - position-based tracking handles most cases automatically.
     */
    public static void clearYankTracking() {
        yankSession = null;
    }

    static void clearForTesting() {
        ring.clear();
        yankIndex = 0;
        clearYankTracking();
    }

    private static boolean isValidYankSession(Object widget, String currentText, int currentCursor) {
        if (yankSession == null || widget == null || currentText == null || ring.isEmpty()) {
            return false;
        }
        if (widget != yankSession.widget() || currentCursor != yankSession.end()) {
            return false;
        }
        int start = yankSession.start();
        int end = yankSession.end();
        if (start < 0 || end < start || end > currentText.length()) {
            return false;
        }
        return currentText.substring(start, end).equals(yankSession.text());
    }
}
