package net.woadwizard.mixin.client;

import net.woadwizard.emacs.EmacsKeyHandler;
import net.woadwizard.emacs.adapters.AdapterCache;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.MultilineTextField;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
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
    private void onKeyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        EmacsKeyHandler.Result result = EmacsKeyHandler.handleKeyPress(
            AdapterCache.get(textField), event.key(), event.modifiers());

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
