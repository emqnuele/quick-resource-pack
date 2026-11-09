package com.emqnuele.quickresourcepack.core.logic;

import java.util.List;
import java.util.Optional;

/**
 * Serialises resource reloads. While one runs, later requests only replace a
 * single pending target, so mashing a key costs at most one extra reload.
 * Not thread-safe: the game layer drives it from the client thread only.
 */
public final class ReloadScheduler {
    private boolean inFlight;
    private List<String> pending;

    /** The stack new actions should build on: the queued target if any, else the live one. */
    public List<String> base(List<String> live) {
        return pending != null ? pending : live;
    }

    /**
     * Requests a stack.
     *
     * @return true when the caller must start a reload now, false when it was queued
     */
    public boolean submit(List<String> target) {
        if (inFlight) {
            pending = List.copyOf(target);
            return false;
        }
        inFlight = true;
        return true;
    }

    /**
     * Marks the running reload finished.
     *
     * @return the queued stack to apply next, already marked in flight
     */
    public Optional<List<String>> complete() {
        if (pending == null) {
            inFlight = false;
            return Optional.empty();
        }
        List<String> next = pending;
        pending = null;
        inFlight = true;
        return Optional.of(next);
    }

    public boolean isBusy() {
        return inFlight;
    }

    public boolean hasPending() {
        return pending != null;
    }

    /** Drops everything, e.g. after a failed reload rolled the packs back. */
    public void reset() {
        inFlight = false;
        pending = null;
    }
}
