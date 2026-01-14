package net.woadwizard.mixin.client;

import net.woadwizard.UndoManager;
import net.woadwizard.search.HistorySearch;
import net.woadwizard.search.SearchController;
import net.woadwizard.emacs.EmacsKeyHandler;
import net.woadwizard.emacs.TextFieldAdapter;
import net.woadwizard.emacs.adapters.AdapterCache;
import net.woadwizard.compat.CharacterEvent;
import net.woadwizard.compat.KeyEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import org.lwjgl.glfw.GLFW;
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
    private void onKeyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        KeyEvent event = new KeyEvent(keyCode, scanCode, modifiers);
        EditBox self = (EditBox)(Object)this;
        TextFieldAdapter adapter = AdapterCache.get(self);

        EmacsKeyHandler.Result result = EmacsKeyHandler.handleKeyPress(
            adapter, event.key(), event.modifiers());

        if (result == EmacsKeyHandler.Result.HANDLED) {
            cir.setReturnValue(true);
        } else {
            // Record undo state before vanilla text-modifying keys
            int key = event.key();
            if (key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE) {
                UndoManager.recordStateForDelete(adapter.getWidget(), adapter.getState(),
                    adapter.getText(), adapter.getCursor());
            }
        }
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void onCharTyped(char c, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        CharacterEvent event = new CharacterEvent(c, modifiers);
        // Handle search mode character input (only on ChatScreen)
        HistorySearch current = HistorySearch.getCurrent();
        if (current != null && current.isActive() && Minecraft.getInstance().screen instanceof ChatScreen) {
            LOGGER.debug("Search mode charTyped: '{}'", c);

            EditBox self = (EditBox)(Object)this;
            current.appendToQuery(c);
            SearchController.syncToEditBox(self, current.getState());

            cir.setReturnValue(true);
            return;
        }

        // Block Ctrl/Alt-modified characters that would interfere with keybinds
        if (EmacsKeyHandler.shouldBlockChar(event.modifiers())) {
            LOGGER.debug("Blocking modified character: codepoint={}", event.codepoint());
            cir.setReturnValue(false);
            return;
        }

        // Record undo state before character is typed (amalgamated)
        EditBox self = (EditBox)(Object)this;
        TextFieldAdapter adapter = AdapterCache.get(self);
        UndoManager.recordStateForInsert(adapter.getWidget(), adapter.getState(),
            adapter.getText(), adapter.getCursor());
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
