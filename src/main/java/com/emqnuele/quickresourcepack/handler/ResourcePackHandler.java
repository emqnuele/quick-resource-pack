package com.emqnuele.quickresourcepack.handler;

import com.emqnuele.quickresourcepack.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.text.Text;
import java.util.ArrayList;
import java.util.List;

public class ResourcePackHandler {
    private static boolean isLoading = false;
    public static boolean isSilentReload = false;

    public static void togglePack() {
        if (isLoading)
            return;

        String packName = ModConfig.getInstance().selectedResourcePack;
        if (packName == null || packName.isEmpty()) {
            NotificationHandler.showToast(
                    Text.translatable("notification.quickresourcepack.nopack"),
                    Text.literal(""));
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        ResourcePackManager manager = client.getResourcePackManager();
        manager.scanPacks(); // Refresh available packs

        if (!manager.getIds().contains(packName)) {
            NotificationHandler.showToast(
                    Text.translatable("notification.quickresourcepack.error", "Pack not found"),
                    Text.literal(packName));
            return;
        }

        boolean isEnabled = manager.getEnabledIds().contains(packName);
        isLoading = true;
        isSilentReload = true;

        if (isEnabled) {
            // Disable
            NotificationHandler.showToast(
                    Text.translatable("notification.quickresourcepack.loading", packName),
                    Text.literal("Disabling..."));

            List<String> enabled = new ArrayList<>(manager.getEnabledIds());
            enabled.remove(packName);
            manager.setEnabledProfiles(enabled);
        } else {
            // Enable
            NotificationHandler.showToast(
                    Text.translatable("notification.quickresourcepack.loading", packName),
                    Text.literal("Enabling..."));

            List<String> enabled = new ArrayList<>(manager.getEnabledIds());
            enabled.add(packName);
            manager.setEnabledProfiles(enabled);
        }

        // Reload resources
        client.reloadResources().thenRun(() -> {
            isLoading = false;
            isSilentReload = false;
            if (client.getResourcePackManager().getEnabledIds().contains(packName)) {
                NotificationHandler.showToast(
                        Text.translatable("notification.quickresourcepack.enabled", packName),
                        Text.literal(""));
            } else {
                NotificationHandler.showToast(
                        Text.translatable("notification.quickresourcepack.disabled", packName),
                        Text.literal(""));
            }
        });
    }
}
