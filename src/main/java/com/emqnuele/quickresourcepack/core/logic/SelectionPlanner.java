package com.emqnuele.quickresourcepack.core.logic;

import com.emqnuele.quickresourcepack.core.config.Slot;
import com.emqnuele.quickresourcepack.core.config.SlotMode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Pure pack-stack maths for slots. Stacks are lists of pack ids ordered bottom
 * to top, exactly like {@code PackRepository#getSelectedIds}; slot packs are
 * ordered top first, like the vanilla "Selected" column.
 *
 * <p>Pinned ids (required, fixed-position and server packs) are never removed
 * or moved: they belong to the game or the server, not to the player.
 */
public final class SelectionPlanner {
    private SelectionPlanner() {
    }

    public enum SnapshotChange {
        KEEP,
        SET,
        CLEAR
    }

    /**
     * Outcome of acting on a slot.
     *
     * @param selection new stack, bottom to top
     * @param changed   whether {@code selection} differs from the input stack
     * @param enabled   whether the slot is on afterwards (for cycle: a pack is on)
     * @param shownPack pack to show in feedback: the one cycled to, else the slot's first
     * @param missing   slot packs that are not installed and were skipped
     */
    public record Plan(List<String> selection,
                       boolean changed,
                       boolean enabled,
                       String shownPack,
                       SnapshotChange snapshotChange,
                       List<String> snapshot,
                       List<String> missing) {
        public boolean nothingInstalled(Slot slot) {
            return !slot.packs.isEmpty() && missing.size() == slot.packs.size();
        }
    }

    /**
     * @param active     slot counts as on
     * @param cycleIndex for cycle slots, index (in slot order) of the pack that is on, else -1
     * @param partial    toggle slot with only some of its packs on
     */
    public record Status(boolean active, int cycleIndex, boolean partial) {
        public static final Status OFF = new Status(false, -1, false);
    }

    public static Status status(Slot slot, List<String> current, PackUniverse universe) {
        Set<String> pinned = universe.pinned();
        List<String> packs = usablePacks(slot, universe);
        if (packs.isEmpty()) {
            return Status.OFF;
        }
        List<String> stack = safe(current);
        return switch (slot.mode) {
            case TOGGLE -> {
                long on = packs.stream().filter(stack::contains).count();
                yield new Status(on == packs.size(), -1, on > 0 && on < packs.size());
            }
            case PROFILE -> new Status(isProfileActive(packs, stack, universe), -1, false);
            case CYCLE -> {
                int index = cycleIndex(packs, stack);
                yield new Status(index >= 0, index, false);
            }
        };
    }

    /** What pressing the slot's key does. */
    public static Plan press(Slot slot, List<String> current, PackUniverse universe) {
        List<String> stack = safe(current);
        List<String> missing = missing(slot, universe.available());
        List<String> packs = usablePacks(slot, universe);
        if (packs.isEmpty()) {
            return unchanged(stack, false, firstOrEmpty(slot.packs), missing);
        }
        return switch (slot.mode) {
            case TOGGLE -> {
                boolean allOn = stack.containsAll(packs);
                yield allOn ? remove(stack, packs, missing) : add(stack, packs, universe, slot.insert, missing);
            }
            case PROFILE -> isProfileActive(packs, stack, universe)
                    ? restoreProfile(slot, stack, universe, missing)
                    : applyProfile(stack, packs, universe, missing);
            case CYCLE -> cycle(slot, stack, packs, universe, missing);
        };
    }

    /** Forces the slot on or off; used by rules and start-up, where pressing twice would be wrong. */
    public static Plan ensure(Slot slot, boolean on, List<String> current, PackUniverse universe) {
        List<String> stack = safe(current);
        List<String> missing = missing(slot, universe.available());
        List<String> packs = usablePacks(slot, universe);
        if (packs.isEmpty()) {
            return unchanged(stack, false, firstOrEmpty(slot.packs), missing);
        }
        Status status = status(slot, stack, universe);
        if (status.active() == on && !status.partial()) {
            String shown = slot.mode == SlotMode.CYCLE && status.cycleIndex() >= 0
                    ? packs.get(status.cycleIndex())
                    : packs.getFirst();
            return unchanged(stack, on, shown, missing);
        }
        return switch (slot.mode) {
            case TOGGLE -> on ? add(stack, packs, universe, slot.insert, missing) : remove(stack, packs, missing);
            case PROFILE -> on
                    ? applyProfile(stack, packs, universe, missing)
                    : restoreProfile(slot, stack, universe, missing);
            case CYCLE -> on
                    ? add(stack, List.of(packs.getFirst()), universe, slot.insert, missing)
                    : remove(stack, packs, missing);
        };
    }

    /** Installed slot packs, in slot order, that the player is allowed to move. */
    public static List<String> usablePacks(Slot slot, PackUniverse universe) {
        List<String> result = new ArrayList<>();
        for (String id : slot.packs) {
            if (universe.isMovable(id) && !result.contains(id)) {
                result.add(id);
            }
        }
        return result;
    }

    private static List<String> missing(Slot slot, Set<String> available) {
        return slot.packs.stream().filter(id -> !available.contains(id)).toList();
    }

    private static boolean isProfileActive(List<String> packs, List<String> stack, PackUniverse universe) {
        Set<String> user = new HashSet<>(userPacks(stack, universe));
        return user.equals(new HashSet<>(packs));
    }

    /** Index in slot order of the top-most enabled cycle pack, or -1. */
    private static int cycleIndex(List<String> packs, List<String> stack) {
        for (int i = stack.size() - 1; i >= 0; i--) {
            int index = packs.indexOf(stack.get(i));
            if (index >= 0) {
                return index;
            }
        }
        return -1;
    }

    private static Plan add(List<String> stack, List<String> packs, PackUniverse universe, Slot.Insert insert,
                            List<String> missing) {
        List<String> next = new ArrayList<>(stack);
        // highest priority first; each missing pack goes right below the slot pack above it
        int anchor = -1;
        for (String id : packs) {
            int present = next.indexOf(id);
            if (present >= 0) {
                anchor = present;
                continue;
            }
            int at = anchor >= 0 ? anchor : insertIndex(next, universe, insert);
            next.add(at, id);
            anchor = at;
        }
        return result(stack, next, true, packs.getFirst(), SnapshotChange.KEEP, null, missing);
    }

    private static Plan remove(List<String> stack, Collection<String> packs, List<String> missing) {
        List<String> next = new ArrayList<>(stack);
        next.removeAll(packs);
        String shown = packs.isEmpty() ? "" : packs.iterator().next();
        return result(stack, next, false, shown, SnapshotChange.KEEP, null, missing);
    }

    private static Plan applyProfile(List<String> stack, List<String> packs, PackUniverse universe,
                                     List<String> missing) {
        List<String> snapshot = userPacks(stack, universe);
        List<String> next = withUserPacks(stack, universe, reversed(packs));
        return result(stack, next, true, packs.getFirst(), SnapshotChange.SET, snapshot, missing);
    }

    private static Plan restoreProfile(Slot slot, List<String> stack, PackUniverse universe,
                                       List<String> missing) {
        List<String> restore = new ArrayList<>();
        if (slot.restoreSnapshot != null) {
            for (String id : slot.restoreSnapshot) {
                if (universe.isMovable(id) && !restore.contains(id)) {
                    restore.add(id);
                }
            }
        }
        List<String> next = withUserPacks(stack, universe, restore);
        return result(stack, next, false, firstOrEmpty(slot.packs), SnapshotChange.CLEAR, null, missing);
    }

    private static Plan cycle(Slot slot, List<String> stack, List<String> packs, PackUniverse universe,
                              List<String> missing) {
        int index = cycleIndex(packs, stack);
        int nextIndex = index + 1;
        if (nextIndex >= packs.size()) {
            nextIndex = slot.cycleIncludesOff ? -1 : 0;
        }
        if (nextIndex == index) {
            // single pack without an off step: nothing to cycle to
            return unchanged(stack, true, packs.get(index), missing);
        }

        List<String> next = new ArrayList<>(stack);
        int position = index >= 0 ? next.indexOf(packs.get(index)) : -1;
        int removedBefore = 0;
        for (int i = next.size() - 1; i >= 0; i--) {
            if (packs.contains(next.get(i))) {
                if (position >= 0 && i < position) {
                    removedBefore++;
                }
                next.remove(i);
            }
        }
        if (nextIndex < 0) {
            return result(stack, next, false, packs.get(index), SnapshotChange.KEEP, null, missing);
        }
        String target = packs.get(nextIndex);
        // replace in place so the rest of the player's order is untouched
        int at = position >= 0 ? position - removedBefore : insertIndex(next, universe, slot.insert);
        next.add(Math.clamp(at, 0, next.size()), target);
        return result(stack, next, true, target, SnapshotChange.KEEP, null, missing);
    }

    /** Where a new pack goes: just below the packs pinned on top, or just above the pinned bottom. */
    static int insertIndex(List<String> stack, PackUniverse universe, Slot.Insert insert) {
        if (insert == Slot.Insert.BOTTOM) {
            int index = 0;
            while (index < stack.size() && isPinnedBottom(stack.get(index), universe)) {
                index++;
            }
            return index;
        }
        int index = stack.size();
        while (index > 0 && universe.pinnedTop().contains(stack.get(index - 1))) {
            index--;
        }
        return index;
    }

    static List<String> userPacks(List<String> stack, PackUniverse universe) {
        return stack.stream().filter(id -> !universe.pinned().contains(id)).toList();
    }

    /** Rebuilds a stack as: pinned bottom packs, the given player packs, pinned top packs. */
    static List<String> withUserPacks(List<String> stack, PackUniverse universe, List<String> userBottomToTop) {
        List<String> next = new ArrayList<>();
        for (String id : stack) {
            if (isPinnedBottom(id, universe)) {
                next.add(id);
            }
        }
        next.addAll(userBottomToTop);
        for (String id : stack) {
            if (universe.pinnedTop().contains(id)) {
                next.add(id);
            }
        }
        return next;
    }

    private static boolean isPinnedBottom(String id, PackUniverse universe) {
        return universe.pinned().contains(id) && !universe.pinnedTop().contains(id);
    }

    private static Plan unchanged(List<String> stack, boolean enabled, String shown, List<String> missing) {
        return new Plan(List.copyOf(stack), false, enabled, shown, SnapshotChange.KEEP, null, missing);
    }

    private static Plan result(List<String> before, List<String> after, boolean enabled, String shown,
                               SnapshotChange change, List<String> snapshot, List<String> missing) {
        return new Plan(List.copyOf(after), !before.equals(after), enabled, shown, change,
                snapshot == null ? null : List.copyOf(snapshot), missing);
    }

    private static List<String> reversed(List<String> list) {
        List<String> copy = new ArrayList<>(list);
        Collections.reverse(copy);
        return copy;
    }

    private static List<String> safe(List<String> list) {
        return list == null ? List.of() : list;
    }

    private static String firstOrEmpty(List<String> list) {
        return list.isEmpty() ? "" : list.getFirst();
    }
}
