package com.emqnuele.quickresourcepack.config;

import com.emqnuele.quickresourcepack.QuickResourcePackMod;
import com.emqnuele.quickresourcepack.core.ModConfigData;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class ModConfig {
    private ModConfig() {
    }

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("quickresourcepack.json");
    }

    private static ModConfigData instance;

    public static synchronized ModConfigData getInstance() {
        if (instance == null) {
            load();
        }
        return instance;
    }

    public static synchronized void load() {
        instance = read(configPath());
    }

    public static synchronized void save() {
        if (instance == null) {
            instance = new ModConfigData();
        }
        write(configPath(), instance);
    }

    /** Reads config from {@code path}; corrupt JSON is backed up and replaced with defaults. */
    static ModConfigData read(Path path) {
        if (!Files.exists(path)) {
            ModConfigData fresh = new ModConfigData();
            write(path, fresh);
            return fresh;
        }
        try {
            return ModConfigData.fromJson(Files.readString(path));
        } catch (IOException | RuntimeException e) {
            QuickResourcePackMod.LOGGER.error("Failed to load config, resetting to defaults: {}", e.getMessage());
            try {
                Path backup = path.resolveSibling(path.getFileName() + ".bak");
                Files.copy(path, backup, StandardCopyOption.REPLACE_EXISTING);
                QuickResourcePackMod.LOGGER.warn("Backed up corrupt config to {}", backup);
            } catch (IOException backupError) {
                QuickResourcePackMod.LOGGER.error("Failed to back up corrupt config: {}", backupError.getMessage());
            }
            ModConfigData fresh = new ModConfigData();
            write(path, fresh);
            return fresh;
        }
    }

    static void write(Path path, ModConfigData data) {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, data.toJson());
        } catch (IOException e) {
            QuickResourcePackMod.LOGGER.error("Failed to save config: {}", e.getMessage());
        }
    }
}
