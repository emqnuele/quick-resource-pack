package com.emqnuele.quickresourcepack.pack;

import com.emqnuele.quickresourcepack.core.config.Slot;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * What the player should be told about one slot action.
 *
 * @param packId  pack whose icon and name represent the action
 * @param enabled state of the slot after the action
 */
public record Feedback(String slotId, Component slotName, String packId, boolean enabled,
                       PackSwitcher.Cause cause, List<String> missing) {
    public static Feedback of(Slot slot, String packId, boolean enabled, PackSwitcher.Cause cause, List<String> missing) {
        return new Feedback(slot.id, SlotNames.of(slot), packId, enabled, cause, List.copyOf(missing));
    }

    public Component packTitle() {
        return PackIndex.title(packId);
    }
}
