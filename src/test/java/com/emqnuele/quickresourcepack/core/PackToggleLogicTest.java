package com.emqnuele.quickresourcepack.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class PackToggleLogicTest {
    @Test
    void toggleAddsMissingPack() {
        var result = PackToggleLogic.toggle(List.of("a", "b"), "c");
        assertEquals(List.of("a", "b", "c"), result.selectedIds());
        assertTrue(result.nowEnabled());
    }

    @Test
    void toggleRemovesPresentPack() {
        var result = PackToggleLogic.toggle(Arrays.asList("a", "b", "c"), "b");
        assertEquals(List.of("a", "c"), result.selectedIds());
        assertFalse(result.nowEnabled());
    }

    @Test
    void toggleDoesNotMutateInput() {
        List<String> input = new ArrayList<>(List.of("a"));
        PackToggleLogic.toggle(input, "b");
        assertEquals(List.of("a"), input);
    }

    @Test
    void toggleTreatsNullSelectionAsEmpty() {
        var result = PackToggleLogic.toggle(null, "pack");
        assertEquals(List.of("pack"), result.selectedIds());
        assertTrue(result.nowEnabled());
    }

    @Test
    void toggleRejectsBlankIds() {
        assertThrows(IllegalArgumentException.class, () -> PackToggleLogic.toggle(List.of(), null));
        assertThrows(IllegalArgumentException.class, () -> PackToggleLogic.toggle(List.of(), ""));
    }

    @Test
    void isValidPackId() {
        assertTrue(PackToggleLogic.isValidPackId("file/pack.zip"));
        assertFalse(PackToggleLogic.isValidPackId(null));
        assertFalse(PackToggleLogic.isValidPackId(""));
    }

    @Test
    void ensureAddedIsIdempotent() {
        assertEquals(List.of("a"), PackToggleLogic.ensureAdded(List.of("a"), "a"));
        assertEquals(List.of("a", "b"), PackToggleLogic.ensureAdded(List.of("a"), "b"));
        assertEquals(List.of("x"), PackToggleLogic.ensureAdded(null, "x"));
    }
}
