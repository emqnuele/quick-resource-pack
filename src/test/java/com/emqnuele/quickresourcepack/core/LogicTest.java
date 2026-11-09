package com.emqnuele.quickresourcepack.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.emqnuele.quickresourcepack.core.config.KeyCombo;
import com.emqnuele.quickresourcepack.core.config.Rule;
import com.emqnuele.quickresourcepack.core.logic.Anim;
import com.emqnuele.quickresourcepack.core.logic.Colors;
import com.emqnuele.quickresourcepack.core.logic.Glob;
import com.emqnuele.quickresourcepack.core.logic.HoldDetector;
import com.emqnuele.quickresourcepack.core.logic.ReloadScheduler;
import com.emqnuele.quickresourcepack.core.logic.RuleEngine;
import com.emqnuele.quickresourcepack.core.logic.WheelGeometry;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class LogicTest {
    @Test
    void schedulerCoalescesRequestsDuringAReload() {
        var scheduler = new ReloadScheduler();
        assertTrue(scheduler.submit(List.of("a")));
        assertFalse(scheduler.submit(List.of("b")));
        assertFalse(scheduler.submit(List.of("c")));
        assertEquals(List.of("c"), scheduler.base(List.of("live")));
        assertEquals(List.of("c"), scheduler.complete().orElseThrow());
        assertTrue(scheduler.isBusy());
        assertTrue(scheduler.complete().isEmpty());
        assertFalse(scheduler.isBusy());
        assertEquals(List.of("live"), scheduler.base(List.of("live")));
    }

    @Test
    void globMatching() {
        assertTrue(Glob.matches("*.hypixel.net", "mc.hypixel.net"));
        assertTrue(Glob.matches("MC.Example.?et", "mc.example.net"));
        assertFalse(Glob.matches("*.hypixel.net", "hypixel.net.evil.com"));
        assertTrue(Glob.matches("*", ""));
        assertFalse(Glob.matches("a", null));
    }

    @Test
    void serverRulesIgnorePortUnlessGiven() {
        var rule = Rule.of(Rule.Trigger.SERVER, "play.example.net", "s");
        assertTrue(RuleEngine.matches(rule, new RuleEngine.Context("play.example.net:25566", null, null)));
        var withPort = Rule.of(Rule.Trigger.SERVER, "play.example.net:25565", "s");
        assertFalse(RuleEngine.matches(withPort, new RuleEngine.Context("play.example.net:25566", null, null)));
    }

    @Test
    void dimensionRulesAcceptBareNames() {
        var rule = Rule.of(Rule.Trigger.DIMENSION, "the_nether", "s");
        assertTrue(RuleEngine.matches(rule, new RuleEngine.Context(null, null, "minecraft:the_nether")));
        assertFalse(RuleEngine.matches(rule, new RuleEngine.Context(null, null, "minecraft:overworld")));
    }

    @Test
    void wantedSlotsSkipDisabledAndIncompleteRules() {
        var on = Rule.of(Rule.Trigger.WORLD, "My*", "one");
        var off = Rule.of(Rule.Trigger.WORLD, "My*", "two");
        off.enabled = false;
        var incomplete = Rule.of(Rule.Trigger.WORLD, "", "three");
        var wanted = RuleEngine.wantedSlots(List.of(on, off, incomplete, on),
                new RuleEngine.Context(null, "My World", null));
        assertEquals(Set.of("one"), wanted);
        assertTrue(RuleEngine.wantedSlots(List.of(on), RuleEngine.Context.NONE).isEmpty());
    }

    @Test
    void holdDetectorTellsTapFromHold() {
        var detector = new HoldDetector();
        detector.press(0);
        assertEquals(HoldDetector.Event.TAP, detector.release(100, 200));

        detector.press(1000);
        assertEquals(HoldDetector.Event.NONE, detector.poll(1100, 200));
        assertEquals(HoldDetector.Event.HOLD_STARTED, detector.poll(1250, 200));
        assertEquals(HoldDetector.Event.NONE, detector.poll(1300, 200));
        assertEquals(HoldDetector.Event.HOLD_RELEASED, detector.release(1400, 200));
        assertEquals(HoldDetector.Event.NONE, detector.release(1500, 200));
    }

    @Test
    void wheelSectorsStartAtTopClockwise() {
        assertEquals(0, WheelGeometry.indexAt(0, -50, 4, 10));
        assertEquals(1, WheelGeometry.indexAt(50, 0, 4, 10));
        assertEquals(2, WheelGeometry.indexAt(0, 50, 4, 10));
        assertEquals(3, WheelGeometry.indexAt(-50, 0, 4, 10));
        assertEquals(-1, WheelGeometry.indexAt(3, 3, 4, 10));
        assertEquals(0, WheelGeometry.indexAt(-10, -50, 4, 10));
        assertTrue(WheelGeometry.radiusFor(12, 32, 40, 6) > 40);
    }

    @Test
    void keyComboMatchesExactlyButReleasesLoosely() {
        var combo = new KeyCombo("key.keyboard.g", KeyCombo.MOD_CONTROL);
        assertTrue(combo.matches("key.keyboard.g", KeyCombo.MOD_CONTROL));
        assertFalse(combo.matches("key.keyboard.g", 0));
        assertFalse(combo.matches("key.keyboard.g", KeyCombo.MOD_CONTROL | KeyCombo.MOD_SHIFT));
        assertTrue(combo.matchesRelease("key.keyboard.g"));
        assertFalse(new KeyCombo("key.keyboard.unknown", 0).isBound());
        assertTrue(KeyCombo.isModifierKey("key.keyboard.left.shift"));
        assertTrue(new KeyCombo("key.mouse.4", 0).isMouse());
    }

    @Test
    void animAndColorsStayInRange() {
        assertEquals(1f, Anim.easeOutCubic(2f));
        assertEquals(0f, Anim.easeOutCubic(-1f));
        assertEquals(10f, Anim.approach(0, 10, 1000, 1));
        assertEquals(0x80FFFFFF, Colors.withAlpha(0xFFFFFFFF, 128));
        assertEquals(0x40FFFFFF, Colors.fade(0x80FFFFFF, 0.5f));
        assertEquals(0xFF000000, Colors.lerp(0, 0xFF000000, 0xFFFFFFFF));
        assertEquals(0xFF111114, Colors.contrastText(0xFFFFFFFF));
    }
}
