package com.emqnuele.quickresourcepack.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Regression cover for issue #3: typing in chat must never trigger the toggle. */
class KeybindGuardTest {
    @Test
    void toggleOnlyInGameWithNoScreen() {
        assertTrue(KeybindGuard.shouldHandleToggle(false, true));
        assertFalse(KeybindGuard.shouldHandleToggle(true, true));
        assertFalse(KeybindGuard.shouldHandleToggle(false, false));
        assertFalse(KeybindGuard.shouldHandleToggle(true, false));
    }

    @Test
    void menuStaysShutWhileTypingButClosesItself() {
        assertTrue(KeybindGuard.shouldHandleMenu(false, false));
        assertFalse(KeybindGuard.shouldHandleMenu(true, false));
        assertTrue(KeybindGuard.shouldHandleMenu(true, true));
    }
}
