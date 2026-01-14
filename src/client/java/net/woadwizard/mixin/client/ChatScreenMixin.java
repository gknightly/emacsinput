package net.woadwizard.mixin.client;

import net.woadwizard.SelectionHelper;
import net.woadwizard.search.HistorySearch;
import net.woadwizard.search.SearchController;
import net.woadwizard.search.SearchFormatter;
import net.woadwizard.search.SearchModeHandler;
import net.woadwizard.search.SearchState;
import net.woadwizard.config.Command;
import net.woadwizard.compat.KeyEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.stream.Collectors;

@Mixin(value = ChatScreen.class, priority = 1100)
public abstract class ChatScreenMixin {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatScreenMixin.class);

    @Unique
    private final HistorySearch historySearch = new HistorySearch();

    @Shadow
    CommandSuggestions commandSuggestions;

    @Shadow
    protected EditBox input;

    @Shadow
    private int historyPos;

    @Shadow
    public abstract void moveInHistory(int direction);

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        HistorySearch.setCurrent(historySearch);
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void onRemoved(CallbackInfo ci) {
        historySearch.exit();
        HistorySearch.setCurrent(null);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        KeyEvent event = new KeyEvent(keyCode, scanCode, modifiers);
        boolean ctrlHeld = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        boolean altHeld = (modifiers & GLFW.GLFW_MOD_ALT) != 0;

        // Handle search mode keys
        if (historySearch.isActive()) {
            SearchModeHandler.Result result = SearchModeHandler.handleKey(
                keyCode, ctrlHeld, altHeld, input, historySearch);

            if (result == SearchModeHandler.Result.HANDLED) {
                // Update historyPos if search accepted
                if (SearchModeHandler.isAcceptKey(keyCode, ctrlHeld, altHeld)) {
                    historyPos = historySearch.getState().historyIndex();
                }
                restoreCommandSuggestions();
                cir.setReturnValue(true);
                return;
            } else if (result == SearchModeHandler.Result.ACCEPT_AND_CONTINUE) {
                historyPos = historySearch.getState().historyIndex();
                restoreCommandSuggestions();
                // Don't consume - let default handling proceed
            }
        }

        // Handle Ctrl keys for history/search navigation
        if (ctrlHeld) {
            Boolean result = handleCtrlKey(event, keyCode, modifiers);
            if (result != null) {
                cir.setReturnValue(result);
                return;
            }
        }

        // Escape outside search mode: clear selection first, then let native close chat
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && !historySearch.isActive()) {
            if (SelectionHelper.clearSelectionOrMark(input)) {
                LOGGER.debug("Escape: cleared selection/mark");
                cir.setReturnValue(true);
                return;
            }
        }
    }

    @Unique
    private Boolean handleCtrlKey(KeyEvent event, int keyCode, int modifiers) {
        switch (keyCode) {
            case GLFW.GLFW_KEY_P -> {
                if (!Command.CTRL_P.isEnabled()) return null;
                exitSearchIfActive();
                // In 1.20.1, use accessor to check if suggestions are visible (no isVisible() method)
                if (commandSuggestions != null && ((CommandSuggestionsAccessor) commandSuggestions).getSuggestions() != null) {
                    LOGGER.debug("C-p: navigating suggestions up");
                    // In 1.20.1, keyPressed takes primitives not KeyEvent
                    return commandSuggestions.keyPressed(GLFW.GLFW_KEY_UP, event.scancode(), modifiers & ~GLFW.GLFW_MOD_CONTROL);
                } else {
                    LOGGER.debug("C-p: previous history");
                    moveInHistory(-1);
                    return true;
                }
            }
            case GLFW.GLFW_KEY_N -> {
                if (!Command.CTRL_N.isEnabled()) return null;
                exitSearchIfActive();
                // In 1.20.1, use accessor to check if suggestions are visible (no isVisible() method)
                if (commandSuggestions != null && ((CommandSuggestionsAccessor) commandSuggestions).getSuggestions() != null) {
                    LOGGER.debug("C-n: navigating suggestions down");
                    // In 1.20.1, keyPressed takes primitives not KeyEvent
                    return commandSuggestions.keyPressed(GLFW.GLFW_KEY_DOWN, event.scancode(), modifiers & ~GLFW.GLFW_MOD_CONTROL);
                } else {
                    LOGGER.debug("C-n: next history");
                    moveInHistory(1);
                    return true;
                }
            }
            case GLFW.GLFW_KEY_R -> {
                if (!Command.CTRL_R.isEnabled()) return null;
                if (!historySearch.isActive()) {
                    LOGGER.debug("C-r: entering search mode");
                    enterSearchMode();
                }
                return true;
            }
            case GLFW.GLFW_KEY_S -> {
                if (!Command.CTRL_S.isEnabled()) return null;
                if (!historySearch.isActive()) {
                    LOGGER.debug("C-s: entering search mode");
                    enterSearchMode();
                }
                return true;
            }
            case GLFW.GLFW_KEY_G -> {
                if (!Command.CTRL_G.isEnabled()) return null;
                if (!historySearch.isActive()) {
                    if (SelectionHelper.clearSelectionOrMark(input)) {
                        LOGGER.debug("C-g: cleared selection/mark");
                        return true;
                    }
                    LOGGER.debug("C-g: closing chat");
                    Minecraft.getInstance().setScreen(null);
                    return true;
                }
            }
        }
        return null;
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!historySearch.isActive()) {
            return;
        }

        SearchState state = historySearch.getState();
        String indicator = buildIndicator(state);

        var font = Minecraft.getInstance().font;
        int x = input.getX();
        int y = input.getY() - font.lineHeight - 2;

        graphics.drawString(font, indicator, x, y, 0xFFFFFFFF, true);
    }

    @Unique
    private String buildIndicator(SearchState state) {
        String type = (state.hasMatches() || state.query().isEmpty())
                ? "bck-i-search"
                : "failing bck-i-search";
        return type + ": " + state.query();
    }

    @Unique
    private void enterSearchMode() {
        addSearchFormatterIfNeeded();
        input.setSuggestion(null);
        historySearch.enter(input.getValue(), getSearchHistory(input.getValue()));
        SearchController.syncToEditBox(input, historySearch.getState());
    }

    @Unique
    private void exitSearchIfActive() {
        if (historySearch.isActive()) {
            historyPos = SearchController.acceptAndExit(input, historySearch);
            restoreCommandSuggestions();
        }
    }

    @Unique
    private void restoreCommandSuggestions() {
        if (commandSuggestions != null) {
            commandSuggestions.updateCommandInfo();
        }
    }

    @Unique
    private void addSearchFormatterIfNeeded() {
        // In 1.20.1, EditBox uses a single formatter BiFunction instead of a List<TextFormatter>
        // Install the SearchFormatter which wraps the existing formatter
        SearchFormatter.install(input);
    }

    @Unique
    private List<String> getSearchHistory(String currentInput) {
        List<String> recentChat = Minecraft.getInstance().gui.getChat().getRecentChat();
        if (currentInput.startsWith("/")) {
            // In 1.20.1, there's no separate command history - filter chat history for commands
            return recentChat.stream()
                .filter(s -> s.startsWith("/"))
                .collect(Collectors.toList());
        }
        return recentChat;
    }
}
