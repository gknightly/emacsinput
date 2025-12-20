package net.woadwizard.emacs.adapters;

import net.woadwizard.emacs.TextFieldAdapter;
import net.woadwizard.emacs.WidgetState;
import net.woadwizard.mixin.client.MultilineTextFieldAccessor;
import net.minecraft.client.gui.components.MultilineTextField;
import net.minecraft.client.gui.components.Whence;

/**
 * Adapter for MultilineTextField (used in book editing).
 */
public class MultilineTextFieldAdapter implements TextFieldAdapter {

    private final MultilineTextField textField;
    private final WidgetState state;

    public MultilineTextFieldAdapter(MultilineTextField textField) {
        this.textField = textField;
        this.state = new WidgetState();
    }

    @Override
    public WidgetState getState() {
        return state;
    }

    @Override
    public String getText() {
        return textField.value();
    }

    @Override
    public void setText(String text) {
        // MultilineTextField doesn't have a direct setValue, so we select all and insert
        textField.seekCursor(Whence.ABSOLUTE, 0);
        textField.setSelecting(true);
        textField.seekCursor(Whence.END, 0);
        textField.insertText(text);
    }

    @Override
    public int getCursor() {
        return textField.cursor();
    }

    @Override
    public void setCursor(int pos) {
        textField.seekCursor(Whence.ABSOLUTE, pos);
    }

    @Override
    public int getSelectionStart() {
        return ((MultilineTextFieldAccessor) textField).getSelectCursor();
    }

    @Override
    public void setSelectionStart(int pos) {
        ((MultilineTextFieldAccessor) textField).setSelectCursor(pos);
    }

    @Override
    public boolean hasSelection() {
        return textField.hasSelection();
    }

    @Override
    public String getSelectedText() {
        return textField.getSelectedText();
    }

    @Override
    public void insertText(String text) {
        textField.insertText(text);
    }

    @Override
    public void deleteChars(int count) {
        textField.deleteText(count);
    }

    @Override
    public void moveChar(int direction, boolean selecting) {
        textField.setSelecting(selecting);
        textField.seekCursor(Whence.RELATIVE, direction);
    }

    @Override
    public void moveWord(int direction, boolean selecting) {
        textField.setSelecting(selecting);
        MultilineTextField.StringView wordView = (direction > 0)
            ? textField.getNextWord()
            : textField.getPreviousWord();
        if (wordView != null) {
            textField.seekCursor(Whence.ABSOLUTE, wordView.beginIndex());
        }
    }

    @Override
    public void moveToStart(boolean selecting) {
        textField.setSelecting(selecting);
        textField.seekCursor(Whence.ABSOLUTE, getLineStart());
    }

    @Override
    public void moveToEnd(boolean selecting) {
        textField.setSelecting(selecting);
        textField.seekCursor(Whence.ABSOLUTE, getLineEnd());
    }

    @Override
    public boolean supportsMultiLine() {
        return true;
    }

    @Override
    public void moveLine(int direction, boolean selecting) {
        textField.setSelecting(selecting);
        textField.seekCursorLine(direction);
    }

    @Override
    public int getLineStart() {
        int currentLine = textField.getLineAtCursor();
        return textField.getLineView(currentLine).beginIndex();
    }

    @Override
    public int getLineEnd() {
        int currentLine = textField.getLineAtCursor();
        return textField.getLineView(currentLine).endIndex();
    }

    @Override
    public Object getWidget() {
        return textField;
    }

    @Override
    public int getWordOffset(int direction) {
        int cursor = getCursor();
        MultilineTextField.StringView wordView = (direction > 0)
            ? textField.getNextWord()
            : textField.getPreviousWord();
        if (wordView != null) {
            return wordView.beginIndex() - cursor;
        }
        return 0;
    }
}
