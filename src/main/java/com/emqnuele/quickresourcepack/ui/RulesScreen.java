package com.emqnuele.quickresourcepack.ui;

import com.emqnuele.quickresourcepack.compat.ScreenCompat;
import com.emqnuele.quickresourcepack.config.ConfigStore;
import com.emqnuele.quickresourcepack.core.config.QrpConfig;
import com.emqnuele.quickresourcepack.core.config.Rule;
import com.emqnuele.quickresourcepack.pack.SlotNames;
import com.emqnuele.quickresourcepack.rules.RuleWatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Locale;

public final class RulesScreen extends Screen {
    private final Screen parent;
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this, 33 + 12, 33);
    private @Nullable RuleList list;

    public RulesScreen(Screen parent) {
        super(Component.translatable("qrp.rules.title"));
        this.parent = parent;
    }

    static Component describe(Rule rule) {
        String trigger = rule.trigger.name().toLowerCase(Locale.ROOT);
        return Component.translatable("qrp.rules.when." + trigger, rule.pattern);
    }

    @Override
    protected void init() {
        LinearLayout header = layout.addToHeader(LinearLayout.vertical().spacing(4));
        header.defaultCellSetting().alignHorizontallyCenter();
        header.addChild(new StringWidget(title, font));
        header.addChild(new StringWidget(
                Component.translatable("qrp.rules.subtitle").withColor(Paint.GRAY), font));
        list = layout.addToContents(new RuleList(minecraft, width, layout.getContentHeight(), layout.getHeaderHeight()));

        QrpConfig config = ConfigStore.get();
        LinearLayout footer = layout.addToFooter(LinearLayout.horizontal().spacing(8));
        footer.addChild(CycleButton.onOffBuilder(config.behavior.rules)
                .withTooltip(on -> Tooltip.create(Component.translatable("qrp.rules.enabled.tooltip")))
                .create(0, 0, 100, 20, Component.translatable("qrp.rules.enabled"), (b, on) -> {
                    config.behavior.rules = on;
                    ConfigStore.save();
                    RuleWatcher.reevaluate();
                }));
        Button add = footer.addChild(Button.builder(Component.translatable("qrp.rules.add"),
                b -> ScreenCompat.setScreen(minecraft, new RuleEditScreen(this, null))).width(100).build());
        if (config.slots.isEmpty()) {
            add.active = false;
            add.setTooltip(Tooltip.create(Component.translatable("qrp.rules.need_slot")));
        }
        footer.addChild(Button.builder(CommonComponents.GUI_DONE, b -> onClose()).width(100).build());
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

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        if (list != null && list.children().isEmpty()) {
            int cy = layout.getHeaderHeight() + layout.getContentHeight() / 2;
            g.centeredText(font, Component.translatable("qrp.rules.empty.title"), width / 2, cy - 10, Paint.WHITE);
            g.centeredText(font, Component.translatable("qrp.rules.empty.body"), width / 2, cy + 2, Paint.GRAY);
        }
    }

    @Override
    public void onClose() {
        ScreenCompat.setScreen(minecraft, parent);
    }

    private final class RuleList extends ContainerObjectSelectionList<RuleList.Entry> {
        RuleList(Minecraft minecraft, int width, int height, int y) {
            super(minecraft, width, height, y, 24);
            for (Rule rule : ConfigStore.get().rules) {
                addEntry(new Entry(rule));
            }
        }

        @Override
        public int getRowWidth() {
            return 340;
        }

        private final class Entry extends ContainerObjectSelectionList.Entry<Entry> {
            private final Rule rule;
            private final CycleButton<Boolean> toggle;
            private final Button edit;

            Entry(Rule rule) {
                this.rule = rule;
                this.toggle = CycleButton.onOffBuilder(rule.enabled).displayOnlyValue()
                        .create(0, 0, 44, 20, describe(rule), (b, on) -> {
                            rule.enabled = on;
                            ConfigStore.save();
                            RuleWatcher.reevaluate();
                        });
                this.edit = Button.builder(Component.translatable("qrp.hub.edit"),
                        b -> ScreenCompat.setScreen(minecraft, new RuleEditScreen(RulesScreen.this, rule))).size(50, 20).build();
            }

            @Override
            public void extractContent(GuiGraphicsExtractor g, int mouseX, int mouseY, boolean hovered, float a) {
                edit.setPosition(scrollBarX() - edit.getWidth() - 10, getContentYMiddle() - 10);
                toggle.setPosition(edit.getX() - 5 - toggle.getWidth(), getContentYMiddle() - 10);
                int x = getContentX();
                int maxWidth = toggle.getX() - 6 - x;
                int color = rule.enabled ? Paint.WHITE : Paint.DARK_GRAY;
                g.text(font, Paint.ellipsize(font, describe(rule).getString(), maxWidth), x, getContentY() + 1, color, true);
                Component target = ConfigStore.get().slot(rule.slotId)
                        .map(slot -> Component.translatable("qrp.rules.turns_on", SlotNames.of(slot)))
                        .orElse(Component.translatable("qrp.rules.no_slot"));
                g.text(font, Paint.ellipsize(font, target.getString(), maxWidth), x, getContentY() + 11, Paint.DARK_GRAY, true);
                toggle.extractRenderState(g, mouseX, mouseY, a);
                edit.extractRenderState(g, mouseX, mouseY, a);
            }

            @Override
            public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
                if (super.mouseClicked(event, doubleClick)) {
                    return true;
                }
                ScreenCompat.setScreen(minecraft, new RuleEditScreen(RulesScreen.this, rule));
                return true;
            }

            @Override
            public List<? extends GuiEventListener> children() {
                return List.of(toggle, edit);
            }

            @Override
            public List<? extends NarratableEntry> narratables() {
                return List.of(toggle, edit);
            }
        }
    }
}
