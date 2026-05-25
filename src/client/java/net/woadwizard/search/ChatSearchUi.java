package net.woadwizard.search;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

/**
 * Presentation and source-selection helpers for chat history search.
 */
public final class ChatSearchUi {

    private ChatSearchUi() {}

    public static String indicator(SearchState state) {
        String type = (state.hasMatches() || state.query().isEmpty())
            ? "bck-i-search"
            : "failing bck-i-search";
        return type + ": " + state.query();
    }

    public static List<String> historyForInput(
        String currentInput,
        Supplier<Collection<String>> commandHistory,
        Supplier<List<String>> chatHistory
    ) {
        if (currentInput.startsWith("/")) {
            Collection<String> commands = commandHistory.get();
            return commands instanceof List<String> list ? list : List.copyOf(commands);
        }
        return chatHistory.get();
    }
}
