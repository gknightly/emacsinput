package net.woadwizard.compat;

/**
 * Compatibility shim for KeyEvent which doesn't exist in MC 1.21.4.
 * In 1.21.6+, keyPressed receives a KeyEvent object; in 1.21.4 it receives primitives.
 */
public record KeyEvent(int key, int scancode, int modifiers) {}
