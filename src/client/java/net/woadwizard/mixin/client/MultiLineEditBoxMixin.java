package net.woadwizard.mixin.client;

import net.woadwizard.emacs.TextFieldAdapter;
import net.woadwizard.emacs.TextInputEventHandler;
import net.woadwizard.emacs.adapters.AdapterCache;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.MultilineTextField;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MultiLineEditBox.class, priority = 1100)
public abstract class MultiLineEditBoxMixin {

    @Shadow
    @Final
    private MultilineTextField textField;

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        TextFieldAdapter adapter = AdapterCache.get(textField);
        if (TextInputEventHandler.handleKeyPress(adapter, event.key(), event.modifiers())) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void onCharTyped(CharacterEvent event, CallbackInfoReturnable<Boolean> cir) {
        TextFieldAdapter adapter = AdapterCache.get(textField);
        if (TextInputEventHandler.handleCharTyped(adapter, event.codepoint(), event.modifiers())) {
            cir.setReturnValue(false);
        }
    }
}
