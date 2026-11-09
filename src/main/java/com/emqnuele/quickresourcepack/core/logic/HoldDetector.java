package com.emqnuele.quickresourcepack.core.logic;

/**
 * Tells a tap from a hold on one key: a release before the threshold is a
 * tap, staying down past it is a hold. Time is passed in so tests need no clock.
 */
public final class HoldDetector {
    public enum Event {
        NONE,
        TAP,
        HOLD_STARTED,
        HOLD_RELEASED
    }

    private long pressedAt = -1;
    private boolean holding;

    public void press(long now) {
        if (pressedAt < 0) {
            pressedAt = now;
            holding = false;
        }
    }

    /** Call every frame or tick while waiting. */
    public Event poll(long now, long thresholdMs) {
        if (pressedAt >= 0 && !holding && now - pressedAt >= thresholdMs) {
            holding = true;
            return Event.HOLD_STARTED;
        }
        return Event.NONE;
    }

    public Event release(long now, long thresholdMs) {
        if (pressedAt < 0) {
            return Event.NONE;
        }
        boolean wasHolding = holding || now - pressedAt >= thresholdMs;
        pressedAt = -1;
        holding = false;
        return wasHolding ? Event.HOLD_RELEASED : Event.TAP;
    }

    public void cancel() {
        pressedAt = -1;
        holding = false;
    }

    public boolean isDown() {
        return pressedAt >= 0;
    }

    public boolean isHolding() {
        return holding;
    }
}
