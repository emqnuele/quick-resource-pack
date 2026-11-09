package com.emqnuele.quickresourcepack.ui;

import com.emqnuele.quickresourcepack.compat.PackCompat;
import com.emqnuele.quickresourcepack.compat.ScreenCompat;
import com.emqnuele.quickresourcepack.config.ConfigStore;
import com.emqnuele.quickresourcepack.core.config.QrpConfig;
import com.emqnuele.quickresourcepack.hud.ActivePacksHud;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.Locale;

public final class QrpOptionsScreen extends Screen {
    private static final int MIN_HOLD = 80;
    private static final int MAX_HOLD = 1000;

    private final Screen parent;
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);

    public QrpOptionsScreen(Screen parent) {
        super(Component.translatable("qrp.options.title"));
        this.parent = parent;
    }

    private static Tooltip tip(String key) {
        return Tooltip.create(Component.translatable(key + ".tooltip"));
    }

    @Override
    protected void init() {
        QrpConfig config = ConfigStore.get();
        QrpConfig.Appearance look = config.appearance;
        QrpConfig.Behavior behavior = config.behavior;
        layout.addTitleHeader(title, font);

        GridLayout grid = new GridLayout().columnSpacing(10).rowSpacing(4);
        GridLayout.RowHelper rows = grid.createRowHelper(2);

        rows.addChild(CycleButton.builder(
                        (QrpConfig.NotificationStyle s) -> Component.translatable("qrp.options.notifications." + s.name().toLowerCase(Locale.ROOT)),
                        look.notifications)
                .withValues(QrpConfig.NotificationStyle.values())
                .withTooltip(s -> tip("qrp.options.notifications"))
                .create(0, 0, 150, 20, Component.translatable("qrp.options.notifications"), (b, s) -> look.notifications = s));
        rows.addChild(CycleButton.onOffBuilder(behavior.seamlessReload)
                .withTooltip(on -> tip("qrp.options.seamless"))
                .create(0, 0, 150, 20, Component.translatable("qrp.options.seamless"), (b, on) -> behavior.seamlessReload = on));

        rows.addChild(CycleButton.onOffBuilder(behavior.wheel)
                .withTooltip(on -> tip("qrp.options.wheel"))
                .create(0, 0, 150, 20, Component.translatable("qrp.options.wheel"), (b, on) -> behavior.wheel = on));
        rows.addChild(new HoldSlider(behavior));

        rows.addChild(CycleButton.onOffBuilder(look.hud)
                .withTooltip(on -> tip("qrp.options.hud"))
                .create(0, 0, 150, 20, Component.translatable("qrp.options.hud"), (b, on) -> {
                    look.hud = on;
                    ActivePacksHud.invalidate();
                }));
        rows.addChild(CycleButton.builder(
                        (QrpConfig.HudCorner c) -> Component.translatable("qrp.options.corner." + c.name().toLowerCase(Locale.ROOT)),
                        look.hudCorner)
                .withValues(QrpConfig.HudCorner.values())
                .create(0, 0, 150, 20, Component.translatable("qrp.options.corner"), (b, c) -> look.hudCorner = c));

        rows.addChild(CycleButton.booleanBuilder(Component.translatable("qrp.options.hud_only_active.on"),
                        Component.translatable("qrp.options.hud_only_active.off"), look.hudOnlyActive)
                .create(0, 0, 150, 20, Component.translatable("qrp.options.hud_only_active"), (b, on) -> {
                    look.hudOnlyActive = on;
                    ActivePacksHud.invalidate();
                }));
        rows.addChild(CycleButton.onOffBuilder(look.animations)
                .withTooltip(on -> tip("qrp.options.animations"))
                .create(0, 0, 150, 20, Component.translatable("qrp.options.animations"), (b, on) -> look.animations = on));

        rows.addChild(Button.builder(Component.translatable("qrp.options.controls"),
                b -> ScreenCompat.setScreen(minecraft, new KeyBindsScreen(this, minecraft.options))).width(150).build());
        rows.addChild(Button.builder(Component.translatable("pack.openFolder"),
                b -> PackCompat.openFolder(minecraft.getResourcePackDirectory())).width(150).build());

        layout.addToContents(grid);
        layout.addToFooter(Button.builder(CommonComponents.GUI_DONE, b -> onClose()).width(200).build());
        layout.visitWidgets(this::addRenderableWidget);
        repositionElements();
    }

    @Override
    protected void repositionElements() {
        layout.arrangeElements();
    }

    @Override
    public void removed() {
        ConfigStore.save();
    }

    @Override
    public void onClose() {
        ScreenCompat.setScreen(minecraft, parent);
    }

    private static final class HoldSlider extends AbstractSliderButton {
        private final QrpConfig.Behavior behavior;

        HoldSlider(QrpConfig.Behavior behavior) {
            super(0, 0, 150, 20, Component.empty(), (behavior.wheelHoldMs - MIN_HOLD) / (double) (MAX_HOLD - MIN_HOLD));
            this.behavior = behavior;
            setTooltip(tip("qrp.options.hold"));
            updateMessage();
        }

        private int millis() {
            // steps of 10 ms: finer than that is noise to a human hand
            return Math.round((float) Mth.lerp(value, MIN_HOLD, MAX_HOLD) / 10f) * 10;
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable("qrp.options.hold", millis()));
        }

        @Override
        protected void applyValue() {
            behavior.wheelHoldMs = millis();
        }
    }
}
