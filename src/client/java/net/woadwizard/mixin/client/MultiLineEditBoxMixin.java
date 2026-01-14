package net.woadwizard.mixin.client;

import net.woadwizard.UndoManager;
import net.woadwizard.emacs.EmacsKeyHandler;
import net.woadwizard.emacs.TextFieldAdapter;
import net.woadwizard.emacs.adapters.AdapterCache;
import net.woadwizard.compat.CharacterEvent;
import net.woadwizard.compat.KeyEvent;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.MultilineTextField;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MultiLineEditBox.class, priority = 1100)
public abstract class MultiLineEditBoxMixin {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiLineEditBoxMixin.class);

    @Shadow
    @Final
    private MultilineTextField textField;

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        KeyEvent event = new KeyEvent(keyCode, scanCode, modifiers);
        TextFieldAdapter adapter = AdapterCache.get(textField);
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
        if (EmacsKeyHandler.shouldBlockChar(event.modifiers())) {
            LOGGER.debug("Blocking modified character: codepoint={}", event.codepoint());
            cir.setReturnValue(false);
            return;
        }

        // Record undo state before character is typed (amalgamated)
        TextFieldAdapter adapter = AdapterCache.get(textField);
        UndoManager.recordStateForInsert(adapter.getWidget(), adapter.getState(),
            adapter.getText(), adapter.getCursor());
    }
}
