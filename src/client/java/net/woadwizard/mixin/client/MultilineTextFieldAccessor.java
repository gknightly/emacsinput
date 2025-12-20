package net.woadwizard.mixin.client;

import net.minecraft.client.gui.components.MultilineTextField;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor interface for MultilineTextField private fields.
 * Used for C-x C-x (exchange point and mark) functionality.
 */
@Mixin(MultilineTextField.class)
public interface MultilineTextFieldAccessor {

    @Accessor("selectCursor")
    int getSelectCursor();

    @Accessor("selectCursor")
    void setSelectCursor(int pos);
}
