package com.emqnuele.quickresourcepack.handler;

import com.emqnuele.quickresourcepack.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class NotificationHandler {
    public static void notify(Text message) {
        if (!ModConfig.getInstance().showNotifications) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(message, true);
        }
    }
}
