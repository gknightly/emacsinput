package net.woadwizard.mixin.client;

import net.woadwizard.KillRing;
import net.woadwizard.SelectionHelper;
import net.woadwizard.search.ChatKeyHandler;
import net.woadwizard.search.ChatSearchUi;
import net.woadwizard.search.HistorySearch;
import net.woadwizard.search.SearchController;
import net.woadwizard.search.SearchFormatter;
import net.woadwizard.search.SearchModeHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.KeyEvent;
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
    private void onKeyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        int keyCode = event.key();
        int modifiers = event.modifiers();
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

        ChatKeyHandler.Result chatKeyResult = ChatKeyHandler.handleCtrlKey(
            keyCode, modifiers, commandSuggestions != null && commandSuggestions.isVisible());
        if (chatKeyResult.handled()) {
            cir.setReturnValue(performChatAction(chatKeyResult.action(), event, modifiers));
            return;
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
    private boolean performChatAction(ChatKeyHandler.Action action, KeyEvent event, int modifiers) {
        if (action != ChatKeyHandler.Action.NONE) {
            KillRing.clearYankTracking();
        }
        return switch (action) {
            case PREVIOUS_SUGGESTION -> {
                exitSearchIfActive();
                LOGGER.debug("C-p: navigating suggestions up");
                KeyEvent upEvent = new KeyEvent(GLFW.GLFW_KEY_UP, event.scancode(), modifiers & ~GLFW.GLFW_MOD_CONTROL);
                yield commandSuggestions != null && commandSuggestions.keyPressed(upEvent);
            }
            case NEXT_SUGGESTION -> {
                exitSearchIfActive();
                LOGGER.debug("C-n: navigating suggestions down");
                KeyEvent downEvent = new KeyEvent(GLFW.GLFW_KEY_DOWN, event.scancode(), modifiers & ~GLFW.GLFW_MOD_CONTROL);
                yield commandSuggestions != null && commandSuggestions.keyPressed(downEvent);
            }
            case PREVIOUS_HISTORY -> {
                exitSearchIfActive();
                LOGGER.debug("C-p: previous history");
                moveInHistory(-1);
                yield true;
            }
            case NEXT_HISTORY -> {
                exitSearchIfActive();
                LOGGER.debug("C-n: next history");
                moveInHistory(1);
                yield true;
            }
            case ENTER_SEARCH_BACKWARD -> {
                LOGGER.debug("C-r: entering search mode");
                enterSearchMode();
                yield true;
            }
            case ENTER_SEARCH_FORWARD -> {
                LOGGER.debug("C-s: entering search mode");
                enterSearchMode();
                yield true;
            }
            case CANCEL_OR_CLOSE -> {
                if (SelectionHelper.clearSelectionOrMark(input)) {
                    LOGGER.debug("C-g: cleared selection/mark");
                } else {
                    LOGGER.debug("C-g: closing chat");
                    Minecraft.getInstance().setScreen(null);
                }
                yield true;
            }
            case NONE -> false;
        };
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!historySearch.isActive()) {
            return;
        }

        String indicator = ChatSearchUi.indicator(historySearch.getState());

        var font = Minecraft.getInstance().font;
        int x = input.getX();
        int y = input.getY() - font.lineHeight - 2;

        graphics.drawString(font, indicator, x, y, 0xFFFFFFFF, true);
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
        List<EditBox.TextFormatter> formatters = ((EditBoxInvoker) input).getFormatters();
        boolean hasSearchFormatter = formatters.stream().anyMatch(f -> f instanceof SearchFormatter);
        if (!hasSearchFormatter) {
            formatters.add(0, new SearchFormatter(input));
        }
    }

    @Unique
    private List<String> getSearchHistory(String currentInput) {
        return ChatSearchUi.historyForInput(
            currentInput,
            () -> Minecraft.getInstance().commandHistory().history(),
            () -> Minecraft.getInstance().gui.getChat().getRecentChat()
        );
    }
}
