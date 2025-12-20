package net.woadwizard.emacs;

/**
 * Abstraction over different Minecraft text field types.
 * Allows shared Emacs keybinding logic to work with EditBox, MultilineTextField, and TextFieldHelper.
 */
public interface TextFieldAdapter {

    // Per-widget Emacs state (mark, C-x prefix, etc.)
    WidgetState getState();

    // Text access
    String getText();
    void setText(String text);

    // Cursor position
    int getCursor();
    void setCursor(int pos);

    // Selection
    int getSelectionStart();
    void setSelectionStart(int pos);
    boolean hasSelection();
    String getSelectedText();

    // Text manipulation
    void insertText(String text);
    void deleteChars(int count);  // positive = forward, negative = backward

    // Movement
    void moveChar(int direction, boolean selecting);
    void moveWord(int direction, boolean selecting);
    void moveToStart(boolean selecting);
    void moveToEnd(boolean selecting);

    // Line operations (for multi-line fields)
    default boolean supportsMultiLine() { return false; }
    default void moveLine(int direction, boolean selecting) {}
    default int getLineStart() { return 0; }
    default int getLineEnd() { return getText().length(); }

    // Word offset for kill operations (should match moveWord behavior)
    int getWordOffset(int direction);

    // Widget identity for undo tracking (used as key in WeakHashMap)
    Object getWidget();

    // Collapse selection to cursor
    default void collapseSelection() {
        setSelectionStart(getCursor());
    }
}
