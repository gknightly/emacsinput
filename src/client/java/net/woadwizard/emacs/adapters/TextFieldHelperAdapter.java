package net.woadwizard.emacs.adapters;

import net.woadwizard.emacs.TextFieldAdapter;
import net.woadwizard.emacs.WidgetState;
import net.woadwizard.emacs.WordBoundary;
import net.woadwizard.mixin.client.TextFieldHelperAccessor;
import net.minecraft.client.gui.font.TextFieldHelper;

/**
 * Adapter for TextFieldHelper (used in sign editing).
 */
public class TextFieldHelperAdapter implements TextFieldAdapter {

    private final TextFieldHelper helper;
    private final TextFieldHelperAccessor accessor;
    private final WidgetState state;

    public TextFieldHelperAdapter(TextFieldHelper helper) {
        this.helper = helper;
        this.accessor = (TextFieldHelperAccessor) helper;
        this.state = new WidgetState();
    }

    @Override
    public WidgetState getState() {
        return state;
    }

    @Override
    public String getText() {
        return accessor.getGetMessageFn().get();
    }

    @Override
    public void setText(String text) {
        accessor.getSetMessageFn().accept(text);
    }

    @Override
    public int getCursor() {
        return accessor.getCursorPos();
    }

    @Override
    public void setCursor(int pos) {
        accessor.setCursorPos(pos);
    }

    @Override
    public int getSelectionStart() {
        return accessor.getSelectionPos();
    }

    @Override
    public void setSelectionStart(int pos) {
        accessor.setSelectionPos(pos);
    }

    @Override
    public boolean hasSelection() {
        return getCursor() != getSelectionStart();
    }

    @Override
    public String getSelectedText() {
        int cursor = getCursor();
        int selection = getSelectionStart();
        int start = Math.min(cursor, selection);
        int end = Math.max(cursor, selection);
        String text = getText();
        if (start >= 0 && end <= text.length()) {
            return text.substring(start, end);
        }
        return "";
    }

    @Override
    public void insertText(String text) {
        helper.insertText(text);
    }

    @Override
    public void deleteChars(int count) {
        helper.removeCharsFromCursor(count);
    }

    @Override
    public void moveChar(int direction, boolean selecting) {
        helper.moveByChars(direction, selecting);
    }

    @Override
    public void moveWord(int direction, boolean selecting) {
        helper.moveByWords(direction, selecting);
    }

    @Override
    public void moveToStart(boolean selecting) {
        helper.setCursorToStart(selecting);
    }

    @Override
    public void moveToEnd(boolean selecting) {
        helper.setCursorToEnd(selecting);
    }

    @Override
    public Object getWidget() {
        return helper;
    }

    @Override
    public int getLineStart() {
        return 0; // Signs are single line per row
    }

    @Override
    public int getLineEnd() {
        return getText().length();
    }

    @Override
    public void collapseSelection() {
        accessor.setSelectionPos(accessor.getCursorPos());
    }

    @Override
    public int getWordOffset(int direction) {
        // TextFieldHelper doesn't expose word position directly
        return WordBoundary.findOffset(getText(), getCursor(), direction);
    }
}
