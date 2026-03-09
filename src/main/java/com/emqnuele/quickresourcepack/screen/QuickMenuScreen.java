package com.emqnuele.quickresourcepack.screen;

import com.emqnuele.quickresourcepack.config.ModConfig;
import com.emqnuele.quickresourcepack.handler.ResourcePackHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class QuickMenuScreen extends Screen {

    private static final int PANEL_W = 240;
    private static final int PAD     = 12;
    private static final int BTN_H   = 20;
    private static final int GAP     = 4;
    private static final int PANEL_H = PAD + BTN_H + GAP + BTN_H + GAP + BTN_H + PAD;

    private int px, py;
    private final Screen parent;

    public QuickMenuScreen(Screen parent) {
        super(Text.translatable("screen.quickresourcepack.menu"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        px = (this.width  - PANEL_W) / 2;
        py = (this.height - PANEL_H) / 2;

        int cx   = px + PAD;
        int cw   = PANEL_W - PAD * 2;
        int half = (cw - GAP) / 2;

        ModConfig cfg = ModConfig.getInstance();

        int row1 = py + PAD;
        int row2 = row1 + BTN_H + GAP;
        int row3 = row2 + BTN_H + GAP;

        addDrawableChild(ButtonWidget.builder(
                        Text.translatable("button.quickresourcepack.toggle"),
                        b -> { ResourcePackHandler.togglePack(); close(); })
                .dimensions(cx, row1, half, BTN_H)
                .build());

        addDrawableChild(ButtonWidget.builder(
                        Text.translatable("button.quickresourcepack.select"),
                        b -> client.setScreen(new ResourcePackPickerScreen(this)))
                .dimensions(cx + half + GAP, row1, half, BTN_H)
                .build());

        addDrawableChild(ButtonWidget.builder(
                        notifText(cfg.showNotifications),
                        b -> {
                            cfg.showNotifications = !cfg.showNotifications;
                            ModConfig.save();
                            b.setMessage(notifText(cfg.showNotifications));
                        })
                .dimensions(cx, row2, cw, BTN_H)
                .build());

        addDrawableChild(ButtonWidget.builder(
                        autoApplyText(cfg.autoApplyOnStart),
                        b -> {
                            cfg.autoApplyOnStart = !cfg.autoApplyOnStart;
                            ModConfig.save();
                            b.setMessage(autoApplyText(cfg.autoApplyOnStart));
                        })
                .dimensions(cx, row3, cw, BTN_H)
                .build());
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);
        context.fill(px - 2, py - 2, px + PANEL_W + 2, py + PANEL_H + 2, 0xFF000000);
        context.fill(px - 1, py - 1, px + PANEL_W + 1, py + PANEL_H + 1, 0xFFAAAAAA);
        context.fill(px, py, px + PANEL_W, py + PANEL_H, 0xFF2D2D2D);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
    }

    private Text notifText(boolean on) {
        return Text.translatable("screen.quickresourcepack.menu.notifications",
                on ? Text.translatable("options.on") : Text.translatable("options.off"));
    }

    private Text autoApplyText(boolean on) {
        return Text.translatable("screen.quickresourcepack.menu.autoapply",
                on ? Text.translatable("options.on") : Text.translatable("options.off"));
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
