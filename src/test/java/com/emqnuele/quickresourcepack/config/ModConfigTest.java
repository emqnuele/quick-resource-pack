package com.emqnuele.quickresourcepack.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.emqnuele.quickresourcepack.core.config.QrpConfig;
import com.emqnuele.quickresourcepack.core.config.Slot;
import com.emqnuele.quickresourcepack.core.config.SlotMode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModConfigTest {
    @Test
    void missingFileCreatesDefaults(@TempDir Path dir) {
        Path path = dir.resolve("quickresourcepack.json");
        QrpConfig data = ConfigStore.read(path);
        assertTrue(data.slots.isEmpty());
        assertTrue(Files.exists(path));
    }

    @Test
    void corruptFileIsBackedUpAndReset(@TempDir Path dir) throws Exception {
        Path path = dir.resolve("quickresourcepack.json");
        Files.writeString(path, "{not json");
        QrpConfig data = ConfigStore.read(path);
        assertTrue(data.slots.isEmpty());
        assertTrue(Files.exists(dir.resolve("quickresourcepack.json.bak")));
        assertTrue(ConfigStore.read(path).slots.isEmpty());
    }

    @Test
    void legacyFileIsMigratedInPlaceWithBackup(@TempDir Path dir) throws Exception {
        Path path = dir.resolve("quickresourcepack.json");
        Files.writeString(path, "{\"selectedResourcePack\":\"file/x.zip\",\"showNotifications\":true}");
        QrpConfig data = ConfigStore.read(path);
        assertEquals(List.of("file/x.zip"), data.slots.getFirst().packs);
        assertTrue(Files.exists(dir.resolve("quickresourcepack.json.v1.bak")));
        assertTrue(Files.readString(path).contains("\"version\""));
    }

    @Test
    void roundTripPreservesValuesAndLeavesNoTempFile(@TempDir Path dir) {
        Path path = dir.resolve("quickresourcepack.json");
        QrpConfig data = new QrpConfig();
        data.slots.add(Slot.of("A", SlotMode.PROFILE, "file/pack.zip"));
        ConfigStore.write(path, data);
        QrpConfig reread = ConfigStore.read(path);
        assertEquals(SlotMode.PROFILE, reread.slots.getFirst().mode);
        assertFalse(Files.exists(dir.resolve("quickresourcepack.json.tmp")));
    }
}
