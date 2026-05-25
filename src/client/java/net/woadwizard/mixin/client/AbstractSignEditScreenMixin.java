package net.woadwizard.mixin.client;

import net.woadwizard.KillRing;
import net.woadwizard.emacs.SignLineNavigation;
import net.woadwizard.emacs.TextFieldAdapter;
import net.woadwizard.emacs.TextInputEventHandler;
import net.woadwizard.emacs.adapters.AdapterCache;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
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

    @Shadow
    private TextFieldHelper signField;

    @Shadow
    private int line;

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        int keyCode = event.key();
        int modifiers = event.modifiers();

        TextFieldAdapter adapter = AdapterCache.get(signField);

        SignLineNavigation.Result signNavigation = SignLineNavigation.handle(keyCode, modifiers, line);
        if (signNavigation.handled()) {
            KillRing.clearYankTracking();
            LOGGER.debug("Moved to sign line {}", signNavigation.line());
            line = signNavigation.line();
            signField.setCursorToEnd();
            adapter.getState().deactivateMark();
            cir.setReturnValue(true);
            return;
        }

        if (TextInputEventHandler.handleKeyPress(adapter, keyCode, modifiers)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void onCharTyped(CharacterEvent event, CallbackInfoReturnable<Boolean> cir) {
        TextFieldAdapter adapter = AdapterCache.get(signField);
        if (TextInputEventHandler.handleCharTyped(adapter, event.codepoint(), event.modifiers())) {
            cir.setReturnValue(false);
        }
    }
}
