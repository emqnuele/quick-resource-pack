package com.emqnuele.quickresourcepack.handler;

import com.emqnuele.quickresourcepack.screen.QuickMenuScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class KeybindHandler {

    private static KeyBinding TOGGLE_BINDING;
    private static KeyBinding MENU_BINDING;

    public static void register() {
        TOGGLE_BINDING = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.quickresourcepack.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                "category.quickresourcepack"
        ));

        MENU_BINDING = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.quickresourcepack.open_menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                "category.quickresourcepack"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (TOGGLE_BINDING.wasPressed()) {
                ResourcePackHandler.togglePack();
            }
            while (MENU_BINDING.wasPressed()) {
                if (client.currentScreen instanceof QuickMenuScreen) {
                    client.setScreen(null);
                } else {
                    client.setScreen(new QuickMenuScreen(client.currentScreen));
                }
            }
        });
    }

    public static KeyBinding getToggleBinding() {
        return TOGGLE_BINDING;
    }

    public static KeyBinding getMenuBinding() {
        return MENU_BINDING;
    }
}
