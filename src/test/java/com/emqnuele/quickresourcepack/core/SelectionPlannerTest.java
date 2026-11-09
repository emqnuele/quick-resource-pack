package com.emqnuele.quickresourcepack.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.emqnuele.quickresourcepack.core.config.Slot;
import com.emqnuele.quickresourcepack.core.config.SlotMode;
import com.emqnuele.quickresourcepack.core.logic.PackUniverse;
import com.emqnuele.quickresourcepack.core.logic.SelectionPlanner;
import com.emqnuele.quickresourcepack.core.logic.SelectionPlanner.SnapshotChange;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class SelectionPlannerTest {
    private static final PackUniverse UNIVERSE = new PackUniverse(
            Set.of("vanilla", "fabric", "server/abc", "a", "b", "c", "d"),
            Set.of("vanilla", "fabric", "server/abc"),
            Set.of("server/abc"));

    private static Slot slot(SlotMode mode, String... packs) {
        return Slot.of("test", mode, packs);
    }

    @Test
    void toggleAddsOnTopBelowServerPacks() {
        var plan = SelectionPlanner.press(slot(SlotMode.TOGGLE, "a"),
                List.of("vanilla", "fabric", "b", "server/abc"), UNIVERSE);
        assertEquals(List.of("vanilla", "fabric", "b", "a", "server/abc"), plan.selection());
        assertTrue(plan.enabled());
        assertTrue(plan.changed());
    }

    @Test
    void toggleKeepsSlotOrderWithFirstPackHighest() {
        var plan = SelectionPlanner.press(slot(SlotMode.TOGGLE, "a", "b"), List.of("vanilla"), UNIVERSE);
        assertEquals(List.of("vanilla", "b", "a"), plan.selection());
    }

    @Test
    void toggleRemovesWhenAllOn() {
        var plan = SelectionPlanner.press(slot(SlotMode.TOGGLE, "a", "b"), List.of("vanilla", "b", "c", "a"), UNIVERSE);
        assertEquals(List.of("vanilla", "c"), plan.selection());
        assertFalse(plan.enabled());
    }

    @Test
    void partialToggleCompletesInsteadOfRemoving() {
        var slot = slot(SlotMode.TOGGLE, "a", "b");
        var status = SelectionPlanner.status(slot, List.of("vanilla", "a"), UNIVERSE);
        assertTrue(status.partial());
        assertFalse(status.active());
        var plan = SelectionPlanner.press(slot, List.of("vanilla", "a"), UNIVERSE);
        assertEquals(List.of("vanilla", "b", "a"), plan.selection());
        assertTrue(plan.enabled());
    }

    @Test
    void bottomInsertGoesAbovePinnedBottom() {
        var slot = slot(SlotMode.TOGGLE, "a");
        slot.insert = Slot.Insert.BOTTOM;
        var plan = SelectionPlanner.press(slot, List.of("vanilla", "fabric", "b"), UNIVERSE);
        assertEquals(List.of("vanilla", "fabric", "a", "b"), plan.selection());
    }

    @Test
    void missingPacksAreReportedAndSkipped() {
        var plan = SelectionPlanner.press(slot(SlotMode.TOGGLE, "gone", "a"), List.of("vanilla"), UNIVERSE);
        assertEquals(List.of("gone"), plan.missing());
        assertEquals(List.of("vanilla", "a"), plan.selection());
    }

    @Test
    void nothingInstalledChangesNothing() {
        var slot = slot(SlotMode.TOGGLE, "gone");
        var plan = SelectionPlanner.press(slot, List.of("vanilla", "a"), UNIVERSE);
        assertFalse(plan.changed());
        assertTrue(plan.nothingInstalled(slot));
    }

    @Test
    void pinnedPacksInASlotAreNeverTouched() {
        var plan = SelectionPlanner.press(slot(SlotMode.TOGGLE, "vanilla"), List.of("vanilla", "a"), UNIVERSE);
        assertFalse(plan.changed());
    }

    @Test
    void profileReplacesUserPacksAndSnapshotsThem() {
        var plan = SelectionPlanner.press(slot(SlotMode.PROFILE, "c", "d"),
                List.of("vanilla", "fabric", "a", "b", "server/abc"), UNIVERSE);
        assertEquals(List.of("vanilla", "fabric", "d", "c", "server/abc"), plan.selection());
        assertEquals(SnapshotChange.SET, plan.snapshotChange());
        assertEquals(List.of("a", "b"), plan.snapshot());
        assertTrue(plan.enabled());
    }

    @Test
    void profileRestoresSnapshotWhenPressedAgain() {
        var slot = slot(SlotMode.PROFILE, "c", "d");
        slot.restoreSnapshot = List.of("a", "gone", "b");
        var plan = SelectionPlanner.press(slot, List.of("vanilla", "fabric", "d", "c", "server/abc"), UNIVERSE);
        assertEquals(List.of("vanilla", "fabric", "a", "b", "server/abc"), plan.selection());
        assertEquals(SnapshotChange.CLEAR, plan.snapshotChange());
        assertFalse(plan.enabled());
        assertNull(plan.snapshot());
    }

    @Test
    void profileStatusIgnoresOrder() {
        var slot = slot(SlotMode.PROFILE, "c", "d");
        assertTrue(SelectionPlanner.status(slot, List.of("vanilla", "c", "d"), UNIVERSE).active());
        assertFalse(SelectionPlanner.status(slot, List.of("vanilla", "c", "d", "a"), UNIVERSE).active());
    }

    @Test
    void cycleStepsThroughPacksAndOff() {
        var slot = slot(SlotMode.CYCLE, "a", "b", "c");
        var stack = List.of("vanilla", "d");
        var first = SelectionPlanner.press(slot, stack, UNIVERSE);
        assertEquals(List.of("vanilla", "d", "a"), first.selection());
        assertEquals("a", first.shownPack());
        var second = SelectionPlanner.press(slot, first.selection(), UNIVERSE);
        assertEquals(List.of("vanilla", "d", "b"), second.selection());
        var third = SelectionPlanner.press(slot, second.selection(), UNIVERSE);
        assertEquals(List.of("vanilla", "d", "c"), third.selection());
        var off = SelectionPlanner.press(slot, third.selection(), UNIVERSE);
        assertEquals(List.of("vanilla", "d"), off.selection());
        assertFalse(off.enabled());
    }

    @Test
    void cycleReplacesInPlace() {
        var slot = slot(SlotMode.CYCLE, "a", "b");
        var plan = SelectionPlanner.press(slot, List.of("vanilla", "c", "a", "d"), UNIVERSE);
        assertEquals(List.of("vanilla", "c", "b", "d"), plan.selection());
    }

    @Test
    void cycleWithoutOffWraps() {
        var slot = slot(SlotMode.CYCLE, "a", "b");
        slot.cycleIncludesOff = false;
        var plan = SelectionPlanner.press(slot, List.of("vanilla", "b"), UNIVERSE);
        assertEquals(List.of("vanilla", "a"), plan.selection());
    }

    @Test
    void singlePackCycleWithoutOffIsStable() {
        var slot = slot(SlotMode.CYCLE, "a");
        slot.cycleIncludesOff = false;
        var plan = SelectionPlanner.press(slot, List.of("vanilla", "a"), UNIVERSE);
        assertFalse(plan.changed());
    }

    @Test
    void ensureIsIdempotent() {
        var slot = slot(SlotMode.TOGGLE, "a");
        var on = SelectionPlanner.ensure(slot, true, List.of("vanilla"), UNIVERSE);
        assertEquals(List.of("vanilla", "a"), on.selection());
        var again = SelectionPlanner.ensure(slot, true, on.selection(), UNIVERSE);
        assertFalse(again.changed());
        var off = SelectionPlanner.ensure(slot, false, again.selection(), UNIVERSE);
        assertEquals(List.of("vanilla"), off.selection());
    }

    @Test
    void ensureCycleOnPicksFirstAndOffClearsAll() {
        var slot = slot(SlotMode.CYCLE, "a", "b");
        assertEquals(List.of("vanilla", "a"), SelectionPlanner.ensure(slot, true, List.of("vanilla"), UNIVERSE).selection());
        assertEquals(List.of("vanilla"), SelectionPlanner.ensure(slot, false, List.of("vanilla", "b"), UNIVERSE).selection());
    }

    @Test
    void nullStackIsEmpty() {
        var plan = SelectionPlanner.press(slot(SlotMode.TOGGLE, "a"), null, UNIVERSE);
        assertEquals(List.of("a"), plan.selection());
    }
}
