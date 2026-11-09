package com.emqnuele.quickresourcepack.core.config;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * A named quick action over one or more packs. Packs are ordered highest
 * priority first, like the "Selected" column of the vanilla pack screen.
 */
public final class Slot {
    public enum Insert {
        TOP,
        BOTTOM
    }

    public String id = newId();
    public String name = "";
    public List<String> packs = new ArrayList<>();
    public SlotMode mode = SlotMode.TOGGLE;
    public KeyCombo key = null;
    public Insert insert = Insert.TOP;
    public boolean cycleIncludesOff = true;
    public boolean showInWheel = true;
    public boolean applyOnStartup = false;

    /** Stack saved when a profile slot took over, restored when it is pressed again. */
    public List<String> restoreSnapshot = null;

    public static String newId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    public static Slot of(String name, SlotMode mode, String... packIds) {
        Slot slot = new Slot();
        slot.name = name;
        slot.mode = mode;
        slot.packs = new ArrayList<>(List.of(packIds));
        return slot;
    }

    public boolean hasPacks() {
        return !packs.isEmpty();
    }

    public boolean isBound() {
        return key != null && key.isBound();
    }

    /** Repairs nulls and duplicates left by hand edits or older versions. */
    void normalize() {
        if (id == null || id.isBlank()) {
            id = newId();
        }
        if (name == null) {
            name = "";
        }
        if (mode == null) {
            mode = SlotMode.TOGGLE;
        }
        if (insert == null) {
            insert = Insert.TOP;
        }
        if (packs == null) {
            packs = new ArrayList<>();
        }
        packs = new ArrayList<>(new LinkedHashSet<>(packs.stream()
                .filter(Objects::nonNull)
                .filter(p -> !p.isBlank())
                .toList()));
        if (key != null) {
            key.normalize();
            if (!key.isBound()) {
                key = null;
            }
        }
        if (restoreSnapshot != null) {
            restoreSnapshot = new ArrayList<>(restoreSnapshot.stream().filter(Objects::nonNull).toList());
        }
    }

    public Slot copy() {
        Slot copy = new Slot();
        copy.id = id;
        copy.name = name;
        copy.packs = new ArrayList<>(packs);
        copy.mode = mode;
        copy.key = key == null ? null : key.copy();
        copy.insert = insert;
        copy.cycleIncludesOff = cycleIncludesOff;
        copy.showInWheel = showInWheel;
        copy.applyOnStartup = applyOnStartup;
        copy.restoreSnapshot = restoreSnapshot == null ? null : new ArrayList<>(restoreSnapshot);
        return copy;
    }
}
