package net.woadwizard.search;

import net.woadwizard.mixin.client.EditBoxInvoker;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;
import java.util.function.BiFunction;

/**
 * Formatter for EditBox that underlines the matched query during search mode.
 * In 1.20.1, EditBox uses a single BiFunction formatter instead of a List<TextFormatter>.
 * This class installs itself by wrapping the existing formatter.
 */
public class SearchFormatter {

    // Track which EditBoxes have our formatter installed (using weak references)
    private static final WeakHashMap<EditBox, OriginalFormatterHolder> installedBoxes = new WeakHashMap<>();

    /**
     * Install the search formatter on the given EditBox.
     * This wraps the existing formatter to add search highlighting.
     */
    public static void install(EditBox editBox) {
        if (installedBoxes.containsKey(editBox)) {
            return; // Already installed
        }

        // Get the current formatter
        BiFunction<String, Integer, FormattedCharSequence> original =
            ((EditBoxInvoker) editBox).getFormatter();

        // Store the original so we can delegate to it
        OriginalFormatterHolder holder = new OriginalFormatterHolder(editBox, original);
        installedBoxes.put(editBox, holder);

        // Install our combined formatter
        ((EditBoxInvoker) editBox).setFormatter(holder::format);
    }

    /**
     * Holder class that stores the original formatter and provides the combined formatting.
     */
    private static class OriginalFormatterHolder {
        private final WeakReference<EditBox> editBoxRef;
        private final BiFunction<String, Integer, FormattedCharSequence> original;

        OriginalFormatterHolder(EditBox editBox, BiFunction<String, Integer, FormattedCharSequence> original) {
            this.editBoxRef = new WeakReference<>(editBox);
            this.original = original;
        }

        FormattedCharSequence format(String text, int cursorPos) {
            EditBox editBox = editBoxRef.get();
            if (editBox == null) {
                return FormattedCharSequence.forward(text, Style.EMPTY);
            }

            // Get base formatting from original formatter
            FormattedCharSequence base = (original != null)
                ? original.apply(text, cursorPos)
                : FormattedCharSequence.forward(text, Style.EMPTY);

            // Check if we should apply search highlighting
            SearchState state = HistorySearch.getCurrentState();
            if (!state.active() || state.selectedMatch() == null || state.query().isEmpty()) {
                return base;
            }

            // Calculate underline range for this text fragment
            int queryPos = state.selectedMatch().queryPosition();
            int queryLen = state.query().length();

            // Determine where this fragment starts in the full text
            int fragmentStart = getFragmentStart(editBox, text);

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
        private int getFragmentStart(EditBox editBox, String text) {
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
