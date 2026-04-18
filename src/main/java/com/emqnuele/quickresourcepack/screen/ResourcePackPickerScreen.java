package com.emqnuele.quickresourcepack.screen;

import com.emqnuele.quickresourcepack.config.ModConfig;
import com.emqnuele.quickresourcepack.handler.NotificationHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.resource.ResourcePackSource;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class ResourcePackPickerScreen extends Screen {

    private static final int LIST_TOP = 32;
    private static final int ITEM_H = 18;

    private final Screen parent;
    private final List<String[]> entries = new ArrayList<>();
    private int selectedIndex = 0;
    private int scrollOffset = 0;

    public ResourcePackPickerScreen(Screen parent) {
        super(Text.translatable("screen.quickresourcepack.picker"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        loadEntries();

        addDrawableChild(ButtonWidget.builder(
                Text.translatable("screen.quickresourcepack.picker.set"),
                button -> {
                    String[] entry = entries.get(selectedIndex);
                    String id = entry[0];
                    String name = entry[1];
                    ModConfig.getInstance().selectedResourcePack = id != null ? id : "";
                    ModConfig.save();
                    if (id != null) {
                        NotificationHandler.notify(Text.translatable("notification.quickresourcepack.set", name));
                    } else {
                        NotificationHandler.notify(Text.translatable("notification.quickresourcepack.nopack"));
                    }
                    client.setScreen(parent);
                })
                .dimensions(width / 2 - 154, height - 27, 150, 20)
                .build());

        addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.cancel"),
                button -> client.setScreen(parent))
                .dimensions(width / 2 + 4, height - 27, 150, 20)
                .build());
    }

    private void loadEntries() {
        entries.clear();
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.getResourcePackManager().scanPacks();
        String currentId = ModConfig.getInstance().selectedResourcePack;

        entries.add(new String[]{null, Text.translatable("screen.quickresourcepack.picker.none").getString()});
        if (currentId == null || currentId.isEmpty()) selectedIndex = 0;

        for (ResourcePackProfile profile : mc.getResourcePackManager().getProfiles()) {
            if (!profile.isPinned() && profile.getSource() == ResourcePackSource.NONE) {
                entries.add(new String[]{profile.getId(), profile.getDisplayName().getString()});
                if (profile.getId().equals(currentId)) selectedIndex = entries.size() - 1;
            }
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int listBottom = height - 36;
        int itemX0 = width / 2 - 130;
        int itemX1 = width / 2 + 130;
        int visible = (listBottom - LIST_TOP) / ITEM_H;

        for (int i = 0; i < visible && (i + scrollOffset) < entries.size(); i++) {
            int idx = i + scrollOffset;
            int y = LIST_TOP + i * ITEM_H;
            boolean sel = idx == selectedIndex;
            boolean hov = mouseX >= itemX0 && mouseX <= itemX1 && mouseY >= y && mouseY < y + ITEM_H;

            if (sel) {
                context.fill(itemX0, y, itemX1, y + ITEM_H, 0xFF4A90D9);
            } else if (hov) {
                context.fill(itemX0, y, itemX1, y + ITEM_H, 0xFF3A3A3A);
            }

            context.drawTextWithShadow(textRenderer, entries.get(idx)[1], itemX0 + 4, y + (ITEM_H - 8) / 2, sel ? 0xFFFFFFFF : 0xFFCCCCCC);
        }

        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 14, 0xFFFFFFFF);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int listBottom = height - 36;
        int itemX0 = width / 2 - 130;
        int itemX1 = width / 2 + 130;
        int visible = (listBottom - LIST_TOP) / ITEM_H;

        for (int i = 0; i < visible; i++) {
            int y = LIST_TOP + i * ITEM_H;
            if (mouseX >= itemX0 && mouseX <= itemX1 && mouseY >= y && mouseY < y + ITEM_H) {
                int idx = i + scrollOffset;
                if (idx < entries.size()) {
                    selectedIndex = idx;
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        int listBottom = height - 36;
        int visible = (listBottom - LIST_TOP) / ITEM_H;
        int maxScroll = Math.max(0, entries.size() - visible);
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) vertical));
        return true;
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
