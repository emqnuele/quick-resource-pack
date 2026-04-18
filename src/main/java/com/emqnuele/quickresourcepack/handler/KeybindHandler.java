package com.emqnuele.quickresourcepack.handler;

import com.emqnuele.quickresourcepack.QuickResourcePackMod;
import com.emqnuele.quickresourcepack.screen.QuickMenuScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class KeybindHandler {

    private static KeyMapping TOGGLE_BINDING;
    private static KeyMapping MENU_BINDING;

    public static void register() {
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(QuickResourcePackMod.MOD_ID, "keys")
        );

        TOGGLE_BINDING = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.quickresourcepack.toggle",
                GLFW.GLFW_KEY_R,
                category
        ));

        MENU_BINDING = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.quickresourcepack.open_menu",
                GLFW.GLFW_KEY_UNKNOWN,
                category
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (TOGGLE_BINDING.consumeClick()) {
                ResourcePackHandler.togglePack();
            }

            while (MENU_BINDING.consumeClick()) {
                if (client.screen instanceof QuickMenuScreen) {
                    client.setScreen(null);
                } else {
                    client.setScreen(new QuickMenuScreen(client.screen));
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
