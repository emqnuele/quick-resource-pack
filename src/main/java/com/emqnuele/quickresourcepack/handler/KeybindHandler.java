package com.emqnuele.quickresourcepack.handler;

import com.emqnuele.quickresourcepack.QuickResourcePackMod;
import com.emqnuele.quickresourcepack.compat.ScreenCompat;
import com.emqnuele.quickresourcepack.core.KeybindGuard;
import com.emqnuele.quickresourcepack.screen.QuickMenuScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;

public final class KeybindHandler {
    private KeybindHandler() {
    }

    private static KeyMapping toggleBinding;
    private static KeyMapping menuBinding;

    public static void register() {
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(QuickResourcePackMod.MOD_ID, "keys"));

        toggleBinding = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.quickresourcepack.toggle",
                InputConstants.KEY_R,
                category));

        menuBinding = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.quickresourcepack.open_menu",
                InputConstants.UNKNOWN.getValue(),
                category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Drain every press even when suppressed: otherwise a key hit in chat
            // stays queued and fires later once the screen closes (issue #3).
            boolean togglePressed = false;
            while (toggleBinding.consumeClick()) {
                togglePressed = true;
            }
            boolean menuPressed = false;
            while (menuBinding.consumeClick()) {
                menuPressed = true;
            }

            if (togglePressed && KeybindGuard.shouldHandleToggle(ScreenCompat.getScreen(client) != null, client.player != null)) {
                ResourcePackHandler.togglePack();
            }

            if (menuPressed) {
                Screen current = ScreenCompat.getScreen(client);
                boolean quickMenuOpen = current instanceof QuickMenuScreen;
                if (KeybindGuard.shouldHandleMenu(current != null, quickMenuOpen)) {
                    setScreen(client, quickMenuOpen ? null : new QuickMenuScreen(current));
                }
            }
        });
    }

    private static void setScreen(Minecraft client, Screen screen) {
        ScreenCompat.setScreen(client, screen);
    }

    public static KeyMapping getToggleBinding() {
        return toggleBinding;
    }

    public static KeyMapping getMenuBinding() {
        return menuBinding;
    }
}
