package com.emqnuele.quickresourcepack.ui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

final class PackList extends ObjectSelectionList<PackList.Entry> {
    private static final Identifier SELECT = Identifier.withDefaultNamespace("transferable_list/select");
    private static final Identifier SELECT_HOT = Identifier.withDefaultNamespace("transferable_list/select_highlighted");
    private static final Identifier UNSELECT = Identifier.withDefaultNamespace("transferable_list/unselect");
    private static final Identifier UNSELECT_HOT = Identifier.withDefaultNamespace("transferable_list/unselect_highlighted");
    private static final Identifier MOVE_UP = Identifier.withDefaultNamespace("transferable_list/move_up");
    private static final Identifier MOVE_UP_HOT = Identifier.withDefaultNamespace("transferable_list/move_up_highlighted");
    private static final Identifier MOVE_DOWN = Identifier.withDefaultNamespace("transferable_list/move_down");
    private static final Identifier MOVE_DOWN_HOT = Identifier.withDefaultNamespace("transferable_list/move_down_highlighted");
    private static final int ICON = 32;
    private static final int TEXT_WIDTH = 157;

    interface Actions {
        default boolean canAdd(Row row) {
            return false;
        }

        default boolean canRemove(Row row) {
            return false;
        }

        default boolean canMoveUp(Row row) {
            return false;
        }

        default boolean canMoveDown(Row row) {
            return false;
        }

        default void add(Row row) {
        }

        default void remove(Row row) {
        }

        default void moveUp(Row row) {
        }

        default void moveDown(Row row) {
        }
    }

    record Row(String packId, Component title, Component description, boolean broken) {
    }

    private final Component title;
    private final Actions actions;

    PackList(Minecraft minecraft, int width, int height, Component title, Actions actions) {
        super(minecraft, width, height, 0, 36);
        this.title = title;
        this.actions = actions;
        this.centerListVertically = false;
    }

    void setRows(List<Row> rows, String focusPackId) {
        clearEntries();
        Component header = Component.empty().append(title).withStyle(ChatFormatting.UNDERLINE, ChatFormatting.BOLD);
        addEntry(new Header(minecraft.font, header), (int) (9 * 1.5f));
        setSelected(null);
        for (Row row : rows) {
            PackEntry entry = new PackEntry(row);
            addEntry(entry);
            if (row.packId().equals(focusPackId)) {
                setFocused(entry);
            }
        }
        refreshScrollAmount();
    }

    @Override
    public int getRowWidth() {
        return width - 4;
    }

    @Override
    protected int scrollBarX() {
        return getRight() - scrollbarWidth();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return getSelected() != null ? getSelected().keyPressed(event) : super.keyPressed(event);
    }

    abstract class Entry extends ObjectSelectionList.Entry<Entry> {
        @Override
        public int getWidth() {
            return super.getWidth() - (scrollable() ? scrollbarWidth() : 0);
        }
    }

    final class Header extends Entry {
        private final Font font;
        private final Component text;

        Header(Font font, Component text) {
            this.font = font;
            this.text = text;
        }

        @Override
        public void extractContent(GuiGraphicsExtractor g, int mouseX, int mouseY, boolean hovered, float a) {
            g.centeredText(font, text, getX() + getWidth() / 2, getContentYMiddle() - 9 / 2, Paint.WHITE);
        }

        @Override
        public Component getNarration() {
            return text;
        }
    }

    final class PackEntry extends Entry {
        private final Row row;

        PackEntry(Row row) {
            this.row = row;
        }

        @Override
        public Component getNarration() {
            return Component.translatable("narrator.select", row.title());
        }

        @Override
        public void extractContent(GuiGraphicsExtractor g, int mouseX, int mouseY, boolean hovered, float a) {
            int x = getContentX();
            int y = getContentY();
            if (row.broken()) {
                g.fill(x - 1, y - 1, getContentRight() + 1, getContentBottom() + 1, 0xFF770000);
            }
            Paint.packIcon(g, row.packId(), x, y, ICON, 1f);
            if (hovered || getSelected() == this && isFocused()) {
                drawOverlay(g, mouseX - x, mouseY - y);
            }
            Font font = minecraft.font;
            int textWidth = TEXT_WIDTH - (scrollable() ? 6 : 0);
            g.text(font, Paint.ellipsize(font, row.title().getString(), textWidth), x + ICON + 2, y + 1, Paint.WHITE, true);
            List<FormattedCharSequence> lines = font.split(row.description(), textWidth);
            for (int i = 0; i < Math.min(2, lines.size()); i++) {
                g.text(font, lines.get(i), x + ICON + 2, y + 12 + i * 9, Paint.DARK_GRAY, true);
            }
        }

        private void drawOverlay(GuiGraphicsExtractor g, int relX, int relY) {
            int x = getContentX();
            int y = getContentY();
            g.fill(x, y, x + ICON, y + ICON, 0xA0909090);
            if (actions.canAdd(row)) {
                g.blitSprite(RenderPipelines.GUI_TEXTURED, overIcon(relX, relY) ? SELECT_HOT : SELECT, x, y, ICON, ICON);
                return;
            }
            if (actions.canRemove(row)) {
                g.blitSprite(RenderPipelines.GUI_TEXTURED, overLeftHalf(relX, relY) ? UNSELECT_HOT : UNSELECT, x, y, ICON, ICON);
            }
            if (actions.canMoveUp(row)) {
                g.blitSprite(RenderPipelines.GUI_TEXTURED, overTopRight(relX, relY) ? MOVE_UP_HOT : MOVE_UP, x, y, ICON, ICON);
            }
            if (actions.canMoveDown(row)) {
                g.blitSprite(RenderPipelines.GUI_TEXTURED, overBottomRight(relX, relY) ? MOVE_DOWN_HOT : MOVE_DOWN, x, y, ICON, ICON);
            }
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            int relX = (int) event.x() - getContentX();
            int relY = (int) event.y() - getContentY();
            if (actions.canAdd(row) && overIcon(relX, relY)) {
                actions.add(row);
                return true;
            }
            if (actions.canRemove(row) && overLeftHalf(relX, relY)) {
                actions.remove(row);
                return true;
            }
            if (actions.canMoveUp(row) && overTopRight(relX, relY)) {
                actions.moveUp(row);
                return true;
            }
            if (actions.canMoveDown(row) && overBottomRight(relX, relY)) {
                actions.moveDown(row);
                return true;
            }
            return super.mouseClicked(event, doubleClick);
        }

        @Override
        public boolean keyPressed(KeyEvent event) {
            if (event.isConfirmation()) {
                if (actions.canAdd(row)) {
                    actions.add(row);
                } else if (actions.canRemove(row)) {
                    actions.remove(row);
                }
                return true;
            }
            if (event.hasShiftDown() && event.isUp() && actions.canMoveUp(row)) {
                actions.moveUp(row);
                return true;
            }
            if (event.hasShiftDown() && event.isDown() && actions.canMoveDown(row)) {
                actions.moveDown(row);
                return true;
            }
            return super.keyPressed(event);
        }

        private static boolean overIcon(int relX, int relY) {
            return relX >= 0 && relY >= 0 && relX < ICON && relY < ICON;
        }

        private static boolean overLeftHalf(int relX, int relY) {
            return relX >= 0 && relY >= 0 && relX < ICON / 2 && relY < ICON;
        }

        private static boolean overTopRight(int relX, int relY) {
            return relX >= ICON / 2 && relY >= 0 && relX < ICON && relY < ICON / 2;
        }

        private static boolean overBottomRight(int relX, int relY) {
            return relX >= ICON / 2 && relY >= ICON / 2 && relX < ICON && relY < ICON;
        }
    }
}
