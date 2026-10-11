package com.emqnuele.quickresourcepack.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

// minecraft 26.1.x: screens are managed directly by Minecraft
public final class ScreenCompat {
    private ScreenCompat() {
    }

    public static Screen getScreen(Minecraft client) {
        return client.screen;
    }

    public static void setScreen(Minecraft client, Screen screen) {
        client.setScreen(screen);
    }
}
