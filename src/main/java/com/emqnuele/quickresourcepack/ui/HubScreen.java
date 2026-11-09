package com.emqnuele.quickresourcepack.ui;

import com.emqnuele.quickresourcepack.compat.ScreenCompat;
import com.emqnuele.quickresourcepack.config.ConfigStore;
import com.emqnuele.quickresourcepack.core.config.Slot;
import com.emqnuele.quickresourcepack.hud.ActivePacksHud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

public final class HubScreen extends Screen {
    private final @Nullable Screen parent;
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    final KeyCapture capture = new KeyCapture();
    private @Nullable SlotList list;

    public HubScreen(@Nullable Screen parent) {
        super(Component.translatable("qrp.hub.title"));
        this.parent = parent;
    }

    public static void open(@Nullable Screen parent) {
        ScreenCompat.setScreen(Minecraft.getInstance(), new HubScreen(parent));
    }

    @Override
    protected void init() {
        layout.addTitleHeader(title, font);
        list = layout.addToContents(new SlotList(minecraft, this, width, layout.getContentHeight(), layout.getHeaderHeight()));
        int buttonWidth = Math.clamp((width - 40 - 3 * 8) / 4, 60, 100);
        LinearLayout footer = layout.addToFooter(LinearLayout.horizontal().spacing(8));
        footer.addChild(Button.builder(Component.translatable("qrp.hub.add_slot"), b -> edit(null)).width(buttonWidth).build());
        footer.addChild(Button.builder(Component.translatable("qrp.hub.rules"),
                b -> ScreenCompat.setScreen(minecraft, new RulesScreen(this))).width(buttonWidth).build());
        footer.addChild(Button.builder(Component.translatable("qrp.hub.options"),
                b -> ScreenCompat.setScreen(minecraft, new QrpOptionsScreen(this))).width(buttonWidth).build());
        footer.addChild(Button.builder(CommonComponents.GUI_DONE, b -> onClose()).width(buttonWidth).build());
        layout.visitWidgets(this::addRenderableWidget);
        repositionElements();
    }

    @Override
    protected void repositionElements() {
        layout.arrangeElements();
        if (list != null) {
            list.updateSize(width, layout);
        }
    }

    void edit(@Nullable Slot slot) {
        capture.cancel();
        ScreenCompat.setScreen(minecraft, new SlotEditScreen(this, slot));
    }

    void bind(Slot slot) {
        capture.start(slot.id, combo -> {
            ConfigStore.get().slot(slot.id).ifPresent(s -> s.key = combo);
            ConfigStore.save();
        });
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return capture.keyPressed(event) || super.keyPressed(event);
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        return capture.keyReleased(event) || super.keyReleased(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (capture.isActive()) {
            capture.mouseClicked(event);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        if (list != null && list.children().isEmpty()) {
            int cy = layout.getHeaderHeight() + layout.getContentHeight() / 2;
            g.centeredText(font, Component.translatable("qrp.hub.empty.title"), width / 2, cy - 10, Paint.WHITE);
            g.centeredText(font, Component.translatable("qrp.hub.empty.body"), width / 2, cy + 2, Paint.GRAY);
        }
    }

    @Override
    public void onClose() {
        ActivePacksHud.invalidate();
        ScreenCompat.setScreen(minecraft, parent);
    }
}
