package com.emqnuele.quickresourcepack.core.config;

import java.util.Objects;

/**
 * A key or mouse button plus the modifiers that must be held with it.
 *
 * <p>The key is stored by its vanilla name ({@code key.keyboard.r},
 * {@code key.mouse.4}), the same format as {@code options.txt}: raw codes
 * changed meaning when the game moved from GLFW to SDL, names did not.
 */
public final class KeyCombo {
    public static final int MOD_SHIFT = 1;
    public static final int MOD_CONTROL = 2;
    public static final int MOD_ALT = 4;
    private static final int MOD_MASK = MOD_SHIFT | MOD_CONTROL | MOD_ALT;

    public String key = "";
    public int modifiers = 0;

    public KeyCombo() {
    }

    public KeyCombo(String key, int modifiers) {
        this.key = key == null ? "" : key;
        this.modifiers = modifiers & MOD_MASK;
    }

    public boolean isBound() {
        return key != null && !key.isBlank() && !key.equals("key.keyboard.unknown");
    }

    public boolean isMouse() {
        return key != null && key.startsWith("key.mouse.");
    }

    public boolean has(int modifier) {
        return (modifiers & modifier) != 0;
    }

    /**
     * Exact match: extra modifiers reject the press, so a plain R binding never
     * fires on Ctrl+R and the two can coexist.
     */
    public boolean matches(String pressedKey, int heldModifiers) {
        if (!isBound() || !key.equals(pressedKey)) {
            return false;
        }
        // a modifier bound on its own reports itself as held, so its bits cannot be compared
        return isModifierKey(key) || (heldModifiers & MOD_MASK) == modifiers;
    }

    /** Release ignores modifiers: letting go of shift first must still end a hold. */
    public boolean matchesRelease(String releasedKey) {
        return isBound() && key.equals(releasedKey);
    }

    /** Pressing a modifier on its own is how a combo starts, never a binding by itself. */
    public static boolean isModifierKey(String keyName) {
        return switch (keyName) {
            case "key.keyboard.left.shift", "key.keyboard.right.shift",
                 "key.keyboard.left.control", "key.keyboard.right.control",
                 "key.keyboard.left.alt", "key.keyboard.right.alt",
                 "key.keyboard.left.win", "key.keyboard.right.win" -> true;
            default -> false;
        };
    }

    public KeyCombo copy() {
        return new KeyCombo(key, modifiers);
    }

    void normalize() {
        if (key == null) {
            key = "";
        }
        modifiers &= MOD_MASK;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof KeyCombo other)) {
            return false;
        }
        return Objects.equals(key, other.key) && modifiers == other.modifiers;
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, modifiers);
    }

    @Override
    public String toString() {
        return key + "+" + modifiers;
    }
}
