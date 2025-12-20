package net.woadwizard.mixin.client;

import net.minecraft.client.gui.font.TextFieldHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Accessor interface for TextFieldHelper private fields.
 * Used for C-x C-x (exchange point and mark) functionality.
 */
@Mixin(TextFieldHelper.class)
public interface TextFieldHelperAccessor {

    @Accessor("selectionPos")
    int getSelectionPos();

    @Accessor("selectionPos")
    void setSelectionPos(int pos);

    @Accessor("cursorPos")
    int getCursorPos();

    @Accessor("cursorPos")
    void setCursorPos(int pos);

    @Accessor("getMessageFn")
    Supplier<String> getGetMessageFn();

    @Accessor("setMessageFn")
    Consumer<String> getSetMessageFn();
}
