package net.woadwizard;

import net.woadwizard.emacs.WidgetState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Undo manager for text fields with Emacs-style change amalgamation.
 * Consecutive character insertions and deletions are grouped together,
 * so one undo reverts multiple characters instead of one at a time.
 */
public class UndoManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(UndoManager.class);
    private static final int MAX_UNDO_HISTORY = 100;
    private static final int AMALGAMATE_LIMIT = 20;

    private static final Map<Object, Deque<UndoState>> undoStacks = new WeakHashMap<>();
    private static final Map<Object, Deque<UndoState>> redoStacks = new WeakHashMap<>();
    private static final Map<Object, AmalgamationState> amalgamationStates = new WeakHashMap<>();

    /**
     * Types of operations for amalgamation tracking.
     */
    public enum OperationType {
        INSERT,   // Character typed
        DELETE,   // Backspace/delete
        COMMAND   // Emacs command (always creates boundary)
    }

    /**
     * Tracks amalgamation state for a widget.
     */
    private static class AmalgamationState {
        OperationType lastOperation;
        int operationCount;
        String boundaryText;
        int boundaryCursor;

        void reset() {
            lastOperation = null;
            operationCount = 0;
            boundaryText = null;
            boundaryCursor = -1;
        }
    }

    /**
     * Record state for a character insertion (typing).
     * Consecutive inserts are amalgamated up to AMALGAMATE_LIMIT.
     */
    public static void recordStateForInsert(Object widget, WidgetState state, String text, int cursorPos) {
        recordStateWithAmalgamation(widget, state, text, cursorPos, OperationType.INSERT);
    }

    /**
     * Record state for a deletion (backspace/delete).
     * Consecutive deletes are amalgamated up to AMALGAMATE_LIMIT.
     */
    public static void recordStateForDelete(Object widget, WidgetState state, String text, int cursorPos) {
        recordStateWithAmalgamation(widget, state, text, cursorPos, OperationType.DELETE);
    }

    /**
     * Record state for an Emacs command.
     * Commands always create a boundary (no amalgamation).
     */
    public static void recordState(Object widget, WidgetState state, String text, int cursorPos) {
        recordStateWithAmalgamation(widget, state, text, cursorPos, OperationType.COMMAND);
    }

    /**
     * Core recording logic with amalgamation support.
     */
    private static void recordStateWithAmalgamation(Object widget, WidgetState state,
            String text, int cursorPos, OperationType opType) {
        if (widget == null || (state != null && (state.isUndoing() || state.isRedoing()))) {
            return;
        }

        // Clear redo stack on new changes
        redoStacks.remove(widget);

        AmalgamationState amalg = amalgamationStates.computeIfAbsent(widget, k -> new AmalgamationState());
        Deque<UndoState> stack = undoStacks.computeIfAbsent(widget, k -> new ArrayDeque<>());

        boolean needsBoundary = false;

        // Commands always create a boundary
        if (opType == OperationType.COMMAND) {
            needsBoundary = true;
        }
        // Operation type changed - save the amalgamated state and start fresh
        else if (amalg.lastOperation != null && amalg.lastOperation != opType) {
            needsBoundary = true;
        }
        // Hit the amalgamation limit
        else if (amalg.operationCount >= AMALGAMATE_LIMIT) {
            needsBoundary = true;
        }

        if (needsBoundary) {
            // If we have an amalgamated boundary, push it
            if (amalg.boundaryText != null) {
                pushState(stack, amalg.boundaryText, amalg.boundaryCursor);
            }
            // Reset and start new amalgamation group
            amalg.reset();
        }

        // For amalgamating operations, only save the boundary state (first state in group)
        if (opType != OperationType.COMMAND) {
            if (amalg.boundaryText == null) {
                // First operation in this group - save as boundary
                amalg.boundaryText = text;
                amalg.boundaryCursor = cursorPos;
            }
            amalg.lastOperation = opType;
            amalg.operationCount++;
            LOGGER.debug("Amalgamating {}: count={}", opType, amalg.operationCount);
        } else {
            // Commands push state immediately
            pushState(stack, text, cursorPos);
            amalg.reset();
            LOGGER.debug("Command boundary: cursor={}, stackSize={}", cursorPos, stack.size());
        }
    }

    /**
     * Force a boundary - pushes any pending amalgamated state.
     * Call this before operations that should start a fresh undo group.
     */
    public static void forceBoundary(Object widget) {
        if (widget == null) return;

        AmalgamationState amalg = amalgamationStates.get(widget);
        if (amalg != null && amalg.boundaryText != null) {
            Deque<UndoState> stack = undoStacks.computeIfAbsent(widget, k -> new ArrayDeque<>());
            pushState(stack, amalg.boundaryText, amalg.boundaryCursor);
            amalg.reset();
        }
    }

    private static void pushState(Deque<UndoState> stack, String text, int cursorPos) {
        // Don't push duplicates
        if (!stack.isEmpty()) {
            UndoState top = stack.peek();
            if (top.text.equals(text) && top.cursorPos == cursorPos) {
                return;
            }
        }
        stack.push(new UndoState(text, cursorPos));
        while (stack.size() > MAX_UNDO_HISTORY) {
            stack.removeLast();
        }
    }

    /**
     * Undo to the previous state.
     */
    public static UndoState undo(Object widget, WidgetState state, String currentText, int currentCursorPos) {
        if (widget == null) {
            return null;
        }

        // Force boundary to ensure pending changes are saved
        forceBoundary(widget);

        Deque<UndoState> stack = undoStacks.get(widget);
        if (stack == null || stack.isEmpty()) {
            LOGGER.debug("Undo: no history available");
            return null;
        }

        if (state != null) {
            state.setUndoing(true);
        }

        try {
            UndoState previousState = stack.pop();

            // If the previous state matches current state, try the one before
            if (previousState.text.equals(currentText)) {
                if (stack.isEmpty()) {
                    stack.push(previousState);
                    LOGGER.debug("Undo: current state matches only available state");
                    return null;
                }
                pushToRedo(widget, currentText, currentCursorPos);
                previousState = stack.pop();
            }

            pushToRedo(widget, currentText, currentCursorPos);

            LOGGER.debug("Undo: restored to cursor={}, remainingStates={}", previousState.cursorPos, stack.size());
            return previousState;
        } finally {
            if (state != null) {
                state.setUndoing(false);
            }
        }
    }

    /**
     * Redo to the next state.
     */
    public static UndoState redo(Object widget, WidgetState state, String currentText, int currentCursorPos) {
        if (widget == null) {
            return null;
        }
        Deque<UndoState> redoStack = redoStacks.get(widget);
        if (redoStack == null || redoStack.isEmpty()) {
            LOGGER.debug("Redo: no redo history available");
            return null;
        }

        if (state != null) {
            state.setRedoing(true);
        }

        try {
            UndoState redoState = redoStack.pop();

            Deque<UndoState> undoStack = undoStacks.computeIfAbsent(widget, k -> new ArrayDeque<>());
            undoStack.push(new UndoState(currentText, currentCursorPos));

            LOGGER.debug("Redo: restored to cursor={}, remainingRedoStates={}", redoState.cursorPos, redoStack.size());
            return redoState;
        } finally {
            if (state != null) {
                state.setRedoing(false);
            }
        }
    }

    private static void pushToRedo(Object widget, String text, int cursorPos) {
        Deque<UndoState> redoStack = redoStacks.computeIfAbsent(widget, k -> new ArrayDeque<>());
        redoStack.push(new UndoState(text, cursorPos));
        while (redoStack.size() > MAX_UNDO_HISTORY) {
            redoStack.removeLast();
        }
    }

    /**
     * Clear undo and redo history for a widget.
     */
    public static void clear(Object widget) {
        if (widget != null) {
            undoStacks.remove(widget);
            redoStacks.remove(widget);
            amalgamationStates.remove(widget);
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
