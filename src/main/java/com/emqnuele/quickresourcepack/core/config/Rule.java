package com.emqnuele.quickresourcepack.core.config;

/** Activates a slot automatically while a condition about where the player is holds. */
public final class Rule {
    public enum Trigger {
        /** Multiplayer server address, glob pattern (e.g. {@code *.hypixel.net}). */
        SERVER,
        /** Singleplayer world name, glob pattern. */
        WORLD,
        /** Dimension id (e.g. {@code minecraft:the_nether}); bare names get the minecraft namespace. */
        DIMENSION
    }

    public String id = Slot.newId();
    public boolean enabled = true;
    public Trigger trigger = Trigger.SERVER;
    public String pattern = "";
    public String slotId = "";

    public static Rule of(Trigger trigger, String pattern, String slotId) {
        Rule rule = new Rule();
        rule.trigger = trigger;
        rule.pattern = pattern;
        rule.slotId = slotId;
        return rule;
    }

    public boolean isComplete() {
        return trigger != null && pattern != null && !pattern.isBlank() && slotId != null && !slotId.isBlank();
    }

    void normalize() {
        if (id == null || id.isBlank()) {
            id = Slot.newId();
        }
        if (trigger == null) {
            trigger = Trigger.SERVER;
        }
        if (pattern == null) {
            pattern = "";
        }
        if (slotId == null) {
            slotId = "";
        }
    }

    public Rule copy() {
        Rule copy = new Rule();
        copy.id = id;
        copy.enabled = enabled;
        copy.trigger = trigger;
        copy.pattern = pattern;
        copy.slotId = slotId;
        return copy;
    }
}
