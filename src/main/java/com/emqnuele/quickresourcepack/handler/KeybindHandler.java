package com.emqnuele.quickresourcepack.handler;

import com.emqnuele.quickresourcepack.screen.QuickMenuScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class KeybindHandler {

    private static KeyBinding TOGGLE_BINDING;
    private static KeyBinding MENU_BINDING;

    private static Object createCategory(Class<?> categoryClass, String categoryName, Exception[] errors) {
        for (String factory : new String[]{"create", "of"}) {
            try {
                Method stringFactory;
                try {
                    stringFactory = categoryClass.getMethod(factory, String.class);
                } catch (NoSuchMethodException ignored) {
                    stringFactory = categoryClass.getDeclaredMethod(factory, String.class);
                    stringFactory.setAccessible(true);
                }
                return stringFactory.invoke(null, categoryName);
            } catch (ReflectiveOperationException | IllegalArgumentException e) {
                errors[0] = e;
            }
        }

        for (String fieldName : new String[]{"MISC", "GAMEPLAY", "MOVEMENT"}) {
            try {
                Field categoryField;
                try {
                    categoryField = categoryClass.getField(fieldName);
                } catch (NoSuchFieldException ignored) {
                    categoryField = categoryClass.getDeclaredField(fieldName);
                    categoryField.setAccessible(true);
                }
                Object value = categoryField.get(null);
                if (value != null) {
                    return value;
                }
            } catch (ReflectiveOperationException | IllegalArgumentException e) {
                errors[0] = e;
            }
        }

        return null;
    }

    private static KeyBinding tryConstruct(Class<?>[] parameterTypes, Object[] args, Exception[] errors) {
        try {
            Constructor<KeyBinding> constructor;
            try {
                constructor = KeyBinding.class.getConstructor(parameterTypes);
            } catch (NoSuchMethodException ignored) {
                constructor = KeyBinding.class.getDeclaredConstructor(parameterTypes);
                constructor.setAccessible(true);
            }
            return constructor.newInstance(args);
        } catch (ReflectiveOperationException | IllegalArgumentException e) {
            errors[0] = e;
            return null;
        }
    }

    private static KeyBinding createKeyBinding(String translationKey, int keyCode) {
        Exception[] errors = new Exception[1];
        Class<?> categoryClass = null;
        Object category = null;

        try {
            categoryClass = Class.forName("net.minecraft.client.option.KeyBinding$Category");
            category = createCategory(categoryClass, "category.quickresourcepack", errors);
        } catch (ReflectiveOperationException e) {
            errors[0] = e;
        }

        if (category != null && categoryClass != null) {
            KeyBinding binding = tryConstruct(
                    new Class<?>[]{String.class, int.class, categoryClass},
                    new Object[]{translationKey, keyCode, category},
                    errors
            );
            if (binding != null) {
                return binding;
            }

            binding = tryConstruct(
                    new Class<?>[]{String.class, InputUtil.Type.class, int.class, categoryClass},
                    new Object[]{translationKey, InputUtil.Type.KEYSYM, keyCode, category},
                    errors
            );
            if (binding != null) {
                return binding;
            }

            binding = tryConstruct(
                    new Class<?>[]{String.class, InputUtil.Type.class, int.class, categoryClass, int.class},
                    new Object[]{translationKey, InputUtil.Type.KEYSYM, keyCode, category, 0},
                    errors
            );
            if (binding != null) {
                return binding;
            }
        }

        KeyBinding binding = tryConstruct(
                new Class<?>[]{String.class, InputUtil.Type.class, int.class, String.class},
                new Object[]{translationKey, InputUtil.Type.KEYSYM, keyCode, "category.quickresourcepack"},
                errors
        );
        if (binding != null) {
            return binding;
        }

        binding = tryConstruct(
                new Class<?>[]{String.class, int.class, String.class},
                new Object[]{translationKey, keyCode, "category.quickresourcepack"},
                errors
        );
        if (binding != null) {
            return binding;
        }

        throw new IllegalStateException("failed to create key binding", errors[0]);
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
