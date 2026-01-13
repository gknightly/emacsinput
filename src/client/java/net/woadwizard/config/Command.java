package net.woadwizard.config;

import net.woadwizard.KillRing;
import net.woadwizard.UndoManager;
import net.woadwizard.emacs.EmacsKeyHandler;
import net.woadwizard.emacs.GraphemeUtils;
import net.woadwizard.emacs.TextFieldAdapter;
import net.woadwizard.emacs.TextOperations;
import net.woadwizard.emacs.WidgetState;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

/**
 * Enum representing all Emacs commands with their configuration metadata and actions.
 * Centralizes command definitions to eliminate repetition in ConfigHelper and EmacsKeyHandler.
 */
public enum Command {
    // Navigation commands - Ctrl
    CTRL_F("C-f", Modifier.CTRL, Category.NAVIGATION, GLFW.GLFW_KEY_F,
           (field, selecting) -> { field.moveChar(1, selecting); return Result.HANDLED; }),

    CTRL_B("C-b", Modifier.CTRL, Category.NAVIGATION, GLFW.GLFW_KEY_B,
           (field, selecting) -> { field.moveChar(-1, selecting); return Result.HANDLED; }),

    CTRL_A("C-a", Modifier.CTRL, Category.NAVIGATION, GLFW.GLFW_KEY_A,
           (field, selecting) -> { field.moveToStart(selecting); return Result.HANDLED; }),

    CTRL_E("C-e", Modifier.CTRL, Category.NAVIGATION, GLFW.GLFW_KEY_E,
           (field, selecting) -> { field.moveToEnd(selecting); return Result.HANDLED; }),

    CTRL_P("C-p", Modifier.CTRL, Category.NAVIGATION, GLFW.GLFW_KEY_P,
           (field, selecting) -> {
               if (field.supportsMultiLine()) {
                   field.moveLine(-1, selecting);
                   return Result.HANDLED;
               }
               return Result.PASS_THROUGH;
           }),

    CTRL_N("C-n", Modifier.CTRL, Category.NAVIGATION, GLFW.GLFW_KEY_N,
           (field, selecting) -> {
               if (field.supportsMultiLine()) {
                   field.moveLine(1, selecting);
                   return Result.HANDLED;
               }
               return Result.PASS_THROUGH;
           }),

    // Navigation commands - Alt
    META_F("M-f", Modifier.ALT, Category.NAVIGATION, GLFW.GLFW_KEY_F,
           (field, selecting) -> { field.moveWord(1, selecting); return Result.HANDLED; }),

    META_B("M-b", Modifier.ALT, Category.NAVIGATION, GLFW.GLFW_KEY_B,
           (field, selecting) -> { field.moveWord(-1, selecting); return Result.HANDLED; }),

    // Kill ring commands - Ctrl
    CTRL_D("C-d", Modifier.CTRL, Category.KILL_RING, GLFW.GLFW_KEY_D,
           (field, selecting) -> {
               WidgetState state = field.getState();
               String text = field.getText();
               int cursor = field.getCursor();
               if (cursor < text.length()) {
                   UndoManager.recordState(field.getWidget(), state, text, cursor);
                   // Delete one grapheme (handles emoji and combining characters)
                   int nextBoundary = GraphemeUtils.nextGraphemeBoundary(text, cursor);
                   field.deleteChars(nextBoundary - cursor);
               }
               state.deactivateMark();
               return Result.HANDLED;
           }),

    CTRL_K("C-k", Modifier.CTRL, Category.KILL_RING, GLFW.GLFW_KEY_K,
           (field, selecting) -> { TextOperations.killToLineEnd(field); return Result.HANDLED; }),

    CTRL_U("C-u", Modifier.CTRL, Category.KILL_RING, GLFW.GLFW_KEY_U,
           (field, selecting) -> { TextOperations.killToLineStart(field); return Result.HANDLED; }),

    CTRL_W("C-w", Modifier.CTRL, Category.KILL_RING, GLFW.GLFW_KEY_W,
           (field, selecting) -> { TextOperations.killRegion(field); return Result.HANDLED; }),

    CTRL_Y("C-y", Modifier.CTRL, Category.KILL_RING, GLFW.GLFW_KEY_Y,
           (field, selecting) -> { TextOperations.yank(field); return Result.HANDLED; }),

    // Kill ring commands - Alt
    META_D("M-d", Modifier.ALT, Category.KILL_RING, GLFW.GLFW_KEY_D,
           (field, selecting) -> { TextOperations.killWord(field, 1); return Result.HANDLED; }),

    META_BACKSPACE("M-Backspace", Modifier.ALT, Category.KILL_RING, GLFW.GLFW_KEY_BACKSPACE,
           (field, selecting) -> { TextOperations.killWord(field, -1); return Result.HANDLED; }),

    META_W("M-w", Modifier.ALT, Category.KILL_RING, GLFW.GLFW_KEY_W,
           (field, selecting) -> { TextOperations.copyRegion(field); return Result.HANDLED; }),

    META_Y("M-y", Modifier.ALT, Category.KILL_RING, GLFW.GLFW_KEY_Y,
           (field, selecting) -> { TextOperations.yankPop(field); return Result.HANDLED; }),

    // Undo/Redo
    CTRL_SLASH("C-/", Modifier.CTRL, Category.UNDO, GLFW.GLFW_KEY_SLASH,
           (field, selecting) -> { TextOperations.performUndo(field); return Result.HANDLED; }),

    CTRL_SHIFT_SLASH("C-S-/", Modifier.CTRL, Category.UNDO, GLFW.GLFW_KEY_SLASH,
           (field, selecting) -> { TextOperations.performRedo(field); return Result.HANDLED; }),

    // Transpose
    CTRL_T("C-t", Modifier.CTRL, Category.TRANSPOSE, GLFW.GLFW_KEY_T,
           (field, selecting) -> { TextOperations.transposeCharacters(field); return Result.HANDLED; }),

    META_T("M-t", Modifier.ALT, Category.TRANSPOSE, GLFW.GLFW_KEY_T,
           (field, selecting) -> { TextOperations.transposeWords(field); return Result.HANDLED; }),

    // Case conversion commands - Alt
    META_U("M-u", Modifier.ALT, Category.CASE_CONVERSION, GLFW.GLFW_KEY_U,
           (field, selecting) -> { TextOperations.uppercaseWord(field); return Result.HANDLED; }),

    META_L("M-l", Modifier.ALT, Category.CASE_CONVERSION, GLFW.GLFW_KEY_L,
           (field, selecting) -> { TextOperations.lowercaseWord(field); return Result.HANDLED; }),

    META_C("M-c", Modifier.ALT, Category.CASE_CONVERSION, GLFW.GLFW_KEY_C,
           (field, selecting) -> { TextOperations.capitalizeWord(field); return Result.HANDLED; }),

    // Mark/selection
    CTRL_SPACE("C-Space", Modifier.CTRL, Category.MARK, GLFW.GLFW_KEY_SPACE,
           (field, selecting) -> {
               field.getState().setMark();
               field.setSelectionStart(field.getCursor());
               return Result.HANDLED;
           }),

    CTRL_X_CTRL_X("C-x C-x", Modifier.CTRL, Category.MARK, GLFW.GLFW_KEY_X,
           null), // Handled specially for C-x prefix sequence

    CTRL_G("C-g", Modifier.CTRL, Category.ALWAYS, GLFW.GLFW_KEY_G,
           (field, selecting) -> {
               field.getState().deactivateMark();
               field.collapseSelection();
               return Result.HANDLED;
           }),

    // History search - handled by ChatScreenMixin
    CTRL_R("C-r", Modifier.CTRL, Category.HISTORY_SEARCH, GLFW.GLFW_KEY_R, null),
    CTRL_S("C-s", Modifier.CTRL, Category.HISTORY_SEARCH, GLFW.GLFW_KEY_S, null),
    ;

    /**
     * Result of executing a command action.
     */
    public enum Result {
        HANDLED,
        PASS_THROUGH
    }

    /**
     * Functional interface for command actions.
     */
    @FunctionalInterface
    public interface Action {
        Result execute(TextFieldAdapter field, boolean selecting);
    }

    private final String name;
    private final Modifier modifier;
    private final Category category;
    private final int keyCode;
    private final Action action;

    // Lookup maps for fast access by key code
    private static final Map<Integer, Command> CTRL_COMMANDS = new HashMap<>();
    private static final Map<Integer, Command> ALT_COMMANDS = new HashMap<>();

    static {
        for (Command cmd : values()) {
            Map<Integer, Command> map = (cmd.modifier == Modifier.CTRL) ? CTRL_COMMANDS : ALT_COMMANDS;
            if (cmd != CTRL_X_CTRL_X) {
                map.putIfAbsent(cmd.keyCode, cmd);
            }
        }
    }

    Command(String name, Modifier modifier, Category category, int keyCode, Action action) {
        this.name = name;
        this.modifier = modifier;
        this.category = category;
        this.keyCode = keyCode;
        this.action = action;
    }

    public String getName() {
        return name;
    }

    public Modifier getModifier() {
        return modifier;
    }

    public Category getCategory() {
        return category;
    }

    public int getKeyCode() {
        return keyCode;
    }

    /**
     * Check if this command is enabled based on current configuration.
     */
    public boolean isEnabled() {
        return ConfigHelper.isCommandEnabled(name, modifier.isEnabled(), category.isEnabled());
    }

    /**
     * Check if this command has an action that can be executed.
     */
    public boolean hasAction() {
        return action != null;
    }

    /**
     * Execute this command's action.
     * @return the result of execution, or null if no action is defined
     */
    public Result execute(TextFieldAdapter field, boolean selecting) {
        if (action == null) {
            return null;
        }
        return action.execute(field, selecting);
    }

    /**
     * Find the Command for a Ctrl+key combination.
     */
    public static Command fromCtrlKey(int keyCode) {
        return CTRL_COMMANDS.get(keyCode);
    }

    /**
     * Find the Command for an Alt+key combination.
     */
    public static Command fromAltKey(int keyCode) {
        return ALT_COMMANDS.get(keyCode);
    }

    /**
     * Check if an Alt+key combination is bound to any command.
     */
    public static boolean isAltKeyBound(int keyCode) {
        Command cmd = ALT_COMMANDS.get(keyCode);
        return cmd != null && cmd.isEnabled();
    }

    /**
     * Modifier type for commands.
     */
    public enum Modifier {
        CTRL {
            @Override
            public boolean isEnabled() {
                return ConfigHelper.isCtrlEnabled();
            }
        },
        ALT {
            @Override
            public boolean isEnabled() {
                return ConfigHelper.isAltEnabled();
            }
        };

        public abstract boolean isEnabled();
    }

    /**
     * Category groupings for commands.
     */
    public enum Category {
        NAVIGATION {
            @Override
            public boolean isEnabled() {
                return ConfigHelper.get().keybinds.navigationEnabled;
            }
        },
        KILL_RING {
            @Override
            public boolean isEnabled() {
                return ConfigHelper.get().keybinds.killRingEnabled;
            }
        },
        UNDO {
            @Override
            public boolean isEnabled() {
                return ConfigHelper.get().keybinds.undoEnabled;
            }
        },
        TRANSPOSE {
            @Override
            public boolean isEnabled() {
                return ConfigHelper.get().keybinds.transposeEnabled;
            }
        },
        CASE_CONVERSION {
            @Override
            public boolean isEnabled() {
                return ConfigHelper.get().keybinds.caseConversionEnabled;
            }
        },
        MARK {
            @Override
            public boolean isEnabled() {
                return ConfigHelper.get().keybinds.markEnabled;
            }
        },
        HISTORY_SEARCH {
            @Override
            public boolean isEnabled() {
                return ConfigHelper.get().historySearch.enabled;
            }
        },
        ALWAYS {
            @Override
            public boolean isEnabled() {
                return true;
            }
        };

        public abstract boolean isEnabled();
    }
}
