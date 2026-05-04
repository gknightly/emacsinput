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
 * In 1.21.4, EditBox uses a single BiFunction formatter instead of a List<TextFormatter>.
 * This class installs itself by wrapping the existing formatter.
 */
public class SearchFormatter {

    private static final WeakHashMap<EditBox, OriginalFormatterHolder> installedBoxes = new WeakHashMap<>();

    public static void install(EditBox editBox) {
        if (installedBoxes.containsKey(editBox)) {
            return;
        }

        BiFunction<String, Integer, FormattedCharSequence> original =
            ((EditBoxInvoker) editBox).getFormatter();

        OriginalFormatterHolder holder = new OriginalFormatterHolder(editBox, original);
        installedBoxes.put(editBox, holder);

        ((EditBoxInvoker) editBox).setFormatter(holder::format);
    }

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

            FormattedCharSequence base = (original != null)
                ? original.apply(text, cursorPos)
                : FormattedCharSequence.forward(text, Style.EMPTY);

            SearchState state = HistorySearch.getCurrentState();
            if (!state.active() || state.selectedMatch() == null || state.query().isEmpty()) {
                return base;
            }

            int queryPos = state.selectedMatch().queryPosition();
            int queryLen = state.query().length();

            int fragmentStart = getFragmentStart(editBox, text);

            int matchStart = queryPos - fragmentStart;
            int matchEnd = matchStart + queryLen;

            if (matchStart < text.length() && matchEnd > 0) {
                int underlineStart = Math.max(0, matchStart);
                int underlineEnd = Math.min(text.length(), matchEnd);

                if (underlineStart < underlineEnd) {
                    return new UnderlineWrapper(base, underlineStart, underlineEnd);
                }
            }

            return base;
        }

        private int getFragmentStart(EditBox editBox, String text) {
            String fullText = editBox.getValue();
            int cursorPosition = editBox.getCursorPosition();

            if (fullText.startsWith(text) && text.length() == cursorPosition) {
                return 0;
            }
            return cursorPosition;
        }
    }

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
