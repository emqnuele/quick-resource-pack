package com.emqnuele.quickresourcepack.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.network.chat.Component;

// minecraft 26.1.x: overlays and toasts still belong to Minecraft itself
public final class GuiCompat {
    private GuiCompat() {
    }

    public static Overlay overlay(Minecraft client) {
        return client.getOverlay();
    }

    public static void setOverlay(Minecraft client, Overlay overlay) {
        client.setOverlay(overlay);
    }

    public static ToastManager toasts(Minecraft client) {
        return client.getToastManager();
    }

    public static void actionBar(Minecraft client, Component message) {
        client.gui.setOverlayMessage(message, false);
    }
}
