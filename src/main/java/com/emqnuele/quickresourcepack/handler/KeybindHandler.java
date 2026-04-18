package com.emqnuele.quickresourcepack.handler;

import com.emqnuele.quickresourcepack.screen.QuickMenuScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class KeybindHandler {

    private static KeyBinding TOGGLE_BINDING;
    private static KeyBinding MENU_BINDING;

    private static KeyBinding createKeyBinding(String translationKey, int keyCode) {
        try {
            Class<?> categoryClass = Class.forName("net.minecraft.client.option.KeyBinding$Category");
            Object category = null;

            for (String factory : new String[]{"create", "of"}) {
                try {
                    Method factoryMethod = categoryClass.getMethod(factory, String.class);
                    category = factoryMethod.invoke(null, "category.quickresourcepack");
                    break;
                } catch (NoSuchMethodException ignored) {
                }
            }

            if (category != null) {
                Constructor<KeyBinding> ctor = KeyBinding.class.getConstructor(
                        String.class,
                        InputUtil.Type.class,
                        int.class,
                        categoryClass
                );
                return ctor.newInstance(translationKey, InputUtil.Type.KEYSYM, keyCode, category);
            }
        } catch (ReflectiveOperationException ignored) {
        }

        try {
            Constructor<KeyBinding> legacyCtor = KeyBinding.class.getConstructor(
                    String.class,
                    InputUtil.Type.class,
                    int.class,
                    String.class
            );
            return legacyCtor.newInstance(translationKey, InputUtil.Type.KEYSYM, keyCode, "category.quickresourcepack");
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("failed to create key binding", e);
        }
    }

    public static void register() {
        TOGGLE_BINDING = KeyBindingHelper.registerKeyBinding(createKeyBinding("key.quickresourcepack.toggle", GLFW.GLFW_KEY_R));
        MENU_BINDING = KeyBindingHelper.registerKeyBinding(createKeyBinding("key.quickresourcepack.open_menu", GLFW.GLFW_KEY_UNKNOWN));

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
