package net.woadwizard.emacs;

/**
 * Per-widget Emacs state.
 * Each text field widget has its own instance of this class,
 * allowing independent mark/selection state across widgets.
 */
public class WidgetState {
    private static final long CX_PREFIX_TIMEOUT_MS = 2000;

    private boolean markActive = false;
    private boolean cxPrefixActive = false;
    private long cxPrefixTimestamp = 0;
    private boolean undoing = false;
    private boolean redoing = false;

    /**
     * Activate the mark at the current cursor position.
     * Called when C-Space is pressed.
     */
    public void setMark() {
        markActive = true;
    }

    /**
     * Check if the mark is currently active.
     * When active, navigation commands should extend selection.
     */
    public boolean isMarkActive() {
        return markActive;
    }

    /**
     * Deactivate the mark (called by C-g or after certain operations).
     */
    public void deactivateMark() {
        markActive = false;
    }

    /**
     * Deactivate the mark if active, returning whether it was active.
     */
    public boolean deactivateIfActive() {
        if (markActive) {
            markActive = false;
            return true;
        }
        return false;
    }

    /**
     * Set the C-x prefix state (for C-x C-x sequence).
     */
    public void setCxPrefix(boolean active) {
        cxPrefixActive = active;
        if (active) {
            cxPrefixTimestamp = System.currentTimeMillis();
        }
    }

    /**
     * Check if C-x prefix is active.
     * Returns false if the prefix has timed out.
     */
    public boolean isCxPrefixActive() {
        if (cxPrefixActive) {
            if (System.currentTimeMillis() - cxPrefixTimestamp > CX_PREFIX_TIMEOUT_MS) {
                cxPrefixActive = false;
                return false;
            }
            return true;
        }
        return false;
    }

    /**
     * Reset C-x prefix (called after processing or on timeout).
     */
    public void resetCxPrefix() {
        cxPrefixActive = false;
    }

    /**
     * Reset all state (called when screen closes).
     */
    public void reset() {
        markActive = false;
        cxPrefixActive = false;
        undoing = false;
        redoing = false;
    }

    /**
     * Check if an undo operation is currently in progress.
     */
    public boolean isUndoing() {
        return undoing;
    }

    /**
     * Set whether an undo operation is in progress.
     * While true, state changes won't be recorded in the undo history.
     */
    public void setUndoing(boolean undoing) {
        this.undoing = undoing;
    }

    /**
     * Check if a redo operation is currently in progress.
     */
    public boolean isRedoing() {
        return redoing;
    }

    /**
     * Set whether a redo operation is in progress.
     * While true, state changes won't be recorded in the undo history.
     */
    public void setRedoing(boolean redoing) {
        this.redoing = redoing;
    }
}
