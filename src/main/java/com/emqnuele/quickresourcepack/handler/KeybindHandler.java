package com.emqnuele.quickresourcepack.handler;

import com.emqnuele.quickresourcepack.QuickResourcePackMod;
import com.emqnuele.quickresourcepack.core.KeybindGuard;
import com.emqnuele.quickresourcepack.screen.QuickMenuScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

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
                GLFW.GLFW_KEY_R,
                category));

        menuBinding = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.quickresourcepack.open_menu",
                GLFW.GLFW_KEY_UNKNOWN,
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

            if (togglePressed && KeybindGuard.shouldHandleToggle(client.screen != null, client.player != null)) {
                ResourcePackHandler.togglePack();
            }

            if (menuPressed) {
                boolean quickMenuOpen = client.screen instanceof QuickMenuScreen;
                if (KeybindGuard.shouldHandleMenu(client.screen != null, quickMenuOpen)) {
                    setScreen(client, quickMenuOpen ? null : new QuickMenuScreen(client.screen));
                }
            }
        });
    }

    private static void setScreen(Minecraft client, net.minecraft.client.gui.screens.Screen screen) {
        client.setScreen(screen);
    }

    public static KeyMapping getToggleBinding() {
        return toggleBinding;
    }

    public static KeyMapping getMenuBinding() {
        return menuBinding;
    }
}
