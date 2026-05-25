package net.woadwizard.mixin.client;

import net.woadwizard.search.HistorySearch;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.CommandSuggestions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to hide command suggestions and usage hints during reverse search mode.
 */
@Mixin(value = CommandSuggestions.class, priority = 1100)
public class CommandSuggestionsMixin {

    /**
     * Skip rendering the entire suggestions UI during search mode.
     */
    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void onExtractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo ci) {
        if (HistorySearch.isSearchActive()) {
            ci.cancel();
        }
    }

    /**
     * Skip rendering the command usage hints (e.g., "<gamemode> [<target>]")
     * when in history search mode.
     */
    @Inject(method = "extractUsage", at = @At("HEAD"), cancellable = true)
    private void onExtractUsage(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (HistorySearch.isSearchActive()) {
            ci.cancel();
        }
    }
}
