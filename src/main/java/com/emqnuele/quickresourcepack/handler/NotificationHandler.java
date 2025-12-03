package com.emqnuele.quickresourcepack.handler;

import com.emqnuele.quickresourcepack.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.text.Text;

public class NotificationHandler {
    public static void showToast(Text title, Text description) {
        if (!ModConfig.getInstance().showNotifications)
            return;

        MinecraftClient.getInstance().getToastManager().add(new SystemToast(
                SystemToast.Type.PERIODIC_NOTIFICATION,
                title,
                description));
    }

    public static void showMessage(Text message) {
        if (!ModConfig.getInstance().showNotifications)
            return;

        if (MinecraftClient.getInstance().player != null) {
            MinecraftClient.getInstance().player.sendMessage(message, true);
        }
    }
}
