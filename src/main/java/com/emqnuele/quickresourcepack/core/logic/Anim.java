package com.emqnuele.quickresourcepack.core.logic;

/** Easing curves and frame-rate independent smoothing. */
public final class Anim {
    private Anim() {
    }

    public static float clamp01(float t) {
        return t < 0 ? 0 : (t > 1 ? 1 : t);
    }

    public static float easeOutCubic(float t) {
        float u = 1 - clamp01(t);
        return 1 - u * u * u;
    }

    public static float easeInOutCubic(float t) {
        float x = clamp01(t);
        return x < 0.5f ? 4 * x * x * x : 1 - (float) Math.pow(-2 * x + 2, 3) / 2;
    }

    /** Overshoots slightly before settling: used for pop-in. */
    public static float easeOutBack(float t) {
        float x = clamp01(t);
        float c1 = 1.70158f;
        float c3 = c1 + 1;
        return 1 + c3 * (float) Math.pow(x - 1, 3) + c1 * (float) Math.pow(x - 1, 2);
    }

    /**
     * Exponential approach of {@code current} to {@code target}; {@code speed} is
     * roughly "fraction per second", so the motion feels the same at 30 or 240 fps.
     */
    public static float approach(float current, float target, float speed, float deltaSeconds) {
        float factor = 1 - (float) Math.exp(-speed * Math.max(0, deltaSeconds));
        float next = current + (target - current) * factor;
        return Math.abs(target - next) < 0.001f ? target : next;
    }

    public static float lerp(float t, float a, float b) {
        return a + (b - a) * t;
    }
}
