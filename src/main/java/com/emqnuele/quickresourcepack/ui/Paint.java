package com.emqnuele.quickresourcepack.ui;

import com.emqnuele.quickresourcepack.QuickResourcePackMod;
import com.emqnuele.quickresourcepack.config.ConfigStore;
import com.emqnuele.quickresourcepack.pack.PackIcons;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

public final class Paint {
    public static final Identifier HOTBAR = Identifier.withDefaultNamespace("hud/hotbar");
    public static final Identifier HOTBAR_SELECTION = Identifier.withDefaultNamespace("hud/hotbar_selection");
    public static final Identifier EFFECT_BACKGROUND = Identifier.withDefaultNamespace("hud/effect_background");
    public static final Identifier CHECKMARK = Identifier.withDefaultNamespace("icon/checkmark");
    public static final Identifier TOAST = Identifier.withDefaultNamespace("toast/advancement");
    public static final Identifier QUICK_ICON = Identifier.fromNamespaceAndPath(QuickResourcePackMod.ASSET_NAMESPACE, "icon/quick_switch");

    public static final int WHITE = 0xFFFFFFFF;
    public static final int GRAY = 0xFFA0A0A0;
    public static final int DARK_GRAY = 0xFF808080;
    public static final int YELLOW = 0xFFFFFF00;
    public static final int GREEN = 0xFF55FF55;
    public static final int RED = 0xFFFF5555;

    public static final int CELL = 22;
    public static final int CELL_WELL = 3;

    private Paint() {
    }

    public static boolean animations() {
        return ConfigStore.get().appearance.animations;
    }

    // one cell cut from the hotbar; its own right edge is the next cell's border, so borrow the bar's outer one
    public static void hotbarCell(GuiGraphicsExtractor g, int x, int y, float alpha) {
        int color = ARGB.white(alpha);
        g.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR, 182, 22, 0, 0, x, y, CELL - 1, CELL, color);
        g.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR, 182, 22, 181, 0, x + CELL - 1, y, 1, CELL, color);
    }

    public static void hotbarSelection(GuiGraphicsExtractor g, int cellX, int cellY, float alpha) {
        g.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SELECTION, cellX - 1, cellY - 1, 24, 23, ARGB.white(alpha));
    }

    public static void packIcon(GuiGraphicsExtractor g, String packId, int x, int y, int size, float alpha) {
        Identifier icon = PackIcons.get(packId);
        g.blit(RenderPipelines.GUI_TEXTURED, icon, x, y, 0, 0, size, size, size, size, ARGB.white(alpha));
    }

    public static void text(GuiGraphicsExtractor g, Font font, String text, int x, int y, int color, float alpha) {
        int faded = ARGB.multiplyAlpha(color, alpha);
        // the font drops near-transparent glyphs inconsistently; skip them outright
        if (ARGB.alpha(faded) > 4) {
            g.text(font, text, x, y, faded, true);
        }
    }

    public static void centered(GuiGraphicsExtractor g, Font font, String text, int cx, int y, int color, float alpha) {
        text(g, font, text, cx - font.width(text) / 2, y, color, alpha);
    }

    public static String ellipsize(Font font, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }
        String dots = "...";
        return font.plainSubstrByWidth(text, Math.max(0, maxWidth - font.width(dots))) + dots;
    }
}
