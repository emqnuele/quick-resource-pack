package com.emqnuele.quickresourcepack.handler;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import com.emqnuele.quickresourcepack.config.ModConfig;

public class KeybindHandler {
    private static boolean wasPressed = false;

    public static void register() {
        // We do NOT register a KeyBinding here anymore to avoid crashes on
        // 1.21.9/Lunar.
        // Instead, we manually poll the key in the client tick event.

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.getWindow() == null)
                return;

            int keyCode = ModConfig.getInstance().toggleKeyCode;
            // Use GLFW directly to avoid mapping issues with InputUtil.isKeyPressed
            boolean isPressed = org.lwjgl.glfw.GLFW.glfwGetKey(client.getWindow().getHandle(),
                    keyCode) == org.lwjgl.glfw.GLFW.GLFW_PRESS;

            if (isPressed && !wasPressed) {
                // Key just pressed
                ResourcePackHandler.togglePack();
            }

            wasPressed = isPressed;
        });
    }
}
