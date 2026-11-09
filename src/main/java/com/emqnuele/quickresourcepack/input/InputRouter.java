package com.emqnuele.quickresourcepack.input;

import com.emqnuele.quickresourcepack.QuickResourcePackMod;
import com.emqnuele.quickresourcepack.compat.ScreenCompat;
import com.emqnuele.quickresourcepack.config.ConfigStore;
import com.emqnuele.quickresourcepack.core.KeybindGuard;
import com.emqnuele.quickresourcepack.core.config.KeyCombo;
import com.emqnuele.quickresourcepack.core.config.QrpConfig;
import com.emqnuele.quickresourcepack.core.config.Slot;
import com.emqnuele.quickresourcepack.core.logic.HoldDetector;
import com.emqnuele.quickresourcepack.pack.PackSwitcher;
import com.emqnuele.quickresourcepack.ui.HubScreen;
import com.emqnuele.quickresourcepack.ui.WheelScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Turns raw key and mouse events into actions. The main key is vanilla-bound
 * (so it lives in Controls and keeps old bindings); slot combos are our own
 * because vanilla bindings have no modifiers.
 */
public final class InputRouter {
    private static final int PRESS = 1;
    private static final int RELEASE = 0;

    private static final HoldDetector MAIN_HOLD = new HoldDetector();
    private static KeyMapping mainKey;
    private static KeyMapping wheelKey;
    private static KeyMapping menuKey;

    private InputRouter() {
    }

    public static void register() {
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(QuickResourcePackMod.MOD_ID, "keys"));
        // ids kept from 1.x so existing bindings in options.txt carry over
        mainKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.quickresourcepack.toggle", InputConstants.KEY_R, category));
        wheelKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.quickresourcepack.wheel", InputConstants.UNKNOWN.getValue(), category));
        menuKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.quickresourcepack.open_menu", InputConstants.UNKNOWN.getValue(), category));
    }

    public static int modifiers(boolean shift, boolean control, boolean alt) {
        return (shift ? KeyCombo.MOD_SHIFT : 0) | (control ? KeyCombo.MOD_CONTROL : 0) | (alt ? KeyCombo.MOD_ALT : 0);
    }

    /** Every key and mouse button event, before vanilla handles it. */
    public static void onInput(long handle, InputConstants.Key key, int action, int modifiers) {
        Minecraft minecraft = Minecraft.getInstance();
        if (handle == 0L || handle != minecraft.getWindow().handle() || mainKey == null) {
            return;
        }
        try {
            if (action == PRESS) {
                onPress(minecraft, key, modifiers);
            } else if (action == RELEASE) {
                onRelease(minecraft, key);
            }
        } catch (RuntimeException e) {
            // never let a bad config crash input handling for the whole game
            QuickResourcePackMod.LOGGER.error("Quick Resource Pack input handling failed", e);
        }
    }

    private static void onPress(Minecraft minecraft, InputConstants.Key key, int modifiers) {
        Screen screen = ScreenCompat.getScreen(minecraft);
        if (screen instanceof WheelScreen wheel) {
            wheel.onTriggerPressedAgain(key);
            return;
        }
        if (!KeybindGuard.shouldHandleToggle(screen != null, minecraft.player != null)) {
            if (isBoundTo(menuKey, key) && screen instanceof HubScreen) {
                ScreenCompat.setScreen(minecraft, null);
            }
            return;
        }
        // f3 plus a key is a debug action; it must not also switch packs
        if (minecraft.options.keyDebugModifier.isDown()) {
            return;
        }
        QrpConfig config = ConfigStore.get();
        List<Slot> bound = slotsBoundTo(config, key.getName(), modifiers);
        for (Slot slot : bound) {
            PackSwitcher.press(slot, PackSwitcher.Cause.KEY);
        }
        // vanilla bindings ignore modifiers: shift+r bound to a slot must not also fire plain r
        boolean comboTaken = !bound.isEmpty() && modifiers != 0;
        if (comboTaken) {
            return;
        }
        if (isBoundTo(mainKey, key)) {
            if (canUseWheel(config)) {
                MAIN_HOLD.press(Util.getMillis());
            } else {
                firePrimary(minecraft, config);
            }
        }
        if (isBoundTo(wheelKey, key)) {
            openWheel(minecraft, key, false);
        }
        if (isBoundTo(menuKey, key)) {
            HubScreen.open(null);
        }
    }

    private static void onRelease(Minecraft minecraft, InputConstants.Key key) {
        Screen screen = ScreenCompat.getScreen(minecraft);
        if (screen instanceof WheelScreen wheel && wheel.isTrigger(key)) {
            MAIN_HOLD.cancel();
            wheel.onTriggerReleased();
            return;
        }
        if (isBoundTo(mainKey, key) && MAIN_HOLD.isDown()) {
            HoldDetector.Event event = MAIN_HOLD.release(Util.getMillis(), holdThreshold());
            if (event == HoldDetector.Event.TAP && screen == null) {
                firePrimary(minecraft, ConfigStore.get());
            }
        }
    }

    /** Polled every frame so the wheel opens the moment the hold threshold passes. */
    public static void frame() {
        if (!MAIN_HOLD.isDown()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Screen screen = ScreenCompat.getScreen(minecraft);
        if (screen != null || minecraft.player == null) {
            MAIN_HOLD.cancel();
            return;
        }
        if (MAIN_HOLD.poll(Util.getMillis(), holdThreshold()) == HoldDetector.Event.HOLD_STARTED) {
            openWheel(minecraft, mainKeyBound(), true);
        }
    }

    public static void tick(Minecraft minecraft) {
        // vanilla queues a click per press; drain them so nothing replays when a screen closes
        while (mainKey.consumeClick()) {
            continue;
        }
        while (wheelKey.consumeClick()) {
            continue;
        }
        while (menuKey.consumeClick()) {
            continue;
        }
        // a focus loss swallows the release event; never leave the hold stuck down
        if (MAIN_HOLD.isDown() && ScreenCompat.getScreen(minecraft) == null && !mainKey.isDown()) {
            MAIN_HOLD.cancel();
        }
    }

    private static void firePrimary(Minecraft minecraft, QrpConfig config) {
        Optional<Slot> primary = config.primarySlot();
        if (primary.isEmpty() || !primary.get().hasPacks()) {
            // first press on a fresh install: show the editor instead of an error
            HubScreen.open(null);
            return;
        }
        PackSwitcher.press(primary.get(), PackSwitcher.Cause.KEY);
    }

    private static void openWheel(Minecraft minecraft, InputConstants.Key trigger, boolean heldOpen) {
        QrpConfig config = ConfigStore.get();
        if (config.wheelSlots().isEmpty()) {
            HubScreen.open(null);
            return;
        }
        ScreenCompat.setScreen(minecraft, new WheelScreen(trigger, heldOpen));
    }

    private static boolean canUseWheel(QrpConfig config) {
        return config.behavior.wheel && config.wheelSlots().size() > 1;
    }

    private static long holdThreshold() {
        return ConfigStore.get().behavior.wheelHoldMs;
    }

    // the same test as KeyMapping.matches(Key), which 26.1 does not have
    private static boolean isBoundTo(KeyMapping mapping, InputConstants.Key key) {
        return !mapping.isUnbound() && KeyMappingHelper.getBoundKeyOf(mapping).equals(key);
    }

    public static List<Slot> slotsBoundTo(QrpConfig config, String keyName, int modifiers) {
        List<Slot> result = new ArrayList<>();
        for (Slot slot : config.slots) {
            if (slot.key != null && slot.key.matches(keyName, modifiers)) {
                result.add(slot);
            }
        }
        return result;
    }

    public static InputConstants.Key mainKeyBound() {
        return InputConstants.getKey(mainKey.saveString());
    }

    public static KeyMapping mainKey() {
        return mainKey;
    }

    public static KeyMapping wheelKey() {
        return wheelKey;
    }

    public static KeyMapping menuKey() {
        return menuKey;
    }
}
