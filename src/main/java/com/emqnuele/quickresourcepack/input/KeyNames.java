package com.emqnuele.quickresourcepack.input;

import com.emqnuele.quickresourcepack.core.config.KeyCombo;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;

public final class KeyNames {
    private KeyNames() {
    }

    public static Component display(KeyCombo combo) {
        if (combo == null || !combo.isBound()) {
            return Component.translatable("qrp.key.none");
        }
        MutableComponent text = Component.empty();
        if (combo.has(KeyCombo.MOD_CONTROL)) {
            text.append(Component.translatable("qrp.key.ctrl")).append(" + ");
        }
        if (combo.has(KeyCombo.MOD_SHIFT)) {
            text.append(Component.translatable("qrp.key.shift")).append(" + ");
        }
        if (combo.has(KeyCombo.MOD_ALT)) {
            text.append(Component.translatable("qrp.key.alt")).append(" + ");
        }
        return text.append(keyName(combo.key));
    }

    public static Component keyName(String name) {
        try {
            return InputConstants.getKey(name).getDisplayName();
        } catch (RuntimeException e) {
            // a name saved by another game version may not exist here
            return Component.literal(name);
        }
    }

    /** Vanilla bindings that share the plain key, so the editor can warn before the player is surprised. */
    public static List<Component> vanillaConflicts(KeyCombo combo) {
        List<Component> conflicts = new ArrayList<>();
        if (combo == null || !combo.isBound() || combo.modifiers != 0) {
            return conflicts;
        }
        var options = Minecraft.getInstance().options;
        for (KeyMapping mapping : options.keyMappings) {
            // debug actions only fire with the debug modifier held, so a bare key never meets them
            boolean debugCombo = mapping.getCategory() == KeyMapping.Category.DEBUG
                    && mapping != options.keyDebugOverlay && mapping != options.keyDebugModifier;
            if (!debugCombo && mapping.saveString().equals(combo.key)) {
                conflicts.add(Component.translatable(mapping.getName()));
            }
        }
        return conflicts;
    }
}
