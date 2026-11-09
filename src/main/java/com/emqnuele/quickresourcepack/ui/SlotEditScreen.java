package com.emqnuele.quickresourcepack.ui;

import com.emqnuele.quickresourcepack.compat.ScreenCompat;
import com.emqnuele.quickresourcepack.config.ConfigStore;
import com.emqnuele.quickresourcepack.core.config.QrpConfig;
import com.emqnuele.quickresourcepack.core.config.Slot;
import com.emqnuele.quickresourcepack.core.config.SlotMode;
import com.emqnuele.quickresourcepack.hud.ActivePacksHud;
import com.emqnuele.quickresourcepack.pack.PackIndex;
import com.emqnuele.quickresourcepack.pack.SlotNames;
import com.emqnuele.quickresourcepack.rules.RuleWatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class SlotEditScreen extends Screen {
    private static final int COLUMN = 200;
    private static final int GAP = 10;
    private static final int TOP = 22;

    private final HubScreen parent;
    private final boolean isNew;
    private final Slot draft;
    private final KeyCapture capture = new KeyCapture();
    private boolean primary;
    private String search = "";

    private @Nullable PackList available;
    private @Nullable PackList chosen;
    private @Nullable EditBox nameBox;
    private @Nullable Button keyButton;
    private @Nullable Button saveButton;

    public SlotEditScreen(HubScreen parent, @Nullable Slot slot) {
        super(Component.translatable(slot == null ? "qrp.edit.title_new" : "qrp.edit.title"));
        this.parent = parent;
        this.isNew = slot == null;
        this.draft = slot == null ? new Slot() : slot.copy();
        QrpConfig config = ConfigStore.get();
        this.primary = isNew ? config.slots.isEmpty() : config.primarySlot().map(s -> s.id.equals(draft.id)).orElse(false);
    }

    private int left() {
        return width / 2 - COLUMN - GAP / 2;
    }

    private int right() {
        return width / 2 + GAP / 2;
    }

    @Override
    protected void init() {
        int left = left();
        int right = right();
        int y = TOP;

        String name = nameBox != null ? nameBox.getValue() : draft.name;
        nameBox = new EditBox(font, left, y, COLUMN, 20, Component.translatable("qrp.edit.name"));
        nameBox.setMaxLength(48);
        nameBox.setValue(name);
        nameBox.setHint(Component.translatable("qrp.edit.name_hint").withStyle(ChatFormatting.DARK_GRAY));
        nameBox.setResponder(value -> draft.name = value.strip());
        addRenderableWidget(nameBox);

        addRenderableWidget(CycleButton.builder(Labels::mode, draft.mode)
                .withValues(SlotMode.values())
                .withTooltip(mode -> Tooltip.create(Labels.modeTooltip(mode)))
                .create(right, y, COLUMN, 20, Component.translatable("qrp.edit.mode"), (b, mode) -> {
                    draft.mode = mode;
                    rebuildWidgets();
                }));

        y += 24;
        keyButton = addRenderableWidget(Button.builder(Component.empty(), b -> capture.start(draft, combo -> draft.key = combo))
                .bounds(left, y, COLUMN, 20)
                .build());
        addRenderableWidget(CycleButton.onOffBuilder(primary)
                .withTooltip(on -> Tooltip.create(Component.translatable("qrp.edit.primary.tooltip")))
                .create(right, y, COLUMN, 20, Component.translatable("qrp.edit.primary"), (b, on) -> primary = on));

        y += 24;
        int half = (COLUMN - 4) / 2;
        addRenderableWidget(CycleButton.onOffBuilder(draft.showInWheel)
                .withTooltip(on -> Tooltip.create(Component.translatable("qrp.edit.wheel.tooltip")))
                .create(left, y, half, 20, Component.translatable("qrp.edit.wheel"), (b, on) -> draft.showInWheel = on));
        addRenderableWidget(CycleButton.onOffBuilder(draft.applyOnStartup)
                .withTooltip(on -> Tooltip.create(Component.translatable("qrp.edit.startup.tooltip")))
                .create(left + half + 4, y, half, 20, Component.translatable("qrp.edit.startup"), (b, on) -> draft.applyOnStartup = on));
        switch (draft.mode) {
            case TOGGLE -> addRenderableWidget(CycleButton.builder(
                            (Slot.Insert insert) -> Component.translatable("qrp.edit.insert." + insert.name().toLowerCase(Locale.ROOT)),
                            draft.insert)
                    .withValues(Slot.Insert.values())
                    .withTooltip(insert -> Tooltip.create(Component.translatable("qrp.edit.insert.tooltip")))
                    .create(right, y, COLUMN, 20, Component.translatable("qrp.edit.insert"), (b, insert) -> draft.insert = insert));
            case CYCLE -> addRenderableWidget(CycleButton.onOffBuilder(draft.cycleIncludesOff)
                    .withTooltip(on -> Tooltip.create(Component.translatable("qrp.edit.cycle_off.tooltip")))
                    .create(right, y, COLUMN, 20, Component.translatable("qrp.edit.cycle_off"), (b, on) -> draft.cycleIncludesOff = on));
            case PROFILE -> {
            }
        }

        y += 26;
        EditBox searchBox = new EditBox(font, left, y, COLUMN, 15, Component.translatable("gui.recipebook.search_hint"));
        searchBox.setValue(search);
        searchBox.setHint(Component.translatable("gui.recipebook.search_hint").withStyle(EditBox.SEARCH_HINT_STYLE));
        searchBox.setResponder(value -> {
            search = value;
            refreshLists("");
        });
        addRenderableWidget(searchBox);

        int listTop = y + 19;
        int listHeight = Math.max(40, height - listTop - 32);
        available = new PackList(minecraft, COLUMN, listHeight, Component.translatable("pack.available.title"), new AvailableActions());
        chosen = new PackList(minecraft, COLUMN, listHeight, Component.translatable("qrp.edit.chosen." + draft.mode.name().toLowerCase(Locale.ROOT)), new ChosenActions());
        available.updateSizeAndPosition(COLUMN, listHeight, left, listTop);
        chosen.updateSizeAndPosition(COLUMN, listHeight, right, listTop);
        addRenderableWidget(available);
        addRenderableWidget(chosen);
        refreshLists("");

        int footerY = height - 26;
        int buttonWidth = (COLUMN * 2 + GAP - 16) / 3;
        Button delete = addRenderableWidget(Button.builder(Component.translatable("qrp.edit.delete"), b -> confirmDelete())
                .bounds(left, footerY, buttonWidth, 20).build());
        delete.active = !isNew;
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> onClose())
                .bounds(left + buttonWidth + 8, footerY, buttonWidth, 20).build());
        saveButton = addRenderableWidget(Button.builder(Component.translatable("qrp.edit.save"), b -> save())
                .bounds(left + (buttonWidth + 8) * 2, footerY, buttonWidth, 20).build());
    }

    private void refreshLists(String focusPackId) {
        if (available == null || chosen == null) {
            return;
        }
        String query = search.toLowerCase(Locale.ROOT).strip();
        Set<String> inSlot = new HashSet<>(draft.packs);
        List<PackList.Row> availableRows = new ArrayList<>();
        for (PackIndex.Entry entry : PackIndex.selectable()) {
            if (inSlot.contains(entry.id())) {
                continue;
            }
            if (!query.isEmpty() && !entry.title().getString().toLowerCase(Locale.ROOT).contains(query)
                    && !entry.id().toLowerCase(Locale.ROOT).contains(query)) {
                continue;
            }
            availableRows.add(new PackList.Row(entry.id(), entry.title(), entry.description(), !entry.compatible()));
        }
        List<PackList.Row> chosenRows = new ArrayList<>();
        for (String id : draft.packs) {
            if (PackIndex.exists(id)) {
                chosenRows.add(new PackList.Row(id, PackIndex.title(id), PackIndex.description(id), false));
            } else {
                chosenRows.add(new PackList.Row(id, PackIndex.title(id), Component.translatable("qrp.edit.missing"), true));
            }
        }
        available.setRows(availableRows, focusPackId);
        chosen.setRows(chosenRows, focusPackId);
    }

    private final class AvailableActions implements PackList.Actions {
        @Override
        public boolean canAdd(PackList.Row row) {
            return true;
        }

        @Override
        public void add(PackList.Row row) {
            // newest goes on top, as in the vanilla pack screen
            draft.packs.addFirst(row.packId());
            refreshLists(row.packId());
        }
    }

    private final class ChosenActions implements PackList.Actions {
        @Override
        public boolean canRemove(PackList.Row row) {
            return true;
        }

        @Override
        public boolean canMoveUp(PackList.Row row) {
            return draft.packs.indexOf(row.packId()) > 0;
        }

        @Override
        public boolean canMoveDown(PackList.Row row) {
            int index = draft.packs.indexOf(row.packId());
            return index >= 0 && index < draft.packs.size() - 1;
        }

        @Override
        public void remove(PackList.Row row) {
            draft.packs.remove(row.packId());
            refreshLists(row.packId());
        }

        @Override
        public void moveUp(PackList.Row row) {
            move(row.packId(), -1);
        }

        @Override
        public void moveDown(PackList.Row row) {
            move(row.packId(), 1);
        }

        private void move(String id, int by) {
            int index = draft.packs.indexOf(id);
            int target = index + by;
            if (index < 0 || target < 0 || target >= draft.packs.size()) {
                return;
            }
            draft.packs.remove(index);
            draft.packs.add(target, id);
            refreshLists(id);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        if (keyButton != null) {
            List<Component> clashes = Labels.conflicts(draft.key, draft.id);
            Component label = Labels.keyButton(draft.key, capture.isActiveFor(draft), !clashes.isEmpty());
            keyButton.setMessage(Component.translatable("qrp.edit.key", label));
            Tooltip clash = Labels.clashTooltip(clashes);
            keyButton.setTooltip(clash != null ? clash : Tooltip.create(Component.translatable("qrp.edit.key.tooltip")));
        }
        if (saveButton != null) {
            saveButton.active = draft.hasPacks();
            saveButton.setTooltip(draft.hasPacks() ? null : Tooltip.create(Component.translatable("qrp.edit.save.empty")));
        }
        super.extractRenderState(g, mouseX, mouseY, a);
        g.centeredText(font, title, width / 2, 8, Paint.WHITE);
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
            return capture.mouseClicked(event);
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void save() {
        if (!draft.hasPacks()) {
            return;
        }
        QrpConfig config = ConfigStore.get();
        int index = -1;
        for (int i = 0; i < config.slots.size(); i++) {
            if (config.slots.get(i).id.equals(draft.id)) {
                index = i;
            }
        }
        if (index >= 0) {
            config.slots.set(index, draft);
        } else {
            config.slots.add(draft);
        }
        if (primary) {
            config.behavior.primarySlotId = draft.id;
        } else if (config.behavior.primarySlotId.equals(draft.id)) {
            config.behavior.primarySlotId = "";
        }
        ConfigStore.save();
        ActivePacksHud.invalidate();
        RuleWatcher.reevaluate();
        onClose();
    }

    private void confirmDelete() {
        ScreenCompat.setScreen(minecraft, new ConfirmScreen(confirmed -> {
            if (confirmed) {
                ConfigStore.get().removeSlot(draft.id);
                ConfigStore.save();
                ActivePacksHud.invalidate();
                ScreenCompat.setScreen(minecraft, parent);
            } else {
                ScreenCompat.setScreen(minecraft, this);
            }
        }, Component.translatable("qrp.edit.delete.title"), Component.translatable("qrp.edit.delete.body",
                SlotNames.of(draft))));
    }

    @Override
    public void onClose() {
        ScreenCompat.setScreen(minecraft, parent);
    }
}
