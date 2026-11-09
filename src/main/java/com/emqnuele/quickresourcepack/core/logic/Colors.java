package com.emqnuele.quickresourcepack.core.logic;

/** ARGB helpers kept game-free so the UI maths is testable. */
public final class Colors {
    private Colors() {
    }

    public static int alpha(int argb) {
        return argb >>> 24;
    }

    public static int withAlpha(int argb, int alpha) {
        return (argb & 0x00FFFFFF) | (Math.clamp(alpha, 0, 255) << 24);
    }

    public static int withAlpha(int argb, float alpha) {
        return withAlpha(argb, Math.round(Anim.clamp01(alpha) * 255));
    }

    /** Scales the existing alpha, e.g. to fade a translucent panel out. */
    public static int fade(int argb, float factor) {
        return withAlpha(argb, Math.round(alpha(argb) * Anim.clamp01(factor)));
    }

    public static int lerp(float t, int from, int to) {
        float x = Anim.clamp01(t);
        int a = Math.round(Anim.lerp(x, from >>> 24, to >>> 24));
        int r = Math.round(Anim.lerp(x, (from >> 16) & 0xFF, (to >> 16) & 0xFF));
        int g = Math.round(Anim.lerp(x, (from >> 8) & 0xFF, (to >> 8) & 0xFF));
        int b = Math.round(Anim.lerp(x, from & 0xFF, to & 0xFF));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /** Mixes towards white (positive) or black (negative) keeping alpha. */
    public static int shade(int argb, float amount) {
        int target = amount >= 0 ? 0xFFFFFFFF : 0xFF000000;
        return withAlpha(lerp(Math.abs(amount), argb | 0xFF000000, target), alpha(argb));
    }

    /** Readable text colour on top of {@code background}. */
    public static int contrastText(int background) {
        int r = (background >> 16) & 0xFF;
        int g = (background >> 8) & 0xFF;
        int b = background & 0xFF;
        double luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255;
        return luminance > 0.6 ? 0xFF111114 : 0xFFFFFFFF;
    }
}
