package com.emqnuele.quickresourcepack.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;

import org.junit.jupiter.api.Test;

class PackDisplayNamesTest {
    @Test
    void buildsOrderedMap() {
        var packs = List.of(
                new PackDisplayNames.PackEntry("a", "Alpha"),
                new PackDisplayNames.PackEntry("b", "Beta"));
        LinkedHashMap<String, String> map = PackDisplayNames.buildDisplayMap(packs);
        assertEquals(2, map.size());
        assertEquals("a", map.get("Alpha"));
        assertEquals("b", map.get("Beta"));
        assertEquals(List.of("Alpha", "Beta"), List.copyOf(map.keySet()));
    }

    @Test
    void disambiguatesDuplicateTitles() {
        var packs = List.of(
                new PackDisplayNames.PackEntry("id-one", "Same"),
                new PackDisplayNames.PackEntry("id-two", "Same"));
        var map = PackDisplayNames.buildDisplayMap(packs);
        assertEquals(2, map.size());
        assertEquals("id-one", map.get("Same"));
        assertEquals("id-two", map.get("Same [id-two]"));
    }

    @Test
    void skipsEntriesWithoutIdAndFallsBackToIdForBlankTitle() {
        var packs = List.of(
                new PackDisplayNames.PackEntry("", "Nope"),
                new PackDisplayNames.PackEntry(null, "Nope"),
                new PackDisplayNames.PackEntry("file/pack.zip", ""));
        var map = PackDisplayNames.buildDisplayMap(packs);
        assertEquals(1, map.size());
        assertEquals("file/pack.zip", map.get("file/pack.zip"));
    }

    @Test
    void handlesNullInput() {
        assertTrue(PackDisplayNames.buildDisplayMap(null).isEmpty());
        assertEquals("", PackDisplayNames.displayNameFor("x", null));
        assertEquals("", PackDisplayNames.displayNameFor(null, new LinkedHashMap<>()));
    }

    @Test
    void reverseLookupAndSelections() {
        var map = PackDisplayNames.buildDisplayMap(List.of(
                new PackDisplayNames.PackEntry("a", "Alpha")));
        assertEquals("Alpha", PackDisplayNames.displayNameFor("a", map));
        assertEquals("", PackDisplayNames.displayNameFor("missing", map));
        assertEquals(List.of("", "Alpha"), PackDisplayNames.selectionsWithNone(map));
    }
}
