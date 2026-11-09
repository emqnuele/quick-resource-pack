package com.emqnuele.quickresourcepack.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.emqnuele.quickresourcepack.core.config.KeyCombo;
import com.emqnuele.quickresourcepack.core.config.QrpConfig;
import com.emqnuele.quickresourcepack.core.config.Rule;
import com.emqnuele.quickresourcepack.core.config.Slot;
import com.emqnuele.quickresourcepack.core.config.SlotMode;
import com.google.gson.JsonSyntaxException;

import java.util.List;

import org.junit.jupiter.api.Test;

class QrpConfigTest {
    @Test
    void defaultsAreEmptyAndCurrent() {
        var config = new QrpConfig();
        assertTrue(config.slots.isEmpty());
        assertEquals(QrpConfig.CURRENT_VERSION, config.version);
        assertTrue(config.primarySlot().isEmpty());
        assertTrue(config.behavior.wheel);
    }

    @Test
    void migratesLegacyConfigToOneToggleSlot() {
        var config = QrpConfig.fromJson("""
                {"selectedResourcePack":"file/Faithful.zip","showNotifications":false,"autoApplyOnStart":true}
                """);
        assertEquals(1, config.slots.size());
        Slot slot = config.slots.getFirst();
        assertEquals(List.of("file/Faithful.zip"), slot.packs);
        assertEquals(SlotMode.TOGGLE, slot.mode);
        assertTrue(slot.applyOnStartup);
        assertEquals(slot.id, config.primarySlot().orElseThrow().id);
        assertEquals(QrpConfig.NotificationStyle.NONE, config.appearance.notifications);
        assertTrue(config.welcomed);
    }

    @Test
    void legacyWithoutPackMigratesToEmpty() {
        var config = QrpConfig.fromJson("{\"selectedResourcePack\":\"\",\"showNotifications\":true}");
        assertTrue(config.slots.isEmpty());
        assertFalse(config.welcomed);
        assertEquals(QrpConfig.NotificationStyle.TOAST, config.appearance.notifications);
    }

    @Test
    void roundTripKeepsEverything() {
        var config = new QrpConfig();
        Slot slot = Slot.of("PvP", SlotMode.CYCLE, "a", "b");
        slot.key = new KeyCombo("key.keyboard.g", KeyCombo.MOD_SHIFT);
        config.slots.add(slot);
        config.rules.add(Rule.of(Rule.Trigger.SERVER, "*.hypixel.net", slot.id));
        config.appearance.hud = true;

        var parsed = QrpConfig.fromJson(config.toJson());
        assertEquals("PvP", parsed.slots.getFirst().name);
        assertEquals(SlotMode.CYCLE, parsed.slots.getFirst().mode);
        assertEquals(slot.key, parsed.slots.getFirst().key);
        assertEquals("*.hypixel.net", parsed.rules.getFirst().pattern);
        assertTrue(parsed.appearance.hud);
    }

    @Test
    void normalizeRepairsHandEdits() {
        var config = QrpConfig.fromJson("""
                {"version":2,"slots":[
                  {"id":"x","name":null,"packs":["a",null,"a",""],"mode":null,"key":{"key":"","modifiers":99}},
                  {"id":"x","packs":["b"]},
                  null
                ],"rules":null,"appearance":null,"behavior":{"wheelHoldMs":5}}
                """);
        assertEquals(2, config.slots.size());
        Slot first = config.slots.get(0);
        assertEquals("", first.name);
        assertEquals(List.of("a"), first.packs);
        assertEquals(SlotMode.TOGGLE, first.mode);
        assertNull(first.key);
        assertNotEquals(first.id, config.slots.get(1).id);
        assertTrue(config.rules.isEmpty());
        assertEquals(80, config.behavior.wheelHoldMs);
    }

    @Test
    void removingSlotDropsItsRulesAndPrimary() {
        var config = new QrpConfig();
        Slot slot = Slot.of("x", SlotMode.TOGGLE, "a");
        config.slots.add(slot);
        config.behavior.primarySlotId = slot.id;
        config.rules.add(Rule.of(Rule.Trigger.WORLD, "w", slot.id));
        config.removeSlot(slot.id);
        assertTrue(config.slots.isEmpty());
        assertTrue(config.rules.isEmpty());
        assertEquals("", config.behavior.primarySlotId);
    }

    @Test
    void blankAndNullInputGiveDefaults() {
        assertTrue(QrpConfig.fromJson(null).slots.isEmpty());
        assertTrue(QrpConfig.fromJson("  ").slots.isEmpty());
        assertTrue(QrpConfig.fromJson("null").slots.isEmpty());
    }

    @Test
    void corruptJsonThrows() {
        assertThrows(JsonSyntaxException.class, () -> QrpConfig.fromJson("{not json"));
        assertThrows(JsonSyntaxException.class, () -> QrpConfig.fromJson("[1,2]"));
    }

    @Test
    void wheelSlotsSkipHiddenAndEmpty() {
        var config = new QrpConfig();
        Slot shown = Slot.of("a", SlotMode.TOGGLE, "a");
        Slot hidden = Slot.of("b", SlotMode.TOGGLE, "b");
        hidden.showInWheel = false;
        Slot empty = Slot.of("c", SlotMode.TOGGLE);
        config.slots.addAll(List.of(shown, hidden, empty));
        assertEquals(List.of(shown), config.wheelSlots());
    }
}
