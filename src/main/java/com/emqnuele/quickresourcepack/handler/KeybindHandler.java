package com.emqnuele.quickresourcepack.handler;

import com.emqnuele.quickresourcepack.QuickResourcePackMod;
import com.emqnuele.quickresourcepack.compat.ScreenCompat;
import com.emqnuele.quickresourcepack.screen.QuickMenuScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;

public class KeybindHandler {

    private static KeyMapping TOGGLE_BINDING;
    private static KeyMapping MENU_BINDING;

    public static void register() {
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(QuickResourcePackMod.MOD_ID, "keys")
        );

        TOGGLE_BINDING = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.quickresourcepack.toggle",
                InputConstants.KEY_R,
                category
        ));

        MENU_BINDING = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.quickresourcepack.open_menu",
                InputConstants.UNKNOWN.getValue(),
                category
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (TOGGLE_BINDING.consumeClick()) {
                ResourcePackHandler.togglePack();
            }

            while (MENU_BINDING.consumeClick()) {
                Screen current = ScreenCompat.getScreen(client);
                if (current instanceof QuickMenuScreen) {
                    ScreenCompat.setScreen(client, null);
                } else {
                    ScreenCompat.setScreen(client, new QuickMenuScreen(current));
                }
            }
        });
    }

    public static KeyMapping getToggleBinding() {
        return TOGGLE_BINDING;
    }

    public static KeyMapping getMenuBinding() {
        return MENU_BINDING;
    }
}
