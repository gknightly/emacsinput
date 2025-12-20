package net.woadwizard.config;

import me.shedaniel.autoconfig.AutoConfig;

import java.util.List;

/**
 * Helper class for accessing mod configuration.
 *
 * Command precedence (highest to lowest):
 * 1. Master switch (enabled)
 * 2. Individual command overrides (disabledCommands > enabledCommands)
 * 3. Modifier switches (ctrlKeybindsEnabled / altKeybindsEnabled)
 * 4. Category switches (navigationEnabled, killRingEnabled, etc.)
 */
public final class ConfigHelper {

    private ConfigHelper() {}

    /**
     * Get the current mod configuration.
     */
    public static ModConfig get() {
        return AutoConfig.getConfigHolder(ModConfig.class).getConfig();
    }

    /**
     * Check if the mod is enabled.
     */
    public static boolean isEnabled() {
        return get().enabled;
    }

    // ========== Command Override System ==========

    /**
     * Check if a command is force-enabled via the enabledCommands list.
     * Command names are case-insensitive.
     */
    public static boolean isCommandForceEnabled(String command) {
        List<String> enabled = get().commandOverrides.enabledCommands;
        return enabled.stream().anyMatch(c -> c.equalsIgnoreCase(command));
    }

    /**
     * Check if a command is force-disabled via the disabledCommands list.
     * Command names are case-insensitive.
     * disabledCommands takes precedence over enabledCommands.
     */
    public static boolean isCommandForceDisabled(String command) {
        List<String> disabled = get().commandOverrides.disabledCommands;
        return disabled.stream().anyMatch(c -> c.equalsIgnoreCase(command));
    }

    /**
     * Check if a command is enabled, considering overrides.
     * @param command The command name (e.g., "C-f", "M-b")
     * @param modifierEnabled Whether the modifier (Ctrl/Alt) is enabled
     * @param categoryEnabled Whether the category (navigation, killRing, etc.) is enabled
     * @return true if the command should be active
     */
    public static boolean isCommandEnabled(String command, boolean modifierEnabled, boolean categoryEnabled) {
        if (!isEnabled()) {
            return false;
        }

        // Check force-disabled first (highest priority override)
        if (isCommandForceDisabled(command)) {
            return false;
        }

        // Check force-enabled (bypasses modifier and category)
        if (isCommandForceEnabled(command)) {
            return true;
        }

        // Fall back to modifier AND category check
        return modifierEnabled && categoryEnabled;
    }

    // ========== Modifier Switches ==========

    /**
     * Check if Ctrl keybinds are enabled (not considering individual overrides).
     */
    public static boolean isCtrlEnabled() {
        return isEnabled() && get().keybinds.ctrlKeybindsEnabled;
    }

    /**
     * Check if Alt keybinds are enabled (not considering individual overrides).
     */
    public static boolean isAltEnabled() {
        return isEnabled() && get().keybinds.altKeybindsEnabled;
    }

    // ========== Other Settings ==========

    /**
     * Check if history search is case sensitive.
     */
    public static boolean isHistorySearchCaseSensitive() {
        return get().historySearch.caseSensitive;
    }

    /**
     * Get the Alt key behavior for Mac.
     */
    public static ModConfig.AltKeyBehavior getAltKeyBehavior() {
        return get().options.altKeyBehavior;
    }

    /**
     * Check if kill-word behavior is enabled for C-w.
     * When enabled and no selection exists, C-w kills backward to whitespace.
     */
    public static boolean isKillWordOnCw() {
        return get().options.killWordOnCw;
    }
}
