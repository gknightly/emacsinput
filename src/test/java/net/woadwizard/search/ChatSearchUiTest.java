package net.woadwizard.search;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public final class ChatSearchUiTest {

    private final List<String> failures = new ArrayList<>();

    public static List<String> run() {
        ChatSearchUiTest test = new ChatSearchUiTest();
        test.runTests();
        return test.failures;
    }

    private void runTests() {
        testIndicatorForEmptyQuery();
        testIndicatorForMatchingQuery();
        testIndicatorForFailingQuery();
        testSelectsCommandHistoryForSlashInput();
        testSelectsChatHistoryForNormalInput();
    }

    private void testIndicatorForEmptyQuery() {
        SearchState state = new SearchState(true, "", "", null, false, -1);

        assertEquals("empty query indicator", "bck-i-search: ", ChatSearchUi.indicator(state));
    }

    private void testIndicatorForMatchingQuery() {
        SearchState state = new SearchState(true, "gi", "", new HistorySearch.Match("/give", 0, 1), true, 0);

        assertEquals("matching query indicator", "bck-i-search: gi", ChatSearchUi.indicator(state));
    }

    private void testIndicatorForFailingQuery() {
        SearchState state = new SearchState(true, "missing", "draft", null, false, -1);

        assertEquals("failing query indicator", "failing bck-i-search: missing", ChatSearchUi.indicator(state));
    }

    private void testSelectsCommandHistoryForSlashInput() {
        List<String> history = ChatSearchUi.historyForInput(
            "/gi",
            () -> new LinkedHashSet<>(List.of("/help", "/give")),
            () -> List.of("hello")
        );

        assertEquals("slash input uses command history", List.of("/help", "/give"), history);
    }

    private void testSelectsChatHistoryForNormalInput() {
        List<String> history = ChatSearchUi.historyForInput(
            "hello",
            () -> List.of("/help"),
            () -> List.of("hello", "world")
        );

        assertEquals("normal input uses chat history", List.of("hello", "world"), history);
    }

    private void assertEquals(String name, Object expected, Object actual) {
        if ((expected == null && actual != null) || (expected != null && !expected.equals(actual))) {
            failures.add(name + ": expected <" + expected + "> but was <" + actual + ">");
        }
    }
}
