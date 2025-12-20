package net.woadwizard.mixin.client;

import net.woadwizard.config.Command;
import net.woadwizard.emacs.EmacsKeyHandler;
import net.woadwizard.emacs.TextFieldAdapter;
import net.woadwizard.emacs.adapters.AdapterCache;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AbstractSignEditScreen.class, priority = 1100)
public abstract class AbstractSignEditScreenMixin {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSignEditScreenMixin.class);
    private static final int SIGN_LINE_COUNT = 4;

    @Shadow
    private TextFieldHelper signField;

    @Shadow
    private int line;

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        int keyCode = event.key();
        int modifiers = event.modifiers();
        boolean ctrlHeld = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;

        TextFieldAdapter adapter = AdapterCache.get(signField);

        // Handle C-p/C-n specially for sign line navigation
        if (ctrlHeld) {
            if (keyCode == GLFW.GLFW_KEY_P && Command.CTRL_P.isEnabled()) {
                LOGGER.debug("C-p: previous sign line");
                line = Math.floorMod(line - 1, SIGN_LINE_COUNT);
                signField.setCursorToEnd();
                adapter.getState().deactivateMark();
                cir.setReturnValue(true);
                return;
            }
            if (keyCode == GLFW.GLFW_KEY_N && Command.CTRL_N.isEnabled()) {
                LOGGER.debug("C-n: next sign line");
                line = (line + 1) % SIGN_LINE_COUNT;
                signField.setCursorToEnd();
                adapter.getState().deactivateMark();
                cir.setReturnValue(true);
                return;
            }
        }

        // Handle all other Emacs bindings via shared handler
        EmacsKeyHandler.Result result = EmacsKeyHandler.handleKeyPress(
            adapter, keyCode, modifiers);

        if (result == EmacsKeyHandler.Result.HANDLED) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void onCharTyped(CharacterEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (EmacsKeyHandler.shouldBlockChar(event.modifiers())) {
            LOGGER.debug("Blocking Alt character: codepoint={}", event.codepoint());
            cir.setReturnValue(false);
        }
    }
}
