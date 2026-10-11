package com.emqnuele.quickresourcepack.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import com.emqnuele.quickresourcepack.core.ModConfigData;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModConfigTest {
    @Test
    void missingFileCreatesDefaults(@TempDir Path dir) throws Exception {
        Path path = dir.resolve("quickresourcepack.json");
        ModConfigData data = ModConfig.read(path);
        assertEquals("", data.selectedResourcePack);
        assertTrue(Files.exists(path));
    }

    @Test
    void corruptFileIsBackedUpAndReset(@TempDir Path dir) throws Exception {
        Path path = dir.resolve("quickresourcepack.json");
        Files.writeString(path, "{not json");
        ModConfigData data = ModConfig.read(path);
        assertEquals("", data.selectedResourcePack);
        assertTrue(Files.exists(dir.resolve("quickresourcepack.json.bak")));
        // The file itself was rewritten with valid defaults.
        ModConfigData reread = ModConfig.read(path);
        assertEquals("", reread.selectedResourcePack);
    }

    @Test
    void roundTripPreservesValues(@TempDir Path dir) {
        Path path = dir.resolve("quickresourcepack.json");
        ModConfigData data = new ModConfigData();
        data.selectedResourcePack = "file/pack.zip";
        data.autoApplyOnStart = true;
        ModConfig.write(path, data);
        ModConfigData reread = ModConfig.read(path);
        assertEquals("file/pack.zip", reread.selectedResourcePack);
        assertTrue(reread.autoApplyOnStart);
    }
}
