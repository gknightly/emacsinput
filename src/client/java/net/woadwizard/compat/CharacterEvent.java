package net.woadwizard.compat;

/**
 * Compatibility shim for CharacterEvent which doesn't exist in MC 1.20.1.
 * In 1.21.x, charTyped receives a CharacterEvent object; in 1.20.1 it receives primitives.
 */
public record CharacterEvent(int codepoint, int modifiers) {}
