package com.emqnuele.quickresourcepack.handler;

import com.emqnuele.quickresourcepack.QuickResourcePackMod;
import com.emqnuele.quickresourcepack.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;

import java.util.ArrayList;
import java.util.List;

public class ResourcePackHandler {
    private static volatile boolean isLoading = false;

    public static String getPackDisplayName(String packId) {
        if (packId == null || packId.isEmpty()) return "";
        PackRepository repository = Minecraft.getInstance().getResourcePackRepository();
        for (Pack pack : repository.getAvailablePacks()) {
            if (pack.getId().equals(packId)) {
                return pack.getTitle().getString();
            }
        }
        return packId;
    }

    public static void togglePack() {
        if (isLoading) return;

        String packId = ModConfig.getInstance().selectedResourcePack;
        if (packId == null || packId.isEmpty()) {
            NotificationHandler.notify(Component.translatable("notification.quickresourcepack.nopack"));
            return;
        }

        Minecraft client = Minecraft.getInstance();
        PackRepository repository = client.getResourcePackRepository();
        repository.reload();

        if (!repository.getAvailableIds().contains(packId)) {
            String displayName = getPackDisplayName(packId);
            NotificationHandler.notify(Component.translatable("notification.quickresourcepack.notfound", displayName));
            return;
        }

        boolean enabled = repository.getSelectedIds().contains(packId);
        List<String> profiles = new ArrayList<>(repository.getSelectedIds());

        if (enabled) {
            profiles.remove(packId);
        } else {
            profiles.add(packId);
        }

        repository.setSelected(profiles);
        isLoading = true;

        client.reloadResourcePacks().thenRun(() -> client.execute(() -> {
            isLoading = false;
            boolean nowEnabled = client.getResourcePackRepository().getSelectedIds().contains(packId);
            String displayName = getPackDisplayName(packId);
            if (nowEnabled) {
                NotificationHandler.notify(Component.translatable("notification.quickresourcepack.enabled", displayName));
            } else {
                NotificationHandler.notify(Component.translatable("notification.quickresourcepack.disabled", displayName));
            }
        }));
    }

    public static void applyPack() {
        if (isLoading) return;

        String packId = ModConfig.getInstance().selectedResourcePack;
        if (packId == null || packId.isEmpty()) return;

        Minecraft client = Minecraft.getInstance();
        PackRepository repository = client.getResourcePackRepository();
        repository.reload();

        if (!repository.getAvailableIds().contains(packId)) {
            QuickResourcePackMod.LOGGER.warn("Auto-apply: pack '{}' not found", packId);
            return;
        }

        if (repository.getSelectedIds().contains(packId)) return;

        List<String> profiles = new ArrayList<>(repository.getSelectedIds());
        profiles.add(packId);
        repository.setSelected(profiles);

        isLoading = true;

        client.reloadResourcePacks().thenRun(() -> client.execute(() -> isLoading = false));
    }
}
