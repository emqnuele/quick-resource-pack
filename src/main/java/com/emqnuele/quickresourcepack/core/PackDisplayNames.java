package com.emqnuele.quickresourcepack.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Display-name mapping for the pack picker. Duplicate titles are disambiguated
 * as {@code "Title [id]"} so every key stays unique and selectable.
 */
public final class PackDisplayNames {
    private PackDisplayNames() {
    }

    public record PackEntry(String id, String title) {
    }

    /** Builds an insertion-ordered displayName -&gt; id map from already-filtered packs. */
    public static LinkedHashMap<String, String> buildDisplayMap(List<PackEntry> packs) {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        if (packs == null) {
            return map;
        }
        for (PackEntry pack : packs) {
            if (pack == null || pack.id() == null || pack.id().isEmpty()) {
                continue;
            }
            String title = pack.title() == null || pack.title().isEmpty() ? pack.id() : pack.title();
            if (map.containsKey(title)) {
                map.put(title + " [" + pack.id() + "]", pack.id());
            } else {
                map.put(title, pack.id());
            }
        }
        return map;
    }

    /** Reverse lookup: display name for a pack id, or {@code ""} when not present. */
    public static String displayNameFor(String packId, Map<String, String> displayMap) {
        if (packId == null || packId.isEmpty() || displayMap == null) {
            return "";
        }
        for (Map.Entry<String, String> entry : displayMap.entrySet()) {
            if (packId.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return "";
    }

    /** Ordered display names with a leading empty entry for "none selected". */
    public static List<String> selectionsWithNone(Map<String, String> displayMap) {
        List<String> selections = new ArrayList<>();
        selections.add("");
        if (displayMap != null) {
            selections.addAll(displayMap.keySet());
        }
        return selections;
    }
}
