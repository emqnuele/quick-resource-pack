package com.emqnuele.quickresourcepack.integration;

import com.emqnuele.quickresourcepack.ui.HubScreen;
import com.emqnuele.quickresourcepack.ui.Paint;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

public final class PackScreenHook {
    private PackScreenHook() {
    }

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (screen instanceof PackSelectionScreen && isResourcePackScreen(screen)) {
                // same size and sprite metrics as the language and accessibility buttons
                SpriteIconButton button = SpriteIconButton.builder(Component.translatable("qrp.packscreen.button"),
                                b -> HubScreen.open(screen), true)
                        .width(20)
                        .sprite(Paint.QUICK_ICON, 15, 15)
                        .build();
                button.setPosition(6, 6);
                button.setTooltip(Tooltip.create(Component.translatable("qrp.packscreen.tooltip")));
                Screens.getWidgets(screen).add(button);
            }
        });
    }

    private static boolean isResourcePackScreen(Screen screen) {
        // the same class shows data packs too; only the title tells them apart
        return screen.getTitle().getContents() instanceof TranslatableContents contents
                && contents.getKey().equals("resourcePack.title");
    }
}
