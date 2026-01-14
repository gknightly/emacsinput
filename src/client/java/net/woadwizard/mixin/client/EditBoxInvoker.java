package net.woadwizard.mixin.client;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.function.BiFunction;

/**
 * Accessor for EditBox private fields.
 * In 1.20.1, EditBox uses a single BiFunction formatter, not a List<TextFormatter>.
 */
@Mixin(EditBox.class)
public interface EditBoxInvoker {
    @Accessor("formatter")
    BiFunction<String, Integer, FormattedCharSequence> getFormatter();

    @Accessor("formatter")
    void setFormatter(BiFunction<String, Integer, FormattedCharSequence> formatter);
}
