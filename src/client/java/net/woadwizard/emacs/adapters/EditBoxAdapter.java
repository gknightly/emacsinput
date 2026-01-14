package net.woadwizard.emacs.adapters;

import net.woadwizard.emacs.TextFieldAdapter;
import net.woadwizard.emacs.WidgetState;
import net.woadwizard.mixin.client.EditBoxAccessor;
import net.minecraft.client.gui.components.EditBox;

/**
 * Adapter for EditBox (single-line text input used in chat, commands, etc.)
 */
public class EditBoxAdapter implements TextFieldAdapter {

    private final EditBox editBox;
    private final WidgetState state;

    public EditBoxAdapter(EditBox editBox) {
        this.editBox = editBox;
        this.state = new WidgetState();
    }

    @Override
    public WidgetState getState() {
        return state;
    }

    @Override
    public String getText() {
        return editBox.getValue();
    }

    @Override
    public void setText(String text) {
        editBox.setValue(text);
    }

    @Override
    public int getCursor() {
        return editBox.getCursorPosition();
    }

    @Override
    public void setCursor(int pos) {
        editBox.setCursorPosition(pos);
    }

    @Override
    public int getSelectionStart() {
        return ((EditBoxAccessor) editBox).getHighlightPos();
    }

    @Override
    public void setSelectionStart(int pos) {
        ((EditBoxAccessor) editBox).setHighlightPos(pos);
    }

    @Override
    public boolean hasSelection() {
        return getCursor() != getSelectionStart();
    }

    @Override
    public String getSelectedText() {
        return editBox.getHighlighted();
    }

    @Override
    public void insertText(String text) {
        editBox.insertText(text);
    }

    @Override
    public void deleteChars(int count) {
        editBox.deleteChars(count);
    }

    @Override
    public void moveChar(int direction, boolean selecting) {
        // In 1.20.1, moveCursor doesn't have a selecting parameter
        editBox.moveCursor(direction);
        if (!selecting) {
            ((EditBoxAccessor) editBox).setHighlightPos(editBox.getCursorPosition());
        }
    }

    @Override
    public void moveWord(int direction, boolean selecting) {
        int wordPos = editBox.getWordPosition(direction);
        // In 1.20.1, moveCursorTo doesn't have a selecting parameter
        editBox.moveCursorTo(wordPos);
        if (!selecting) {
            ((EditBoxAccessor) editBox).setHighlightPos(wordPos);
        }
    }

    @Override
    public void moveToStart(boolean selecting) {
        // In 1.20.1, moveCursorToStart doesn't have a selecting parameter
        editBox.moveCursorToStart();
        if (!selecting) {
            ((EditBoxAccessor) editBox).setHighlightPos(editBox.getCursorPosition());
        }
    }

    @Override
    public void moveToEnd(boolean selecting) {
        // In 1.20.1, moveCursorToEnd doesn't have a selecting parameter
        editBox.moveCursorToEnd();
        if (!selecting) {
            ((EditBoxAccessor) editBox).setHighlightPos(editBox.getCursorPosition());
        }
    }

    @Override
    public Object getWidget() {
        return editBox;
    }

    @Override
    public int getLineStart() {
        return 0; // Single line
    }

    @Override
    public int getLineEnd() {
        return getText().length();
    }

    @Override
    public int getWordOffset(int direction) {
        int cursor = getCursor();
        int wordPos = editBox.getWordPosition(direction);
        return wordPos - cursor;
    }
}
