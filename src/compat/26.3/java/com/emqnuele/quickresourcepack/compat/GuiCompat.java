package com.emqnuele.quickresourcepack.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.network.chat.Component;

// minecraft 26.3: overlays, toasts and the action bar are owned by Gui
public final class GuiCompat {
    private GuiCompat() {
    }

    public static Overlay overlay(Minecraft client) {
        return client.gui.overlay();
    }

    public static void setOverlay(Minecraft client, Overlay overlay) {
        client.gui.setOverlay(overlay);
    }

    public static ToastManager toasts(Minecraft client) {
        return client.gui.toastManager();
    }

    public static void actionBar(Minecraft client, Component message) {
        client.gui.hud.setOverlayMessage(message, false);
    }
}
