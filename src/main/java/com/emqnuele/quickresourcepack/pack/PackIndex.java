package com.emqnuele.quickresourcepack.pack;

import com.emqnuele.quickresourcepack.core.logic.PackUniverse;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Read-only view of the client pack repository in the shapes the rest of the mod needs. */
public final class PackIndex {
    private PackIndex() {
    }

    public record Entry(String id, Component title, Component description, boolean pinned, boolean compatible) {
    }

    private static PackRepository repository() {
        return Minecraft.getInstance().getResourcePackRepository();
    }

    /** Rescans the resourcepacks folder: picks up packs added while the game runs. */
    public static void refresh() {
        repository().reload();
        PackIcons.invalidate();
    }

    public static PackUniverse universe() {
        Set<String> available = new HashSet<>();
        Set<String> pinned = new HashSet<>();
        Set<String> pinnedTop = new HashSet<>();
        for (Pack pack : repository().getAvailablePacks()) {
            available.add(pack.getId());
            if (isPinned(pack)) {
                pinned.add(pack.getId());
                if (pack.getDefaultPosition() == Pack.Position.TOP) {
                    pinnedTop.add(pack.getId());
                }
            }
        }
        return new PackUniverse(available, pinned, pinnedTop);
    }

    /** Selected ids, bottom to top. */
    public static List<String> selected() {
        return List.copyOf(repository().getSelectedIds());
    }

    public static boolean exists(String id) {
        return id != null && repository().isAvailable(id);
    }

    public static boolean isEnabled(String id) {
        return repository().getSelectedIds().contains(id);
    }

    public static Component title(String id) {
        Pack pack = id == null ? null : repository().getPack(id);
        if (pack != null) {
            return displayTitle(pack);
        }
        return Component.literal(prettyId(id));
    }

    // zip packs are titled by file name; the extension is noise in a switcher
    private static Component displayTitle(Pack pack) {
        Component title = pack.getTitle();
        String text = title.getString();
        if (text.toLowerCase(Locale.ROOT).endsWith(".zip")) {
            return Component.literal(text.substring(0, text.length() - 4));
        }
        return title;
    }

    public static Component description(String id) {
        Pack pack = id == null ? null : repository().getPack(id);
        return pack != null ? pack.getDescription() : Component.empty();
    }

    /** Packs a player can put in a slot, enabled ones first, then by name. */
    public static List<Entry> selectable() {
        Set<String> enabled = new HashSet<>(repository().getSelectedIds());
        List<Entry> entries = new ArrayList<>();
        for (Pack pack : repository().getAvailablePacks()) {
            if (isPinned(pack)) {
                continue;
            }
            entries.add(new Entry(pack.getId(), displayTitle(pack), pack.getDescription(), false,
                    pack.getCompatibility().isCompatible()));
        }
        entries.sort(Comparator.<Entry, Boolean>comparing(e -> !enabled.contains(e.id()))
                .thenComparing(e -> e.title().getString().toLowerCase(Locale.ROOT)));
        return entries;
    }

    /** Enabled packs the player controls, top first like the vanilla screen. */
    public static List<String> enabledUserPacks() {
        List<String> ids = new ArrayList<>();
        for (Pack pack : repository().getSelectedPacks()) {
            if (!isPinned(pack)) {
                ids.add(pack.getId());
            }
        }
        java.util.Collections.reverse(ids);
        return ids;
    }

    static boolean isPinned(Pack pack) {
        return pack.isRequired() || pack.isFixedPosition();
    }

    /** "file/Faithful 32x.zip" reads better as "Faithful 32x" when the pack itself is gone. */
    public static String prettyId(String id) {
        if (id == null || id.isEmpty()) {
            return "";
        }
        String name = id.startsWith("file/") ? id.substring(5) : id;
        if (name.toLowerCase(Locale.ROOT).endsWith(".zip")) {
            name = name.substring(0, name.length() - 4);
        }
        return name;
    }
}
