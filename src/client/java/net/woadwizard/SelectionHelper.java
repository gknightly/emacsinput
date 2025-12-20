package net.woadwizard;

import net.woadwizard.emacs.TextFieldAdapter;
import net.woadwizard.emacs.WidgetState;
import net.woadwizard.emacs.adapters.AdapterCache;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultilineTextField;

import java.util.Objects;

/**
 * Utility methods for clearing selection/mark in different text field types.
 */
public final class SelectionHelper {

    private SelectionHelper() {}

    /**
     * Clear selection and mark for any text field using its adapter.
     * @return true if something was cleared (mark was active or selection existed)
     */
    public static boolean clearSelectionOrMark(TextFieldAdapter adapter) {
        Objects.requireNonNull(adapter, "adapter must not be null");

        boolean hadSelection = adapter.hasSelection();
        WidgetState state = adapter.getState();
        boolean hadMark = state.deactivateIfActive();

        if (hadMark || hadSelection) {
            adapter.collapseSelection();
            return true;
        }
        return false;
    }

    /**
     * Clear selection and mark for an EditBox.
     * @return true if something was cleared (mark was active or selection existed)
     */
    public static boolean clearSelectionOrMark(EditBox editBox) {
        Objects.requireNonNull(editBox, "editBox must not be null");
        return clearSelectionOrMark(AdapterCache.get(editBox));
    }

    /**
     * Clear selection and mark for a MultilineTextField.
     * @return true if something was cleared (mark was active or selection existed)
     */
    public static boolean clearSelectionOrMark(MultilineTextField textField) {
        Objects.requireNonNull(textField, "textField must not be null");
        return clearSelectionOrMark(AdapterCache.get(textField));
    }
}
