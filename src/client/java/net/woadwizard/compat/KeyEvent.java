package net.woadwizard.compat;

/**
 * Compatibility shim for KeyEvent which doesn't exist in MC 1.20.1.
 * In 1.21.x, keyPressed receives a KeyEvent object; in 1.20.1 it receives primitives.
 */
public record KeyEvent(int key, int scancode, int modifiers) {}
