package com.emqnuele.quickresourcepack.core.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * The whole mod configuration. Pure data plus (de)serialisation and migration;
 * file handling lives in the game layer.
 */
public final class QrpConfig {
    public static final int CURRENT_VERSION = 2;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public enum NotificationStyle {
        TOAST,
        ACTION_BAR,
        CHAT,
        NONE;

        public NotificationStyle next() {
            NotificationStyle[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }

    public enum HudCorner {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT;

        public HudCorner next() {
            HudCorner[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }

    public static final class Appearance {
        public NotificationStyle notifications = NotificationStyle.TOAST;
        public boolean hud = false;
        public HudCorner hudCorner = HudCorner.TOP_LEFT;
        public boolean hudOnlyActive = true;
        public boolean animations = true;

        void normalize() {
            // unknown names (the 2.0 betas' island style) parse as null
            if (notifications == null) {
                notifications = NotificationStyle.TOAST;
            }
            if (hudCorner == null) {
                hudCorner = HudCorner.TOP_LEFT;
            }
        }
    }

    public static final class Behavior {
        /** Slot fired by a short tap of the main key; empty means the first slot. */
        public String primarySlotId = "";
        public boolean wheel = true;
        public int wheelHoldMs = 200;
        /** Keeps playing during a reload: the full-screen loading overlay becomes a small HUD bar. */
        public boolean seamlessReload = true;
        public boolean rules = true;

        void normalize() {
            if (primarySlotId == null) {
                primarySlotId = "";
            }
            wheelHoldMs = Math.clamp(wheelHoldMs, 80, 1000);
        }
    }

    public int version = CURRENT_VERSION;
    public List<Slot> slots = new ArrayList<>();
    public List<Rule> rules = new ArrayList<>();
    public Appearance appearance = new Appearance();
    public Behavior behavior = new Behavior();
    public boolean welcomed = false;
    /** Slots switched on by rules, kept across restarts so leaving a server reverts lazily. */
    public List<String> ruleActivated = new ArrayList<>();

    public void normalize() {
        version = CURRENT_VERSION;
        if (slots == null) {
            slots = new ArrayList<>();
        }
        if (rules == null) {
            rules = new ArrayList<>();
        }
        if (ruleActivated == null) {
            ruleActivated = new ArrayList<>();
        }
        if (appearance == null) {
            appearance = new Appearance();
        }
        if (behavior == null) {
            behavior = new Behavior();
        }
        slots.removeIf(Objects::isNull);
        rules.removeIf(Objects::isNull);
        Set<String> seen = new HashSet<>();
        for (Slot slot : slots) {
            slot.normalize();
            // hand-copied slots can share an id; rules must point at exactly one
            while (!seen.add(slot.id)) {
                slot.id = Slot.newId();
            }
        }
        rules.forEach(Rule::normalize);
        ruleActivated.removeIf(id -> id == null || slot(id).isEmpty());
        appearance.normalize();
        behavior.normalize();
    }

    public Optional<Slot> slot(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return slots.stream().filter(s -> id.equals(s.id)).findFirst();
    }

    /** The slot a tap of the main key acts on: the chosen one, else the first slot. */
    public Optional<Slot> primarySlot() {
        Optional<Slot> chosen = slot(behavior.primarySlotId);
        if (chosen.isPresent()) {
            return chosen;
        }
        return slots.isEmpty() ? Optional.empty() : Optional.of(slots.getFirst());
    }

    public List<Slot> wheelSlots() {
        return slots.stream().filter(s -> s.showInWheel && s.hasPacks()).toList();
    }

    public void removeSlot(String id) {
        slots.removeIf(s -> s.id.equals(id));
        rules.removeIf(r -> r.slotId.equals(id));
        if (behavior.primarySlotId.equals(id)) {
            behavior.primarySlotId = "";
        }
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    public QrpConfig copy() {
        return fromJson(toJson());
    }

    /**
     * Parses stored JSON, migrating older layouts. Blank input yields defaults.
     *
     * @throws JsonSyntaxException on corrupt JSON so callers can back the file up
     */
    public static QrpConfig fromJson(String json) throws JsonSyntaxException {
        if (json == null || json.isBlank()) {
            return new QrpConfig();
        }
        JsonElement root;
        try {
            root = JsonParser.parseString(json);
        } catch (RuntimeException e) {
            throw new JsonSyntaxException(e);
        }
        if (root == null || root.isJsonNull()) {
            return new QrpConfig();
        }
        if (!root.isJsonObject()) {
            throw new JsonSyntaxException("config root must be an object");
        }
        JsonObject object = root.getAsJsonObject();
        QrpConfig config = isLegacy(object) ? migrateV1(object) : GSON.fromJson(object, QrpConfig.class);
        if (config == null) {
            config = new QrpConfig();
        }
        config.normalize();
        return config;
    }

    private static boolean isLegacy(JsonObject object) {
        return !object.has("version") && !object.has("slots");
    }

    /**
     * 1.x stored one pack plus two switches. It becomes one toggle slot on the
     * main key, so an upgrade keeps R doing exactly what it did before.
     */
    static QrpConfig migrateV1(JsonObject legacy) {
        QrpConfig config = new QrpConfig();
        String pack = stringOr(legacy, "selectedResourcePack", "");
        boolean notifications = boolOr(legacy, "showNotifications", true);
        boolean autoApply = boolOr(legacy, "autoApplyOnStart", false);

        if (!pack.isBlank()) {
            Slot slot = Slot.of("", SlotMode.TOGGLE, pack);
            slot.applyOnStartup = autoApply;
            config.slots.add(slot);
            config.behavior.primarySlotId = slot.id;
            // existing users already know the mod; skip the first-run hint
            config.welcomed = true;
        }
        config.appearance.notifications = notifications ? NotificationStyle.TOAST : NotificationStyle.NONE;
        return config;
    }

    private static String stringOr(JsonObject object, String key, String fallback) {
        JsonElement value = object.get(key);
        if (value == null || value.isJsonNull() || !value.isJsonPrimitive()) {
            return fallback;
        }
        return value.getAsString();
    }

    private static boolean boolOr(JsonObject object, String key, boolean fallback) {
        JsonElement value = object.get(key);
        if (value == null || value.isJsonNull() || !value.isJsonPrimitive()) {
            return fallback;
        }
        return value.getAsBoolean();
    }
}
