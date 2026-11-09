package com.emqnuele.quickresourcepack.ui;

import com.emqnuele.quickresourcepack.config.ConfigStore;
import com.emqnuele.quickresourcepack.core.config.KeyCombo;
import com.emqnuele.quickresourcepack.core.config.Slot;
import com.emqnuele.quickresourcepack.core.config.SlotMode;
import com.emqnuele.quickresourcepack.input.KeyNames;
import com.emqnuele.quickresourcepack.pack.SlotNames;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class Labels {
    private Labels() {
    }

    public static Component mode(SlotMode mode) {
        return Component.translatable("qrp.mode." + mode.name().toLowerCase(Locale.ROOT));
    }

    public static Component modeTooltip(SlotMode mode) {
        return Component.translatable("qrp.mode." + mode.name().toLowerCase(Locale.ROOT) + ".tooltip");
    }

    public static List<Component> conflicts(@Nullable KeyCombo combo, String slotId) {
        List<Component> conflicts = new ArrayList<>(KeyNames.vanillaConflicts(combo));
        if (combo == null || !combo.isBound()) {
            return conflicts;
        }
        for (Slot other : ConfigStore.get().slots) {
            if (!other.id.equals(slotId) && combo.equals(other.key)) {
                conflicts.add(SlotNames.of(other));
            }
        }
        return conflicts;
    }

    public static Component keyButton(@Nullable KeyCombo combo, boolean listening, boolean clash) {
        Component key = KeyNames.display(combo);
        if (listening) {
            return Component.literal("> ").append(key.copy().withStyle(ChatFormatting.WHITE, ChatFormatting.UNDERLINE))
                    .append(" <").withStyle(ChatFormatting.YELLOW);
        }
        if (clash) {
            return Component.literal("[ ").append(key.copy().withStyle(ChatFormatting.WHITE)).append(" ]")
                    .withStyle(ChatFormatting.YELLOW);
        }
        return key;
    }

    public static @Nullable Tooltip clashTooltip(List<Component> conflicts) {
        if (conflicts.isEmpty()) {
            return null;
        }
        MutableComponent names = Component.empty();
        for (int i = 0; i < conflicts.size(); i++) {
            if (i > 0) {
                names.append(", ");
            }
            names.append(conflicts.get(i));
        }
        return Tooltip.create(Component.translatable("qrp.key.clash", names));
    }
}
