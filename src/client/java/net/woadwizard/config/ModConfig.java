package net.woadwizard.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for Emacs Input mod.
 *
 * Option precedence (highest to lowest):
 * 1. enabled (master switch)
 * 2. Individual command overrides (enabledCommands / disabledCommands)
 * 3. ctrlKeybindsEnabled / altKeybindsEnabled (modifier switches)
 * 4. Feature switches (navigationEnabled, killRingEnabled, etc.)
 *
 * For example, if ctrlKeybindsEnabled=false, then C-y (yank) is disabled
 * even if killRingEnabled=true, because C-y requires Ctrl.
 * However, adding "C-y" to enabledCommands would force it on regardless.
 */
@Config(name = "emacsinput")
public class ModConfig implements ConfigData {

    @ConfigEntry.Gui.Tooltip
    public boolean enabled = true;

    @ConfigEntry.Gui.CollapsibleObject
    public CommandOverrides commandOverrides = new CommandOverrides();

    @ConfigEntry.Gui.CollapsibleObject
    public Keybinds keybinds = new Keybinds();

    @ConfigEntry.Gui.CollapsibleObject
    public HistorySearch historySearch = new HistorySearch();

    @ConfigEntry.Gui.CollapsibleObject
    public Options options = new Options();

    public static class CommandOverrides {
        @ConfigEntry.Gui.Tooltip
        public List<String> enabledCommands = new ArrayList<>();

        @ConfigEntry.Gui.Tooltip
        public List<String> disabledCommands = new ArrayList<>();
    }

    public static class Keybinds {
        @ConfigEntry.Gui.Tooltip
        public boolean ctrlKeybindsEnabled = true;

        @ConfigEntry.Gui.Tooltip
        public boolean altKeybindsEnabled = true;

        @ConfigEntry.Gui.Tooltip
        public boolean navigationEnabled = true;

        @ConfigEntry.Gui.Tooltip
        public boolean killRingEnabled = true;

        @ConfigEntry.Gui.Tooltip
        public boolean undoEnabled = true;

        @ConfigEntry.Gui.Tooltip
        public boolean transposeEnabled = true;

        @ConfigEntry.Gui.Tooltip
        public boolean markEnabled = true;
    }

    public static class HistorySearch {
        @ConfigEntry.Gui.Tooltip
        public boolean enabled = true;

        @ConfigEntry.Gui.Tooltip
        public boolean caseSensitive = false;
    }

    public static class Options {
        @ConfigEntry.Gui.Tooltip
        public boolean killWordOnCw = true;

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public AltKeyBehavior altKeyBehavior = AltKeyBehavior.BLOCK_ALL;
    }

    public enum AltKeyBehavior {
        /** Alt/Option always blocks character input (default Emacs behavior) */
        BLOCK_ALL,
        /** Alt/Option only blocks when bound to an Emacs command */
        BLOCK_WHEN_BOUND
    }
}
