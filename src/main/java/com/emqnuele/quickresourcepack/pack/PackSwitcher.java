package com.emqnuele.quickresourcepack.pack;

import com.emqnuele.quickresourcepack.QuickResourcePackMod;
import com.emqnuele.quickresourcepack.config.ConfigStore;
import com.emqnuele.quickresourcepack.core.config.QrpConfig;
import com.emqnuele.quickresourcepack.core.config.Slot;
import com.emqnuele.quickresourcepack.core.logic.PackUniverse;
import com.emqnuele.quickresourcepack.core.logic.ReloadScheduler;
import com.emqnuele.quickresourcepack.core.logic.SelectionPlanner;
import com.emqnuele.quickresourcepack.hud.Notifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Applies slot actions to the pack stack. Every change goes through one
 * scheduler, is written to options.txt like the vanilla screen does, and ends
 * in exactly one notification however fast the player presses.
 */
public final class PackSwitcher {
    public enum Cause {
        KEY,
        WHEEL,
        COMMAND,
        MENU,
        RULE,
        STARTUP
    }

    private static final long WATCHDOG_MS = 90_000;
    private static final ReloadScheduler SCHEDULER = new ReloadScheduler();

    private static @Nullable Feedback inFlight;
    private static @Nullable Feedback pending;
    private static long startedAt;

    private PackSwitcher() {
    }

    public static void press(Slot slot, Cause cause) {
        if (!slot.hasPacks()) {
            Notifier.emptySlot(slot);
            return;
        }
        PackUniverse universe = freshUniverse(slot.packs);
        List<String> base = SCHEDULER.base(PackIndex.selected());
        handle(slot, SelectionPlanner.press(slot, base, universe), cause);
    }

    /** Forces slots on and off in one reload: used by rules and start-up. */
    public static void ensure(List<Slot> on, List<Slot> off, Cause cause) {
        if (on.isEmpty() && off.isEmpty()) {
            return;
        }
        PackUniverse universe = PackIndex.universe();
        List<String> original = SCHEDULER.base(PackIndex.selected());
        List<String> stack = original;
        Feedback last = null;
        boolean configChanged = false;
        for (int pass = 0; pass < 2; pass++) {
            boolean enable = pass == 1;
            for (Slot slot : enable ? on : off) {
                SelectionPlanner.Plan plan = SelectionPlanner.ensure(slot, enable, stack, universe);
                configChanged |= applySnapshot(slot, plan);
                if (plan.changed()) {
                    stack = plan.selection();
                    last = Feedback.of(slot, plan.shownPack(), plan.enabled(), cause, plan.missing());
                }
            }
        }
        if (configChanged) {
            ConfigStore.save();
        }
        if (last != null && !stack.equals(original)) {
            submit(stack, last);
        }
    }

    private static void handle(Slot slot, SelectionPlanner.Plan plan, Cause cause) {
        if (plan.nothingInstalled(slot)) {
            Notifier.missing(slot, plan.missing());
            return;
        }
        if (applySnapshot(slot, plan)) {
            ConfigStore.save();
        }
        Feedback feedback = Feedback.of(slot, plan.shownPack(), plan.enabled(), cause, plan.missing());
        if (!plan.changed()) {
            Notifier.done(feedback);
            return;
        }
        submit(plan.selection(), feedback);
    }

    private static boolean applySnapshot(Slot slot, SelectionPlanner.Plan plan) {
        return switch (plan.snapshotChange()) {
            case KEEP -> false;
            case SET -> {
                slot.restoreSnapshot = plan.snapshot();
                yield true;
            }
            case CLEAR -> {
                slot.restoreSnapshot = null;
                yield true;
            }
        };
    }

    private static void submit(List<String> target, Feedback feedback) {
        if (SCHEDULER.submit(target)) {
            inFlight = feedback;
            start(target);
        } else {
            pending = feedback;
            Notifier.loading(feedback);
        }
    }

    private static void start(List<String> target) {
        Minecraft minecraft = Minecraft.getInstance();
        PackRepository repository = minecraft.getResourcePackRepository();
        repository.setSelected(target);
        persist(minecraft.options, repository);

        QrpConfig config = ConfigStore.get();
        boolean seamless = config.behavior.seamlessReload && minecraft.level != null;
        SeamlessReload.arm(seamless);
        CompletableFuture<Void> future;
        boolean claimed;
        try {
            future = minecraft.reloadResourcePacks();
        } finally {
            claimed = SeamlessReload.disarm();
        }
        startedAt = Util.getMillis();
        if (inFlight != null) {
            Notifier.loading(inFlight);
        }
        if (!claimed) {
            // another reload was already running: vanilla queued ours and only reports success
            future.whenComplete((ignored, error) -> minecraft.execute(() -> onReloadFinished(error != null)));
        }
    }

    /** Mirrors Options#updateResourcePacks without its reload, which we start ourselves. */
    private static void persist(Options options, PackRepository repository) {
        options.resourcePacks.clear();
        options.incompatibleResourcePacks.clear();
        for (Pack pack : repository.getSelectedPacks()) {
            if (!pack.isFixedPosition()) {
                options.resourcePacks.add(pack.getId());
                if (!pack.getCompatibility().isCompatible()) {
                    options.incompatibleResourcePacks.add(pack.getId());
                }
            }
        }
        options.save();
    }

    static void onReloadFinished(boolean failed) {
        if (!SCHEDULER.isBusy()) {
            return;
        }
        if (failed) {
            Feedback failedFeedback = inFlight;
            reset();
            Notifier.failed(failedFeedback);
            return;
        }
        Feedback done = inFlight;
        Optional<List<String>> next = SCHEDULER.complete();
        if (next.isPresent()) {
            inFlight = pending;
            pending = null;
            start(next.get());
        } else {
            inFlight = null;
            if (done != null) {
                Notifier.done(done);
            }
        }
    }

    public static void tick() {
        if (SCHEDULER.isBusy() && !SeamlessReload.isRunning() && Util.getMillis() - startedAt > WATCHDOG_MS) {
            QuickResourcePackMod.LOGGER.warn("A resource reload never reported back; unlocking pack switching");
            reset();
        }
    }

    public static boolean isBusy() {
        return SCHEDULER.isBusy();
    }

    private static void reset() {
        SCHEDULER.reset();
        inFlight = null;
        pending = null;
    }

    /** A pack dropped into the folder mid-session is found without restarting. */
    private static PackUniverse freshUniverse(List<String> wanted) {
        PackUniverse universe = PackIndex.universe();
        if (!universe.available().containsAll(wanted)) {
            PackIndex.refresh();
            universe = PackIndex.universe();
        }
        return universe;
    }
}
