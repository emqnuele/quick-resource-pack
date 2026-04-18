package com.emqnuele.quickresourcepack.screen;

import com.emqnuele.quickresourcepack.config.ClothConfigScreen;
import com.emqnuele.quickresourcepack.config.ModConfig;
import com.emqnuele.quickresourcepack.handler.ResourcePackHandler;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class QuickMenuScreen extends Screen {
    private static final int BUTTON_W = 230;
    private static final int BUTTON_H = 20;
    private static final int GAP = 6;

    private final Screen parent;

    private Button selectedPackButton;
    private Button notificationsButton;
    private Button autoApplyButton;

    public QuickMenuScreen(Screen parent) {
        super(Component.translatable("screen.quickresourcepack.menu"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        ModConfig cfg = ModConfig.getInstance();

        int x = this.width / 2 - BUTTON_W / 2;
        int y = this.height / 2 - (BUTTON_H * 5 + GAP * 4) / 2;

        this.addRenderableWidget(Button.builder(
                        Component.translatable("button.quickresourcepack.toggle"),
                        button -> {
                            ResourcePackHandler.togglePack();
                            this.onClose();
                        })
                .bounds(x, y, BUTTON_W, BUTTON_H)
                .build());

        y += BUTTON_H + GAP;
        this.selectedPackButton = this.addRenderableWidget(Button.builder(
                        selectedPackText(),
                        button -> this.minecraft.setScreen(ClothConfigScreen.createConfigScreen(this)))
                .bounds(x, y, BUTTON_W, BUTTON_H)
                .build());

        y += BUTTON_H + GAP;
        this.notificationsButton = this.addRenderableWidget(Button.builder(
                        notificationsText(cfg.showNotifications),
                        button -> {
                            cfg.showNotifications = !cfg.showNotifications;
                            ModConfig.save();
                            this.notificationsButton.setMessage(notificationsText(cfg.showNotifications));
                        })
                .bounds(x, y, BUTTON_W, BUTTON_H)
                .build());

        y += BUTTON_H + GAP;
        this.autoApplyButton = this.addRenderableWidget(Button.builder(
                        autoApplyText(cfg.autoApplyOnStart),
                        button -> {
                            cfg.autoApplyOnStart = !cfg.autoApplyOnStart;
                            ModConfig.save();
                            this.autoApplyButton.setMessage(autoApplyText(cfg.autoApplyOnStart));
                        })
                .bounds(x, y, BUTTON_W, BUTTON_H)
                .build());

        y += BUTTON_H + GAP;
        this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.done"),
                        button -> this.onClose())
                .bounds(x, y, BUTTON_W, BUTTON_H)
                .build());
    }

    @Override
    public void tick() {
        if (this.selectedPackButton != null) {
            this.selectedPackButton.setMessage(selectedPackText());
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    private Component selectedPackText() {
        String packId = ModConfig.getInstance().selectedResourcePack;
        String displayName;
        if (packId == null || packId.isEmpty()) {
            displayName = Component.translatable("screen.quickresourcepack.picker.none").getString();
        } else {
            displayName = ResourcePackHandler.getPackDisplayName(packId);
        }

        return Component.translatable("screen.quickresourcepack.menu.pack", displayName);
    }

    private Component notificationsText(boolean enabled) {
        return Component.translatable(
                "screen.quickresourcepack.menu.notifications",
                enabled ? Component.translatable("options.on") : Component.translatable("options.off")
        );
    }

    private Component autoApplyText(boolean enabled) {
        return Component.translatable(
                "screen.quickresourcepack.menu.autoapply",
                enabled ? Component.translatable("options.on") : Component.translatable("options.off")
        );
    }
}
