package com.emqnuele.quickresourcepack.pack;

import com.emqnuele.quickresourcepack.core.config.Slot;
import net.minecraft.network.chat.Component;

public final class SlotNames {
    private SlotNames() {
    }

    /** A slot without a name is called after its first pack, which is what players recognise. */
    public static Component of(Slot slot) {
        if (slot.name != null && !slot.name.isBlank()) {
            return Component.literal(slot.name);
        }
        if (!slot.packs.isEmpty()) {
            return PackIndex.title(slot.packs.getFirst());
        }
        return Component.translatable("qrp.slot.unnamed");
    }

    public static String plain(Slot slot) {
        return of(slot).getString();
    }
}
