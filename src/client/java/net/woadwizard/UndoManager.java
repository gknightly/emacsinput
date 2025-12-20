package net.woadwizard;

import net.woadwizard.emacs.WidgetState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Simple undo manager for text fields.
 * Tracks text state changes and allows undoing to previous states.
 * Uses WeakHashMap so undo history is automatically cleaned up when widgets are garbage collected.
 */
public class UndoManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(UndoManager.class);
    private static final int MAX_UNDO_HISTORY = 100;

    // Separate undo stacks per widget instance. WeakHashMap ensures automatic cleanup
    // when the widget is no longer referenced (e.g., screen closed).
    private static final Map<Object, Deque<UndoState>> undoStacks = new WeakHashMap<>();

    /**
     * Record a state for potential undo.
     * Call this BEFORE making changes to capture the previous state.
     *
     * @param widget The widget object (used as key for undo stack lookup)
     * @param state The widget's state (used to check if undoing)
     * @param text Current text before the change
     * @param cursorPos Current cursor position before the change
     */
    public static void recordState(Object widget, WidgetState state, String text, int cursorPos) {
        if (widget == null || (state != null && state.isUndoing())) {
            return; // Don't record state during undo operations
        }

        Deque<UndoState> stack = undoStacks.computeIfAbsent(widget, k -> new ArrayDeque<>());

        // Don't record duplicate states
        if (!stack.isEmpty()) {
            UndoState top = stack.peek();
            if (top.text.equals(text) && top.cursorPos == cursorPos) {
                return;
            }
        }

        stack.push(new UndoState(text, cursorPos));
        LOGGER.debug("Recorded undo state: cursor={}, stackSize={}", cursorPos, stack.size());

        // Limit history size
        while (stack.size() > MAX_UNDO_HISTORY) {
            stack.removeLast();
        }
    }

    /**
     * Undo to the previous state.
     *
     * @param widget The widget object
     * @param state The widget's state (used to set undoing flag)
     * @param currentText Current text (to record for potential redo)
     * @param currentCursorPos Current cursor position
     * @return The previous state to restore, or null if no undo available
     */
    public static UndoState undo(Object widget, WidgetState state, String currentText, int currentCursorPos) {
        if (widget == null) {
            return null;
        }
        Deque<UndoState> stack = undoStacks.get(widget);
        if (stack == null || stack.isEmpty()) {
            LOGGER.debug("Undo: no history available");
            return null;
        }

        // Set undoing flag to prevent recording intermediate states
        if (state != null) {
            state.setUndoing(true);
        }

        try {
            // Pop the previous state
            UndoState previousState = stack.pop();

            // If the previous state matches current state, try the one before
            if (previousState.text.equals(currentText)) {
                if (stack.isEmpty()) {
                    // Put it back and return null
                    stack.push(previousState);
                    LOGGER.debug("Undo: current state matches only available state");
                    return null;
                }
                previousState = stack.pop();
            }

            LOGGER.debug("Undo: restored to cursor={}, remainingStates={}", previousState.cursorPos, stack.size());
            return previousState;
        } finally {
            if (state != null) {
                state.setUndoing(false);
            }
        }
    }

    /**
     * Clear undo history for a widget.
     */
    public static void clear(Object widget) {
        if (widget != null) {
            undoStacks.remove(widget);
        }
    }

    /**
     * Represents a saved text state.
     */
    public static class UndoState {
        public final String text;
        public final int cursorPos;

        public UndoState(String text, int cursorPos) {
            this.text = text;
            this.cursorPos = cursorPos;
        }
    }
}
