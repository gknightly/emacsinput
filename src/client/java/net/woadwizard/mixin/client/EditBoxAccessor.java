package net.woadwizard.mixin.client;

import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor interface for EditBox private fields.
 * Used for C-x C-x (exchange point and mark) functionality.
 */
@Mixin(EditBox.class)
public interface EditBoxAccessor {

    @Accessor("highlightPos")
    int getHighlightPos();

    @Accessor("highlightPos")
    void setHighlightPos(int pos);
}
