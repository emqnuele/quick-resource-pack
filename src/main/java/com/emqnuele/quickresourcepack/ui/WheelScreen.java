package com.emqnuele.quickresourcepack.ui;

import com.emqnuele.quickresourcepack.compat.ScreenCompat;
import com.emqnuele.quickresourcepack.config.ConfigStore;
import com.emqnuele.quickresourcepack.core.config.QrpConfig;
import com.emqnuele.quickresourcepack.core.config.Slot;
import com.emqnuele.quickresourcepack.core.config.SlotMode;
import com.emqnuele.quickresourcepack.core.logic.Anim;
import com.emqnuele.quickresourcepack.core.logic.PackUniverse;
import com.emqnuele.quickresourcepack.core.logic.SelectionPlanner;
import com.emqnuele.quickresourcepack.core.logic.WheelGeometry;
import com.emqnuele.quickresourcepack.input.KeyNames;
import com.emqnuele.quickresourcepack.pack.PackIndex;
import com.emqnuele.quickresourcepack.pack.PackSwitcher;
import com.emqnuele.quickresourcepack.pack.SlotNames;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class WheelScreen extends Screen {
    private static final long CLICK_MODE_AFTER_MS = 350;
    private static final int CENTER_ICON = 32;

    private record Item(Slot slot, String iconPack, boolean active, Component action, int actionColor) {
    }

    private final InputConstants.Key trigger;
    private final boolean heldOpen;
    private final long openedAt = Util.getMillis();
    private final List<Item> items = new ArrayList<>();
    private float[] lift = new float[0];
    private float open;
    private int hovered = -1;
    private long lastFrame = Util.getMillis();
    private boolean closing;

    public WheelScreen(InputConstants.Key trigger, boolean heldOpen) {
        super(Component.translatable("qrp.wheel.title"));
        this.trigger = trigger;
        this.heldOpen = heldOpen;
    }

    @Override
    protected void init() {
        if (!items.isEmpty()) {
            return;
        }
        QrpConfig config = ConfigStore.get();
        PackUniverse universe = PackIndex.universe();
        List<String> selected = PackIndex.selected();
        for (Slot slot : config.wheelSlots()) {
            SelectionPlanner.Status status = SelectionPlanner.status(slot, selected, universe);
            SelectionPlanner.Plan next = SelectionPlanner.press(slot, selected, universe);
            List<String> usable = SelectionPlanner.usablePacks(slot, universe);
            String icon = status.cycleIndex() >= 0 && status.cycleIndex() < usable.size()
                    ? usable.get(status.cycleIndex())
                    : slot.packs.getFirst();
            items.add(item(slot, icon, status.active(), next));
        }
        lift = new float[items.size()];
        open = Paint.animations() ? 0f : 1f;
    }

    private static Item item(Slot slot, String icon, boolean active, SelectionPlanner.Plan next) {
        if (!next.changed()) {
            return new Item(slot, icon, active, Component.translatable("qrp.wheel.no_change"), Paint.DARK_GRAY);
        }
        if (slot.mode == SlotMode.CYCLE && next.enabled()) {
            return new Item(slot, icon, active, Component.translatable("qrp.wheel.next", PackIndex.title(next.shownPack())), Paint.GREEN);
        }
        return next.enabled()
                ? new Item(slot, icon, active, Component.translatable("qrp.wheel.turn_on"), Paint.GREEN)
                : new Item(slot, icon, active, Component.translatable("qrp.wheel.turn_off"), Paint.GRAY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean isInGameUi() {
        return true;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        extractBlurredBackground(g);
        g.fill(0, 0, width, height, ARGB.color(open * 0.45f, 0x000000));
    }

    // pixel art stays crisp only at whole multiples, so the ring doubles instead of growing smoothly
    private int scale() {
        return Math.min(width, height) >= 230 ? 2 : 1;
    }

    private int radius(int scale) {
        int cell = Paint.CELL * scale;
        int fits = (int) WheelGeometry.radiusFor(items.size(), cell, 0, 4 * scale);
        // leave room in the middle for the name and a 32 pixel icon
        int middle = 56 + cell / 2;
        int screenLimit = Math.min(width, height) / 2 - cell / 2 - 16;
        return Math.max(Math.min(Math.max(fits, middle), screenLimit), cell);
    }

    private int deadZone() {
        return Math.max(16, radius(scale()) / 3);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        long now = Util.getMillis();
        float dt = Math.min(0.1f, (now - lastFrame) / 1000f);
        lastFrame = now;
        boolean animate = Paint.animations();
        // critically damped approach: the ring settles without bouncing
        open = animate ? Anim.approach(open, 1f, 26f, dt) : 1f;

        int scale = scale();
        int cx = width / 2;
        int cy = height / 2;
        int ring = Math.round(radius(scale) * (0.86f + 0.14f * open));
        int count = items.size();
        hovered = WheelGeometry.indexAt(mouseX - cx, mouseY - cy, count, deadZone());

        for (int i = 0; i < count; i++) {
            float target = i == hovered ? 1f : 0f;
            lift[i] = animate ? Anim.approach(lift[i], target, 30f, dt) : target;
            renderCell(g, i, cx, cy, ring, scale);
        }
        renderCenter(g, cx, cy);

        Component hint = Component.translatable(heldOpen ? "qrp.wheel.hint_hold" : "qrp.wheel.hint_click",
                trigger.getDisplayName());
        int hintY = Math.min(height - 14, cy + radius(scale) + Paint.CELL * scale / 2 + 8);
        Paint.centered(g, font, hint.getString(), cx, hintY, Paint.GRAY, open);
    }

    private void renderCell(GuiGraphicsExtractor g, int index, int cx, int cy, int ring, int scale) {
        Item item = items.get(index);
        double angle = WheelGeometry.angleOf(index, items.size());
        // the hovered cell leans out toward the pointer, like a slot being pulled
        float out = ring + lift[index] * 3 * scale;
        int px = Math.round(cx + (float) Math.cos(angle) * out);
        int py = Math.round(cy + (float) Math.sin(angle) * out);

        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(px, py);
        pose.scale(scale, scale);
        int x = -Paint.CELL / 2;
        int y = -Paint.CELL / 2;
        Paint.hotbarCell(g, x, y, open);
        Paint.packIcon(g, item.iconPack(), x + Paint.CELL_WELL, y + Paint.CELL_WELL, 16, open);
        if (index == hovered) {
            Paint.hotbarSelection(g, x, y, open);
        }
        if (item.active()) {
            g.blitSprite(RenderPipelines.GUI_TEXTURED, Paint.CHECKMARK, x + Paint.CELL - 7, y - 3, 9, 8, ARGB.white(open));
        }
        pose.popMatrix();
    }

    private void renderCenter(GuiGraphicsExtractor g, int cx, int cy) {
        int scale = scale();
        int maxWidth = Math.max(60, (radius(scale) - Paint.CELL * scale / 2) * 2 - 12);
        if (hovered < 0) {
            Component prompt = Component.translatable(heldOpen ? "qrp.wheel.release_cancel" : "qrp.wheel.pick");
            Paint.centered(g, font, Paint.ellipsize(font, prompt.getString(), maxWidth), cx, cy - 4, Paint.GRAY, open);
            return;
        }
        Item item = items.get(hovered);
        int top = cy - (CENTER_ICON + 4 + 12 + 12 + 9) / 2;
        Paint.packIcon(g, item.iconPack(), cx - CENTER_ICON / 2, top, CENTER_ICON, open);
        int y = top + CENTER_ICON + 4;
        Paint.centered(g, font, Paint.ellipsize(font, SlotNames.plain(item.slot()), maxWidth), cx, y, Paint.WHITE, open);
        y += 12;
        Paint.centered(g, font, Paint.ellipsize(font, item.action().getString(), maxWidth), cx, y, item.actionColor(), open);
        y += 12;
        String mode = Component.translatable("qrp.mode." + item.slot().mode.name().toLowerCase(Locale.ROOT)).getString();
        String detail = item.slot().isBound() ? mode + "  " + KeyNames.display(item.slot().key).getString() : mode;
        Paint.centered(g, font, Paint.ellipsize(font, detail, maxWidth), cx, y, Paint.DARK_GRAY, open);
    }

    public boolean isTrigger(InputConstants.Key key) {
        return trigger.equals(key);
    }

    public void onTriggerReleased() {
        if (hovered >= 0) {
            confirm(hovered);
        } else if (heldOpen || Util.getMillis() - openedAt > CLICK_MODE_AFTER_MS) {
            onClose();
        }
    }

    public void onTriggerPressedAgain(InputConstants.Key key) {
        if (!heldOpen && isTrigger(key) && Util.getMillis() - openedAt > 120) {
            if (hovered >= 0) {
                confirm(hovered);
            } else {
                onClose();
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_RIGHT) {
            onClose();
            return true;
        }
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            // the click position is authoritative; the last rendered frame may be stale
            int index = WheelGeometry.indexAt(event.x() - width / 2.0, event.y() - height / 2.0, items.size(), deadZone());
            if (index >= 0) {
                confirm(index);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int digit = digitOf(event.key());
        if (digit >= 1 && digit <= Math.min(9, items.size())) {
            confirm(digit - 1);
            return true;
        }
        return super.keyPressed(event);
    }

    private static int digitOf(int key) {
        return switch (key) {
            case InputConstants.KEY_1, InputConstants.KEY_NUMPAD1 -> 1;
            case InputConstants.KEY_2, InputConstants.KEY_NUMPAD2 -> 2;
            case InputConstants.KEY_3, InputConstants.KEY_NUMPAD3 -> 3;
            case InputConstants.KEY_4, InputConstants.KEY_NUMPAD4 -> 4;
            case InputConstants.KEY_5, InputConstants.KEY_NUMPAD5 -> 5;
            case InputConstants.KEY_6, InputConstants.KEY_NUMPAD6 -> 6;
            case InputConstants.KEY_7, InputConstants.KEY_NUMPAD7 -> 7;
            case InputConstants.KEY_8, InputConstants.KEY_NUMPAD8 -> 8;
            case InputConstants.KEY_9, InputConstants.KEY_NUMPAD9 -> 9;
            default -> -1;
        };
    }

    private void confirm(int index) {
        if (closing || index < 0 || index >= items.size()) {
            return;
        }
        closing = true;
        Slot slot = items.get(index).slot();
        ScreenCompat.setScreen(minecraft, null);
        PackSwitcher.press(slot, PackSwitcher.Cause.WHEEL);
    }

    @Override
    public void onClose() {
        closing = true;
        ScreenCompat.setScreen(minecraft, null);
    }
}
