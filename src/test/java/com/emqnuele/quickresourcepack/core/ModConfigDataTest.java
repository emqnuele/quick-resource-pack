package com.emqnuele.quickresourcepack.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonSyntaxException;

import org.junit.jupiter.api.Test;

class ModConfigDataTest {
    @Test
    void defaults() {
        var data = new ModConfigData();
        assertEquals("", data.selectedResourcePack);
        assertTrue(data.showNotifications);
        assertFalse(data.autoApplyOnStart);
    }

    @Test
    void roundTrip() {
        var data = new ModConfigData();
        data.selectedResourcePack = "file/pack.zip";
        data.showNotifications = false;
        data.autoApplyOnStart = true;
        var parsed = ModConfigData.fromJson(data.toJson());
        assertEquals("file/pack.zip", parsed.selectedResourcePack);
        assertFalse(parsed.showNotifications);
        assertTrue(parsed.autoApplyOnStart);
    }

    @Test
    void blankInputYieldsDefaults() {
        assertEquals("", ModConfigData.fromJson(null).selectedResourcePack);
        assertEquals("", ModConfigData.fromJson("  ").selectedResourcePack);
        assertEquals("", ModConfigData.fromJson("null").selectedResourcePack);
    }

    @Test
    void explicitNullsAreNormalized() {
        var parsed = ModConfigData.fromJson("{\"selectedResourcePack\":null}");
        assertEquals("", parsed.selectedResourcePack);
        assertTrue(parsed.showNotifications);
    }

    @Test
    void corruptJsonThrowsSoCallersCanBackUp() {
        assertThrows(JsonSyntaxException.class, () -> ModConfigData.fromJson("{not json"));
    }
}
