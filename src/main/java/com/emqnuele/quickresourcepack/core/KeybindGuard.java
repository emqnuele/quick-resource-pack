package com.emqnuele.quickresourcepack.core;

/**
 * Decides whether a key press should act. The toggle must never fire while the
 * player types in chat (or any other screen): presses are drained but ignored.
 */
public final class KeybindGuard {
    private KeybindGuard() {
    }

    /** Toggle acts only in-game with no screen open and a player present. */
    public static boolean shouldHandleToggle(boolean hasOpenScreen, boolean hasPlayer) {
        return !hasOpenScreen && hasPlayer;
    }

    /**
     * Menu acts when nothing is open, or to close the quick menu itself.
     * While chat (or any other screen) is open it stays shut so typing never opens UI.
     */
    public static boolean shouldHandleMenu(boolean hasOpenScreen, boolean isQuickMenuOpen) {
        if (isQuickMenuOpen) {
            return true;
        }
        return !hasOpenScreen;
    }
}
