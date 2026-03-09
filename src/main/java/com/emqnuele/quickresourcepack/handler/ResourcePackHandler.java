package com.emqnuele.quickresourcepack.handler;

import com.emqnuele.quickresourcepack.QuickResourcePackMod;
import com.emqnuele.quickresourcepack.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class ResourcePackHandler {
    private static volatile boolean isLoading = false;

    public static String getPackDisplayName(String packId) {
        if (packId == null || packId.isEmpty()) return "";
        for (ResourcePackProfile profile : MinecraftClient.getInstance().getResourcePackManager().getProfiles()) {
            if (profile.getId().equals(packId)) {
                return profile.getDisplayName().getString();
            }
        }
        return packId;
    }

    public static void togglePack() {
        if (isLoading) return;

        String packId = ModConfig.getInstance().selectedResourcePack;
        if (packId == null || packId.isEmpty()) {
            NotificationHandler.notify(Text.translatable("notification.quickresourcepack.nopack"));
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        ResourcePackManager manager = client.getResourcePackManager();
        manager.scanPacks();

        if (!manager.getIds().contains(packId)) {
            String displayName = getPackDisplayName(packId);
            NotificationHandler.notify(Text.translatable("notification.quickresourcepack.notfound", displayName));
            return;
        }

        boolean enabled = manager.getEnabledIds().contains(packId);
        List<String> profiles = new ArrayList<>(manager.getEnabledIds());

        if (enabled) {
            profiles.remove(packId);
        } else {
            profiles.add(packId);
        }

        manager.setEnabledProfiles(profiles);
        isLoading = true;

        client.reloadResources().thenRun(() -> client.execute(() -> {
            isLoading = false;
            boolean nowEnabled = client.getResourcePackManager().getEnabledIds().contains(packId);
            String displayName = getPackDisplayName(packId);
            if (nowEnabled) {
                NotificationHandler.notify(Text.translatable("notification.quickresourcepack.enabled", displayName));
            } else {
                NotificationHandler.notify(Text.translatable("notification.quickresourcepack.disabled", displayName));
            }
        }));
    }

    public static void applyPack() {
        if (isLoading) return;

        String packId = ModConfig.getInstance().selectedResourcePack;
        if (packId == null || packId.isEmpty()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        ResourcePackManager manager = client.getResourcePackManager();
        manager.scanPacks();

        if (!manager.getIds().contains(packId)) {
            QuickResourcePackMod.LOGGER.warn("Auto-apply: pack '{}' not found", packId);
            return;
        }

        if (manager.getEnabledIds().contains(packId)) return;

        List<String> profiles = new ArrayList<>(manager.getEnabledIds());
        profiles.add(packId);
        manager.setEnabledProfiles(profiles);

        isLoading = true;

        client.reloadResources().thenRun(() -> client.execute(() -> isLoading = false));
    }
}
