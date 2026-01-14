# Emacs Input

A client-side Fabric mod that adds Emacs-style keybindings to Minecraft text
input fields, including chat, signs, command blocks, and books.

It affects the following text fields:

 -  Chat and command input
 -  Signs and hanging signs
 -  Books and quills
 -  Command blocks
 -  Anvil naming
 -  Any other standard text field

## Keybindings

### Navigation

| Keybind | Action |  
|---------|--------|  
| `C-f` | Move forward one character |  
| `C-b` | Move backward one character |  
| `C-a` | Move to beginning of line |  
| `C-e` | Move to end of line |  
| `C-p` | Move to previous line (multiline fields only) |  
| `C-n` | Move to next line (multiline fields only) |  
| `M-f` | Move forward one word |  
| `M-b` | Move backward one word |

### Kill Ring (Cut/Copy/Paste)

| Keybind | Action |  
|---------|--------|  
| `C-d` | Delete character at cursor |  
| `C-k` | Kill (cut) to end of line |  
| `C-u` | Kill (cut) to beginning of line |  
| `C-w` | Kill (cut) region (selected text) |  
| `C-y` | Yank (paste) from kill ring |  
| `M-d` | Kill word forward |  
| `M-Backspace` | Kill word backward |  
| `M-w` | Copy region to kill ring |  
| `M-y` | Cycle through kill ring (after yank) |

### Undo/Redo

| Keybind | Action |
|---------|--------|
| `C-/` | Undo last change |
| `C-S-/` | Redo |

### Transpose

| Keybind | Action |
|---------|--------|
| `C-t` | Transpose characters |
| `M-t` | Transpose words |

### Case Conversion

| Keybind | Action |
|---------|--------|
| `M-u` | Uppercase word |
| `M-l` | Lowercase word |
| `M-c` | Capitalize word |

### Mark and Selection

| Keybind | Action |  
|---------|--------|  
| `C-Space` | Set mark (begin selection) |  
| `C-x C-x` | Exchange point and mark |  
| `C-g` | Cancel selection / deactivate mark |

### Chat History Search

| Keybind | Action |  
|---------|--------|  
| `C-r` | Search backward through chat history |  
| `C-s` | Search forward through chat history |

**Note:** `C-` denotes Ctrl, `M-` denotes Alt (or Option on macOS), `S-` denotes Shift.

## Configuration

Configuration is available through Mod Menu (or by editing the raw json at
config/emacsinput:
 -  Enable/disable entire mod
 -  Toggle Ctrl or Alt keybinds independently
 -  Enable/disable specific feature categories (navigation, kill ring, undo, transpose, case conversion, mark)
 -  Force-enable or force-disable individual commands
 -  History search settings (case sensitivity)
 -  Alt key behavior (block all input vs. block only when bound)

## Dependencies

**Required:**
 -  [Fabric Loader](https://fabricmc.net/)
 -  [Fabric API](https://modrinth.com/mod/fabric-api)
 -  [Cloth Config](https://modrinth.com/mod/cloth-config)

**Optional:**
 -  [Mod Menu](https://modrinth.com/mod/modmenu) (for in-game configuration access)
