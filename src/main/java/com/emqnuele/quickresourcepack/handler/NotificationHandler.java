package com.emqnuele.quickresourcepack.handler;

import com.emqnuele.quickresourcepack.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class NotificationHandler {
    public static void notify(Component message) {
        if (!ModConfig.getInstance().showNotifications) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.sendSystemMessage(message);
        }
    }
}
