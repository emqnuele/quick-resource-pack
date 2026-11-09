package com.emqnuele.quickresourcepack.pack;

import com.emqnuele.quickresourcepack.QuickResourcePackMod;
import com.emqnuele.quickresourcepack.compat.PackCompat;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.util.Util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.HashMap;
import java.util.Map;

public final class PackIcons {
    public static final Identifier UNKNOWN = Identifier.withDefaultNamespace("textures/misc/unknown_pack.png");
    // a time window instead of a per-frame hook, so icons load on every screen without freezing long lists
    private static final int LOADS_PER_WINDOW = 2;
    private static final long WINDOW_MS = 16;

    private static final Map<String, Identifier> LOADED = new HashMap<>();
    private static long windowStart;
    private static int loadsInWindow;

    private PackIcons() {
    }

    public static Identifier get(String packId) {
        if (packId == null || packId.isEmpty()) {
            return UNKNOWN;
        }
        Identifier icon = LOADED.get(packId);
        if (icon != null) {
            return icon;
        }
        long now = Util.getMillis();
        if (now - windowStart >= WINDOW_MS) {
            windowStart = now;
            loadsInWindow = 0;
        }
        if (loadsInWindow >= LOADS_PER_WINDOW) {
            return UNKNOWN;
        }
        loadsInWindow++;
        icon = load(packId);
        LOADED.put(packId, icon);
        return icon;
    }

    public static void invalidate() {
        Minecraft minecraft = Minecraft.getInstance();
        for (Identifier icon : LOADED.values()) {
            if (!icon.equals(UNKNOWN)) {
                minecraft.getTextureManager().release(icon);
            }
        }
        LOADED.clear();
    }

    private static String digest(String packId) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-1").digest(packId.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(packId.hashCode());
        }
    }

    private static Identifier load(String packId) {
        Minecraft minecraft = Minecraft.getInstance();
        Pack pack = minecraft.getResourcePackRepository().getPack(packId);
        if (pack == null) {
            return UNKNOWN;
        }
        try {
            NativeImage image = PackCompat.readRootImage(pack, "pack.png");
            if (image == null) {
                return UNKNOWN;
            }
            Identifier location = Identifier.fromNamespaceAndPath(QuickResourcePackMod.ASSET_NAMESPACE,
                    "pack_icon/" + digest(packId));
            minecraft.getTextureManager().register(location, new DynamicTexture(location::toString, image));
            return location;
        } catch (Exception e) {
            QuickResourcePackMod.LOGGER.warn("Failed to load icon from pack {}", packId, e);
            return UNKNOWN;
        }
    }
}
