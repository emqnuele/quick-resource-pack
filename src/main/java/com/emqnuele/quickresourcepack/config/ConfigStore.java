package com.emqnuele.quickresourcepack.config;

import com.emqnuele.quickresourcepack.QuickResourcePackMod;
import com.emqnuele.quickresourcepack.core.config.QrpConfig;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class ConfigStore {
    private static final String FILE_NAME = "quickresourcepack.json";

    private static QrpConfig config;

    private ConfigStore() {
    }

    public static synchronized QrpConfig get() {
        if (config == null) {
            load();
        }
        return config;
    }

    public static synchronized void load() {
        config = read(path());
    }

    public static synchronized void save() {
        write(path(), get());
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }

    static QrpConfig read(Path path) {
        if (!Files.exists(path)) {
            QrpConfig fresh = new QrpConfig();
            write(path, fresh);
            return fresh;
        }
        String text;
        try {
            text = Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            QuickResourcePackMod.LOGGER.error("Could not read {}, using defaults", path, e);
            return new QrpConfig();
        }
        try {
            boolean legacy = !text.isBlank() && !text.contains("\"version\"");
            QrpConfig parsed = QrpConfig.fromJson(text);
            if (legacy) {
                // a downgrade cannot read the new layout, so keep the old file around
                backup(path, ".v1.bak");
                write(path, parsed);
                QuickResourcePackMod.LOGGER.info("Migrated config to version {}", QrpConfig.CURRENT_VERSION);
            }
            return parsed;
        } catch (RuntimeException e) {
            QuickResourcePackMod.LOGGER.error("Config is corrupt, backing it up and resetting: {}", e.getMessage());
            backup(path, ".bak");
            QrpConfig fresh = new QrpConfig();
            write(path, fresh);
            return fresh;
        }
    }

    static void write(Path path, QrpConfig data) {
        try {
            Files.createDirectories(path.getParent());
            Path temp = path.resolveSibling(path.getFileName() + ".tmp");
            Files.writeString(temp, data.toJson(), StandardCharsets.UTF_8);
            // a crash mid-write must never leave a truncated config behind
            try {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            QuickResourcePackMod.LOGGER.error("Could not save config to {}", path, e);
        }
    }

    private static void backup(Path path, String suffix) {
        try {
            Path backup = path.resolveSibling(path.getFileName() + suffix);
            Files.copy(path, backup, StandardCopyOption.REPLACE_EXISTING);
            QuickResourcePackMod.LOGGER.warn("Saved a copy of the old config to {}", backup);
        } catch (IOException e) {
            QuickResourcePackMod.LOGGER.error("Could not back up config: {}", e.getMessage());
        }
    }
}
