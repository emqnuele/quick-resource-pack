package com.emqnuele.quickresourcepack.core.logic;

import java.util.Set;

/**
 * What the planner needs to know about installed packs.
 *
 * @param available every installed pack id
 * @param pinned    ids the player cannot move or remove (required, fixed-position, server)
 * @param pinnedTop subset of {@code pinned} that sits above player packs (server packs)
 */
public record PackUniverse(Set<String> available, Set<String> pinned, Set<String> pinnedTop) {
    public PackUniverse {
        available = Set.copyOf(available);
        pinned = Set.copyOf(pinned);
        pinnedTop = Set.copyOf(pinnedTop);
    }

    public static PackUniverse of(Set<String> available) {
        return new PackUniverse(available, Set.of(), Set.of());
    }

    public boolean isMovable(String id) {
        return available.contains(id) && !pinned.contains(id);
    }
}
