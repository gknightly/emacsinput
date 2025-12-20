package net.woadwizard.mixin.client;

import net.woadwizard.SelectionHelper;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.MultilineTextField;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BookEditScreen.class, priority = 1100)
public class BookEditScreenMixin {

    private static final Logger LOGGER = LoggerFactory.getLogger(BookEditScreenMixin.class);

    @Shadow
    @Final
    private MultiLineEditBox page;

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        // Handle Escape: clear selection first, then let native close screen
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            MultilineTextField textField = ((MultiLineEditBoxAccessor) page).getTextField();
            if (SelectionHelper.clearSelectionOrMark(textField)) {
                LOGGER.debug("Escape: cleared selection/mark");
                cir.setReturnValue(true);
                return;
            }
            // Nothing to clear - let native handler close the screen
        }
    }
}
