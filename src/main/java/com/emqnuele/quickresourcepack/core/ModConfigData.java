package com.emqnuele.quickresourcepack.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

/**
 * Plain config data: serialization only, no files, no Fabric types.
 * File handling (paths, backups, logging) lives in {@code ModConfig}.
 */
public class ModConfigData {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public String selectedResourcePack = "";
    public boolean showNotifications = true;
    public boolean autoApplyOnStart = false;

    /** Repairs explicit nulls / missing state after deserialization. */
    public void normalize() {
        if (selectedResourcePack == null) {
            selectedResourcePack = "";
        }
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    /**
     * Parses stored JSON. Empty or blank input yields defaults.
     *
     * @throws JsonSyntaxException on corrupt JSON so callers can back the file up
     */
    public static ModConfigData fromJson(String json) throws JsonSyntaxException {
        if (json == null || json.isBlank()) {
            return new ModConfigData();
        }
        ModConfigData data = GSON.fromJson(json, ModConfigData.class);
        if (data == null) {
            return new ModConfigData();
        }
        data.normalize();
        return data;
    }
}
