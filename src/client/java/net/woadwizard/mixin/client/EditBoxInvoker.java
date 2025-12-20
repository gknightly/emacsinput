package net.woadwizard.mixin.client;

import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(EditBox.class)
public interface EditBoxInvoker {
    @Accessor("formatters")
    List<EditBox.TextFormatter> getFormatters();
}
