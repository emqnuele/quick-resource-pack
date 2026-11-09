package com.emqnuele.quickresourcepack.pack;

import net.minecraft.client.gui.screens.Overlay;
import org.jspecify.annotations.Nullable;

/**
 * Hand-off between the switcher and the loading overlay mixin. The switcher
 * arms a flag right before it calls the reload; the overlay built inside that
 * call claims it, so only our own reloads are ever affected.
 */
public final class SeamlessReload {
    /** Implemented by the loading overlay through a mixin. */
    public interface Tracked {
        boolean qrp$isOwned();

        boolean qrp$isSeamless();

        float qrp$progress();
    }

    public enum Claim {
        NONE,
        OWNED,
        SEAMLESS
    }

    private static Claim armed = Claim.NONE;
    private static boolean claimed;
    private static @Nullable Tracked active;

    private SeamlessReload() {
    }

    static void arm(boolean seamless) {
        armed = seamless ? Claim.SEAMLESS : Claim.OWNED;
        claimed = false;
    }

    /** @return whether an overlay picked the flag up since {@link #arm} */
    static boolean disarm() {
        armed = Claim.NONE;
        return claimed;
    }

    /** Called from the overlay constructor. */
    public static Claim claim(Tracked overlay) {
        Claim claim = armed;
        if (claim != Claim.NONE) {
            armed = Claim.NONE;
            claimed = true;
            active = overlay;
        }
        return claim;
    }

    /** Called by an owned overlay once its reload finished, successfully or not. */
    public static void finished(Tracked overlay, boolean failed) {
        if (active == overlay) {
            active = null;
        }
        PackSwitcher.onReloadFinished(failed);
    }

    public static boolean isRunning() {
        return active != null;
    }

    public static boolean isSeamlessRunning() {
        return active != null && active.qrp$isSeamless();
    }

    public static float progress() {
        return active == null ? 1f : active.qrp$progress();
    }

    /** What gameplay code should see instead of the overlay: nothing, when it is ours and seamless. */
    public static @Nullable Overlay hide(@Nullable Overlay overlay) {
        if (overlay instanceof Tracked tracked && tracked.qrp$isSeamless()) {
            return null;
        }
        return overlay;
    }
}
