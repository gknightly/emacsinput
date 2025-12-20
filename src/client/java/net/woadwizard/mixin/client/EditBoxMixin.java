package net.woadwizard.mixin.client;

import net.woadwizard.search.HistorySearch;
import net.woadwizard.search.SearchController;
import net.woadwizard.emacs.EmacsKeyHandler;
import net.woadwizard.emacs.adapters.AdapterCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EditBox.class, priority = 1100)
public abstract class EditBoxMixin {

    private static final Logger LOGGER = LoggerFactory.getLogger(EditBoxMixin.class);

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        EditBox self = (EditBox)(Object)this;

        EmacsKeyHandler.Result result = EmacsKeyHandler.handleKeyPress(
            AdapterCache.get(self), event.key(), event.modifiers());

        if (result == EmacsKeyHandler.Result.HANDLED) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void onCharTyped(CharacterEvent event, CallbackInfoReturnable<Boolean> cir) {
        // Handle search mode character input (only on ChatScreen)
        HistorySearch current = HistorySearch.getCurrent();
        if (current != null && current.isActive() && Minecraft.getInstance().screen instanceof ChatScreen) {
            char c = (char) event.codepoint();
            LOGGER.debug("Search mode charTyped: '{}'", c);

            EditBox self = (EditBox)(Object)this;
            current.appendToQuery(c);
            SearchController.syncToEditBox(self, current.getState());

            cir.setReturnValue(true);
            return;
        }

        // Block Alt-modified characters (macOS sends special chars)
        if (EmacsKeyHandler.shouldBlockChar(event.modifiers())) {
            LOGGER.debug("Blocking Alt character: codepoint={}", event.codepoint());
            cir.setReturnValue(false);
        }
    }

    /**
     * Block suggestion ghost text during search mode.
     */
    @Inject(method = "setSuggestion", at = @At("HEAD"), cancellable = true)
    private void onSetSuggestion(String suggestion, CallbackInfo ci) {
        if (HistorySearch.isSearchActive()) {
            ci.cancel();
        }
    }
}
