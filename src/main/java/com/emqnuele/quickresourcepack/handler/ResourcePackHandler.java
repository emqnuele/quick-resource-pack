package com.emqnuele.quickresourcepack.handler;

import com.emqnuele.quickresourcepack.QuickResourcePackMod;
import com.emqnuele.quickresourcepack.config.ModConfig;
import com.emqnuele.quickresourcepack.core.PackToggleLogic;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ResourcePackHandler {
    private ResourcePackHandler() {
    }

    private static final AtomicBoolean LOADING = new AtomicBoolean(false);

    static boolean isLoading() {
        return LOADING.get();
    }

    public static String getPackDisplayName(String packId) {
        if (!PackToggleLogic.isValidPackId(packId)) {
            return "";
        }
        PackRepository repository = Minecraft.getInstance().getResourcePackRepository();
        for (Pack pack : repository.getAvailablePacks()) {
            if (pack.getId().equals(packId)) {
                return pack.getTitle().getString();
            }
        }
        return packId;
    }

    public static void togglePack() {
        if (!LOADING.compareAndSet(false, true)) {
            return;
        }
        try {
            String packId = ModConfig.getInstance().selectedResourcePack;
            if (!PackToggleLogic.isValidPackId(packId)) {
                LOADING.set(false);
                NotificationHandler.notify(Component.translatable("notification.quickresourcepack.nopack"));
                return;
            }

            Minecraft client = Minecraft.getInstance();
            PackRepository repository = client.getResourcePackRepository();
            repository.reload();

            if (!repository.getAvailableIds().contains(packId)) {
                LOADING.set(false);
                NotificationHandler.notify(Component.translatable("notification.quickresourcepack.notfound", packId));
                return;
            }

            PackToggleLogic.ToggleResult result =
                    PackToggleLogic.toggle(List.copyOf(repository.getSelectedIds()), packId);
            repository.setSelected(result.selectedIds());

            reloadAndNotify(client, packId);
        } catch (RuntimeException e) {
            LOADING.set(false);
            QuickResourcePackMod.LOGGER.error("Failed to toggle resource pack", e);
        }
    }

    public static void applyPack() {
        if (!LOADING.compareAndSet(false, true)) {
            return;
        }
        try {
            String packId = ModConfig.getInstance().selectedResourcePack;
            if (!PackToggleLogic.isValidPackId(packId)) {
                LOADING.set(false);
                return;
            }

            Minecraft client = Minecraft.getInstance();
            PackRepository repository = client.getResourcePackRepository();
            repository.reload();

            if (!repository.getAvailableIds().contains(packId)) {
                LOADING.set(false);
                QuickResourcePackMod.LOGGER.warn("Auto-apply: pack '{}' not found", packId);
                return;
            }

            if (repository.getSelectedIds().contains(packId)) {
                LOADING.set(false);
                return;
            }

            repository.setSelected(PackToggleLogic.ensureAdded(List.copyOf(repository.getSelectedIds()), packId));

            client.reloadResourcePacks().whenComplete((ignored, error) -> client.execute(() -> {
                LOADING.set(false);
                if (error != null) {
                    QuickResourcePackMod.LOGGER.error("Auto-apply: reload failed", error);
                }
            }));
        } catch (RuntimeException e) {
            LOADING.set(false);
            QuickResourcePackMod.LOGGER.error("Auto-apply failed", e);
        }
    }

    private static void reloadAndNotify(Minecraft client, String packId) {
        client.reloadResourcePacks().whenComplete((ignored, error) -> client.execute(() -> {
            LOADING.set(false);
            if (error != null) {
                QuickResourcePackMod.LOGGER.error("Resource pack reload failed", error);
                return;
            }
            boolean nowEnabled = client.getResourcePackRepository().getSelectedIds().contains(packId);
            String displayName = getPackDisplayName(packId);
            if (nowEnabled) {
                NotificationHandler.notify(Component.translatable("notification.quickresourcepack.enabled", displayName));
            } else {
                NotificationHandler.notify(Component.translatable("notification.quickresourcepack.disabled", displayName));
            }
        }));
    }
}
