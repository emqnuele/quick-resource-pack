package com.emqnuele.quickresourcepack.ui;

import com.emqnuele.quickresourcepack.config.ConfigStore;
import com.emqnuele.quickresourcepack.core.config.QrpConfig;
import com.emqnuele.quickresourcepack.core.config.Slot;
import com.emqnuele.quickresourcepack.core.logic.PackUniverse;
import com.emqnuele.quickresourcepack.core.logic.SelectionPlanner;
import com.emqnuele.quickresourcepack.pack.PackIndex;
import com.emqnuele.quickresourcepack.pack.SlotNames;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;

final class SlotList extends ContainerObjectSelectionList<SlotList.Entry> {
    private static final int ROW_HEIGHT = 36;
    private static final int ICON = 32;

    private final HubScreen screen;

    SlotList(Minecraft minecraft, HubScreen screen, int width, int height, int y) {
        super(minecraft, width, height, y, ROW_HEIGHT);
        this.screen = screen;
        refresh();
    }

    void refresh() {
        clearEntries();
        QrpConfig config = ConfigStore.get();
        PackUniverse universe = PackIndex.universe();
        List<String> selected = PackIndex.selected();
        String primaryId = config.primarySlot().map(s -> s.id).orElse("");
        for (Slot slot : config.slots) {
            boolean active = slot.hasPacks() && SelectionPlanner.status(slot, selected, universe).active();
            long missing = slot.packs.stream().filter(p -> !universe.available().contains(p)).count();
            addEntry(new Entry(slot, active, slot.id.equals(primaryId), (int) missing));
        }
    }

    @Override
    public int getRowWidth() {
        return 340;
    }

    final class Entry extends ContainerObjectSelectionList.Entry<Entry> {
        private final Slot slot;
        private final boolean active;
        private final boolean primary;
        private final int missing;
        private final Button keyButton;
        private final Button editButton;

        private Entry(Slot slot, boolean active, boolean primary, int missing) {
            this.slot = slot;
            this.active = active;
            this.primary = primary;
            this.missing = missing;
            this.keyButton = Button.builder(Labels.keyButton(slot.key, false, false), b -> screen.bind(slot))
                    .size(75, 20)
                    .createNarration(n -> Component.translatable("qrp.narrate.key", SlotNames.of(slot), n.get()))
                    .build();
            this.editButton = Button.builder(Component.translatable("qrp.hub.edit"), b -> screen.edit(slot))
                    .size(50, 20)
                    .createNarration(n -> Component.translatable("qrp.narrate.edit", SlotNames.of(slot)))
                    .build();
        }

        @Override
        public void extractContent(GuiGraphicsExtractor g, int mouseX, int mouseY, boolean hovered, float a) {
            var font = minecraft.font;
            int x = getContentX();
            int y = getContentY();
            String icon = slot.hasPacks() ? slot.packs.getFirst() : "";
            Paint.packIcon(g, icon, x, y, ICON, 1f);
            if (slot.packs.size() > 1) {
                // a stack count says "more than one pack" in the game's own language
                String count = String.valueOf(slot.packs.size());
                g.text(font, count, x + ICON - font.width(count), y + ICON - 8, Paint.WHITE, true);
            }

            // labels follow live state, as in the controls screen, so a rebind never rebuilds the list mid-click
            List<Component> clashes = Labels.conflicts(slot.key, slot.id);
            keyButton.setMessage(Labels.keyButton(slot.key, screen.capture.isActiveFor(slot.id), !clashes.isEmpty()));
            keyButton.setTooltip(Labels.clashTooltip(clashes));
            editButton.setPosition(scrollBarX() - editButton.getWidth() - 10, getContentYMiddle() - 10);
            keyButton.setPosition(editButton.getX() - 5 - keyButton.getWidth(), getContentYMiddle() - 10);
            int textX = x + ICON + 4;
            int maxWidth = keyButton.getX() - 6 - textX;

            g.text(font, Paint.ellipsize(font, SlotNames.plain(slot), maxWidth), textX, y + 1, Paint.WHITE, true);
            Component mode = primary
                    ? Component.translatable("qrp.hub.mode_primary", Labels.mode(slot.mode))
                    : Labels.mode(slot.mode);
            g.text(font, Paint.ellipsize(font, mode.getString(), maxWidth), textX, y + 12, Paint.DARK_GRAY, true);
            Component state;
            int color;
            if (!slot.hasPacks()) {
                state = Component.translatable("qrp.hub.no_packs");
                color = Paint.DARK_GRAY;
            } else if (missing > 0) {
                state = Component.translatable("qrp.hub.missing", missing);
                color = Paint.RED;
            } else {
                state = Component.translatable(active ? "qrp.notify.on" : "qrp.notify.off");
                color = active ? Paint.GREEN : Paint.DARK_GRAY;
            }
            g.text(font, Paint.ellipsize(font, state.getString(), maxWidth), textX, y + 23, color, true);

            keyButton.extractRenderState(g, mouseX, mouseY, a);
            editButton.extractRenderState(g, mouseX, mouseY, a);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (super.mouseClicked(event, doubleClick)) {
                return true;
            }
            screen.edit(slot);
            return true;
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(keyButton, editButton);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(keyButton, editButton);
        }
    }
}
