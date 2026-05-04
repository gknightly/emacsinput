package net.woadwizard.compat;

/**
 * Compatibility shim for CharacterEvent which doesn't exist in MC 1.21.4.
 * In 1.21.6+, charTyped receives a CharacterEvent object; in 1.21.4 it receives primitives.
 */
public record CharacterEvent(int codepoint, int modifiers) {}
