package com.emqnuele.quickresourcepack.ui;

import com.emqnuele.quickresourcepack.compat.ScreenCompat;
import com.emqnuele.quickresourcepack.config.ConfigStore;
import com.emqnuele.quickresourcepack.core.config.QrpConfig;
import com.emqnuele.quickresourcepack.core.config.Rule;
import com.emqnuele.quickresourcepack.core.config.Slot;
import com.emqnuele.quickresourcepack.core.logic.RuleEngine;
import com.emqnuele.quickresourcepack.pack.SlotNames;
import com.emqnuele.quickresourcepack.rules.RuleWatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Locale;

public final class RuleEditScreen extends Screen {
    private static final int WIDTH = 310;

    private final Screen parent;
    private final boolean isNew;
    private final Rule draft;
    private @Nullable EditBox patternBox;
    private @Nullable Button saveButton;
    private @Nullable Button useCurrent;
    private int hintY;

    public RuleEditScreen(Screen parent, @Nullable Rule rule) {
        super(Component.translatable(rule == null ? "qrp.rule.title_new" : "qrp.rule.title"));
        this.parent = parent;
        this.isNew = rule == null;
        this.draft = rule == null ? new Rule() : rule.copy();
        QrpConfig config = ConfigStore.get();
        if (draft.slotId.isBlank() && !config.slots.isEmpty()) {
            draft.slotId = config.slots.getFirst().id;
        }
    }

    @Override
    protected void init() {
        QrpConfig config = ConfigStore.get();
        int left = width / 2 - WIDTH / 2;
        int y = Math.max(28, height / 2 - 70);

        addRenderableWidget(CycleButton.builder(
                        (Rule.Trigger t) -> Component.translatable("qrp.rule.trigger." + t.name().toLowerCase(Locale.ROOT)), draft.trigger)
                .withValues(Rule.Trigger.values())
                .create(left, y, WIDTH, 20, Component.translatable("qrp.rule.trigger"), (b, trigger) -> {
                    draft.trigger = trigger;
                    rebuildWidgets();
                }));

        y += 26;
        patternBox = new EditBox(font, left, y, WIDTH - 84, 20, Component.translatable("qrp.rule.pattern"));
        patternBox.setMaxLength(128);
        patternBox.setValue(draft.pattern);
        patternBox.setHint(Component.translatable("qrp.rule.pattern_hint." + triggerKey()).withStyle(ChatFormatting.DARK_GRAY));
        patternBox.setResponder(value -> draft.pattern = value.strip());
        addRenderableWidget(patternBox);
        useCurrent = addRenderableWidget(Button.builder(Component.translatable("qrp.rule.use_current"), b -> fillCurrent())
                .bounds(left + WIDTH - 80, y, 80, 20).build());
        String current = currentValue();
        useCurrent.active = current != null;
        useCurrent.setTooltip(Tooltip.create(current != null
                ? Component.literal(current)
                : Component.translatable("qrp.rule.use_current.none")));

        y += 24;
        hintY = y;
        y += 24;

        if (!config.slots.isEmpty()) {
            Slot initial = config.slot(draft.slotId).orElse(config.slots.getFirst());
            draft.slotId = initial.id;
            addRenderableWidget(CycleButton.builder(SlotNames::of, initial)
                    .withValues(config.slots)
                    .create(left, y, WIDTH, 20, Component.translatable("qrp.rule.slot"), (b, slot) -> draft.slotId = slot.id));
        }

        int footerY = height - 26;
        int buttonWidth = (WIDTH - 16) / 3;
        Button delete = addRenderableWidget(Button.builder(Component.translatable("qrp.edit.delete"), b -> delete())
                .bounds(left, footerY, buttonWidth, 20).build());
        delete.active = !isNew;
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> onClose())
                .bounds(left + buttonWidth + 8, footerY, buttonWidth, 20).build());
        saveButton = addRenderableWidget(Button.builder(Component.translatable("qrp.edit.save"), b -> save())
                .bounds(left + (buttonWidth + 8) * 2, footerY, buttonWidth, 20).build());
    }

    private String triggerKey() {
        return draft.trigger.name().toLowerCase(Locale.ROOT);
    }

    private @Nullable String currentValue() {
        if (minecraft.level == null) {
            return null;
        }
        RuleEngine.Context context = RuleWatcher.currentContext(minecraft);
        return switch (draft.trigger) {
            case SERVER -> context.server();
            case WORLD -> context.world();
            case DIMENSION -> context.dimension();
        };
    }

    private void fillCurrent() {
        String value = currentValue();
        if (value != null && patternBox != null) {
            patternBox.setValue(value);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        if (saveButton != null) {
            saveButton.active = draft.isComplete();
        }
        super.extractRenderState(g, mouseX, mouseY, a);
        g.centeredText(font, title, width / 2, 15, Paint.WHITE);
        int left = width / 2 - WIDTH / 2;
        List<FormattedCharSequence> lines = font.split(Component.translatable("qrp.rule.help." + triggerKey()), WIDTH);
        for (int i = 0; i < Math.min(2, lines.size()); i++) {
            g.text(font, lines.get(i), left, hintY + i * 10, Paint.GRAY, true);
        }
    }

    private void save() {
        if (!draft.isComplete()) {
            return;
        }
        List<Rule> rules = ConfigStore.get().rules;
        boolean replaced = false;
        for (int i = 0; i < rules.size(); i++) {
            if (rules.get(i).id.equals(draft.id)) {
                rules.set(i, draft);
                replaced = true;
            }
        }
        if (!replaced) {
            rules.add(draft);
        }
        ConfigStore.save();
        RuleWatcher.reevaluate();
        onClose();
    }

    private void delete() {
        ConfigStore.get().rules.removeIf(r -> r.id.equals(draft.id));
        ConfigStore.save();
        RuleWatcher.reevaluate();
        onClose();
    }

    @Override
    public void onClose() {
        ScreenCompat.setScreen(minecraft, parent);
    }
}
