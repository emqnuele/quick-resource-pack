package com.emqnuele.quickresourcepack.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Pure toggle maths: no Minecraft types, so plain JUnit can cover it. */
public final class PackToggleLogic {
    private PackToggleLogic() {
    }

    public record ToggleResult(List<String> selectedIds, boolean nowEnabled) {
    }

    public static boolean isValidPackId(String packId) {
        return packId != null && !packId.isEmpty();
    }

    /**
     * Returns the new selection after toggling {@code packId}.
     * Never mutates {@code current}; the returned list is a fresh copy.
     *
     * @throws IllegalArgumentException when {@code packId} is null or empty
     */
    public static ToggleResult toggle(List<String> current, String packId) {
        if (!isValidPackId(packId)) {
            throw new IllegalArgumentException("packId must not be null or empty");
        }
        List<String> base = current == null ? List.of() : current;
        List<String> next = new ArrayList<>(base);
        boolean wasEnabled = next.contains(packId);
        if (wasEnabled) {
            next.removeIf(id -> Objects.equals(id, packId));
        } else if (next.stream().noneMatch(id -> Objects.equals(id, packId))) {
            next.add(packId);
        }
        return new ToggleResult(next, !wasEnabled);
    }

    /** Adds {@code packId} when missing; returns a fresh list, never mutates the input. */
    public static List<String> ensureAdded(List<String> current, String packId) {
        if (!isValidPackId(packId)) {
            throw new IllegalArgumentException("packId must not be null or empty");
        }
        List<String> base = current == null ? List.of() : current;
        if (base.contains(packId)) {
            return new ArrayList<>(base);
        }
        List<String> next = new ArrayList<>(base);
        next.add(packId);
        return next;
    }
}
