package com.emqnuele.quickresourcepack.core.config;

/** How pressing a slot changes the selected pack stack. */
public enum SlotMode {
    /** Adds the slot packs on top of whatever is enabled; pressing again removes them. */
    TOGGLE,
    /** Replaces the whole stack with the slot packs; pressing again restores the previous stack. */
    PROFILE,
    /** Steps through the slot packs one at a time, optionally passing through "off". */
    CYCLE;

    public SlotMode next() {
        SlotMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
