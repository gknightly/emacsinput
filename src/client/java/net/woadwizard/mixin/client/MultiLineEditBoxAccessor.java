package net.woadwizard.mixin.client;

import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.MultilineTextField;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor interface for MultiLineEditBox to get the underlying MultilineTextField.
 */
@Mixin(MultiLineEditBox.class)
public interface MultiLineEditBoxAccessor {

    @Accessor("textField")
    MultilineTextField getTextField();
}
