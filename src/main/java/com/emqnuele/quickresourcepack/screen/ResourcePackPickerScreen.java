package com.emqnuele.quickresourcepack.screen;

import com.emqnuele.quickresourcepack.config.ModConfig;
import com.emqnuele.quickresourcepack.handler.NotificationHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.resource.ResourcePackSource;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class ResourcePackPickerScreen extends Screen {

    private final Screen parent;
    private PackListWidget list;

    public ResourcePackPickerScreen(Screen parent) {
        super(Text.translatable("screen.quickresourcepack.picker"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.list = new PackListWidget(this.client, this.width, this.height - 64, 32, 18);
        this.addSelectableChild(this.list);

        this.addDrawableChild(ButtonWidget.builder(
                        Text.translatable("screen.quickresourcepack.picker.set"),
                        button -> {
                            PackListWidget.PackEntry selected = this.list.getSelectedOrNull();
                            if (selected != null) {
                                ModConfig.getInstance().selectedResourcePack = selected.packId != null ? selected.packId : "";
                                ModConfig.save();
                                if (selected.packId != null) {
                                    NotificationHandler.notify(Text.translatable("notification.quickresourcepack.set", selected.displayName));
                                } else {
                                    NotificationHandler.notify(Text.translatable("notification.quickresourcepack.nopack"));
                                }
                            }
                            this.client.setScreen(this.parent);
                        })
                .dimensions(this.width / 2 - 154, this.height - 27, 150, 20)
                .build());

        this.addDrawableChild(ButtonWidget.builder(
                        Text.translatable("gui.cancel"),
                        button -> this.client.setScreen(this.parent))
                .dimensions(this.width / 2 + 4, this.height - 27, 150, 20)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xBE000000);
        this.list.render(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 14, 0xFFFFFF);
    }

    @Override
    public void close() {
        this.client.setScreen(this.parent);
    }

    class PackListWidget extends AlwaysSelectedEntryListWidget<PackListWidget.PackEntry> {

        PackListWidget(MinecraftClient client, int width, int height, int top, int itemHeight) {
            super(client, width, height, top, itemHeight);
            populate();
        }

        private void populate() {
            MinecraftClient mc = MinecraftClient.getInstance();
            mc.getResourcePackManager().scanPacks();
            String currentId = ModConfig.getInstance().selectedResourcePack;

            PackEntry noneEntry = new PackEntry(null, Text.translatable("screen.quickresourcepack.picker.none").getString());
            this.addEntry(noneEntry);

            List<ResourcePackProfile> profiles = new ArrayList<>();
            for (ResourcePackProfile profile : mc.getResourcePackManager().getProfiles()) {
                if (!profile.isPinned() && profile.getSource() == ResourcePackSource.NONE) {
                    profiles.add(profile);
                }
            }

            PackEntry toSelect = (currentId == null || currentId.isEmpty()) ? noneEntry : null;
            for (ResourcePackProfile profile : profiles) {
                PackEntry entry = new PackEntry(profile.getId(), profile.getDisplayName().getString());
                this.addEntry(entry);
                if (profile.getId().equals(currentId)) {
                    toSelect = entry;
                }
            }

            this.setSelected(toSelect != null ? toSelect : noneEntry);
        }

        @Override
        public int getRowWidth() {
            return Math.min(260, width - 40);
        }

        class PackEntry extends AlwaysSelectedEntryListWidget.Entry<PackEntry> {
            final String packId;
            final String displayName;

            PackEntry(String packId, String displayName) {
                this.packId = packId;
                this.displayName = displayName;
            }

            @Override
            public Text getNarration() {
                return Text.literal(displayName);
            }

            @Override
            public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                boolean selected = PackListWidget.this.getSelectedOrNull() == this;
                int color = selected ? 0xFFFFFF : (packId == null ? 0xAAAAAA : 0xCCCCCC);
                context.drawTextWithShadow(
                        ResourcePackPickerScreen.this.textRenderer,
                        displayName,
                        x + 4,
                        y + (entryHeight - 8) / 2,
                        color
                );
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                PackListWidget.this.setSelected(this);
                return true;
            }
        }
    }
}
