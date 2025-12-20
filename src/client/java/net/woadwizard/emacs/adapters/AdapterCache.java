package net.woadwizard.emacs.adapters;

import net.woadwizard.emacs.TextFieldAdapter;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultilineTextField;
import net.minecraft.client.gui.font.TextFieldHelper;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Cache for TextFieldAdapter instances to avoid allocating on every key press.
 * Uses WeakHashMap so adapters are automatically cleaned up when widgets are GC'd.
 */
public final class AdapterCache {

    private static final Map<EditBox, EditBoxAdapter> editBoxAdapters = new WeakHashMap<>();
    private static final Map<MultilineTextField, MultilineTextFieldAdapter> multilineAdapters = new WeakHashMap<>();
    private static final Map<TextFieldHelper, TextFieldHelperAdapter> helperAdapters = new WeakHashMap<>();

    private AdapterCache() {}

    public static TextFieldAdapter get(EditBox editBox) {
        return editBoxAdapters.computeIfAbsent(editBox, EditBoxAdapter::new);
    }

    public static TextFieldAdapter get(MultilineTextField textField) {
        return multilineAdapters.computeIfAbsent(textField, MultilineTextFieldAdapter::new);
    }

    public static TextFieldAdapter get(TextFieldHelper helper) {
        return helperAdapters.computeIfAbsent(helper, TextFieldHelperAdapter::new);
    }
}
