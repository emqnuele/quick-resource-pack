package com.emqnuele.quickresourcepack.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

// minecraft 26.3: screen management moved from Minecraft to Gui
public final class ScreenCompat {
    private ScreenCompat() {
    }

    public static Screen getScreen(Minecraft client) {
        return client.gui.screen();
    }

    public static void setScreen(Minecraft client, Screen screen) {
        client.gui.setScreen(screen);
    }
}
