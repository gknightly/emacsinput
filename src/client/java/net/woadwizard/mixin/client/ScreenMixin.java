package net.woadwizard.mixin.client;

import net.woadwizard.KillRing;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Clears global Emacs state when any screen is closed.
 * Note: Mark state is now per-widget and cleaned up automatically via WeakHashMap.
 * HistorySearch is now per-ChatScreen (handled in ChatScreenMixin).
 */
@Mixin(value = Screen.class, priority = 1100)
public class ScreenMixin {

    @Inject(method = "removed", at = @At("HEAD"))
    private void onRemoved(CallbackInfo ci) {
        // Clear yank tracking - prevents M-y from working across screens
        KillRing.clearYankTracking();
    }
}
