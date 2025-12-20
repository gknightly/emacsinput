package net.woadwizard.search;

import net.woadwizard.mixin.client.EditBoxInvoker;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.EditBox.TextFormatter;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;

import java.util.List;

/**
 * Formatter for EditBox that underlines the matched query during search mode.
 * The search indicator is rendered separately by ChatScreenMixin.
 */
public class SearchFormatter implements TextFormatter {

    private final EditBox editBox;

    public SearchFormatter(EditBox editBox) {
        this.editBox = editBox;
    }

    @Override
    public FormattedCharSequence format(String text, int cursorPos) {
        SearchState state = HistorySearch.getCurrentState();
        if (!state.active() || state.selectedMatch() == null || state.query().isEmpty()) {
            return null;  // Let other formatters handle it
        }

        // Get base formatting from other formatters (preserves syntax highlighting)
        FormattedCharSequence base = getBaseFormatting(text, cursorPos);

        // Calculate underline range for this text fragment
        int queryPos = state.selectedMatch().queryPosition();
        int queryLen = state.query().length();

        // Determine where this fragment starts in the full text
        int fragmentStart = getFragmentStart(text);

        // Check if the match falls within this fragment
        int matchStart = queryPos - fragmentStart;
        int matchEnd = matchStart + queryLen;

        if (matchStart < text.length() && matchEnd > 0) {
            // Clamp to fragment bounds
            int underlineStart = Math.max(0, matchStart);
            int underlineEnd = Math.min(text.length(), matchEnd);

            if (underlineStart < underlineEnd) {
                return new UnderlineWrapper(base, underlineStart, underlineEnd);
            }
        }

        return base;
    }

    /**
     * Determine where this text fragment starts in the full EditBox text.
     */
    private int getFragmentStart(String text) {
        String fullText = editBox.getValue();
        int cursorPosition = editBox.getCursorPosition();

        // If this fragment is a prefix of full text with length == cursor position,
        // it's the "before cursor" part starting at 0.
        // Otherwise it's the "after cursor" part starting at cursor position.
        if (fullText.startsWith(text) && text.length() == cursorPosition) {
            return 0;
        }
        return cursorPosition;
    }

    /**
     * Get formatting from other formatters (skipping SearchFormatter instances).
     */
    private FormattedCharSequence getBaseFormatting(String text, int cursorPos) {
        List<TextFormatter> formatters = ((EditBoxInvoker) editBox).getFormatters();

        if (formatters != null) {
            for (TextFormatter formatter : formatters) {
                if (formatter instanceof SearchFormatter) {
                    continue;
                }
                FormattedCharSequence result = formatter.format(text, cursorPos);
                if (result != null) {
                    return result;
                }
            }
        }

        return FormattedCharSequence.forward(text, Style.EMPTY);
    }

    /**
     * Wrapper that adds underline style to characters in a specific range.
     */
    private static class UnderlineWrapper implements FormattedCharSequence {
        private final FormattedCharSequence wrapped;
        private final int start;
        private final int end;

        UnderlineWrapper(FormattedCharSequence wrapped, int start, int end) {
            this.wrapped = wrapped;
            this.start = start;
            this.end = end;
        }

        @Override
        public boolean accept(FormattedCharSink sink) {
            int[] pos = {0};
            return wrapped.accept((index, style, codePoint) -> {
                Style effectiveStyle = (pos[0] >= start && pos[0] < end)
                        ? style.withUnderlined(true)
                        : style;
                pos[0]++;
                return sink.accept(index, effectiveStyle, codePoint);
            });
        }
    }
}
