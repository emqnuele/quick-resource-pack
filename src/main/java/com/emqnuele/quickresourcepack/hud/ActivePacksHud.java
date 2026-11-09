package com.emqnuele.quickresourcepack.hud;

import com.emqnuele.quickresourcepack.config.ConfigStore;
import com.emqnuele.quickresourcepack.core.config.QrpConfig;
import com.emqnuele.quickresourcepack.core.config.Slot;
import com.emqnuele.quickresourcepack.core.logic.PackUniverse;
import com.emqnuele.quickresourcepack.core.logic.SelectionPlanner;
import com.emqnuele.quickresourcepack.pack.PackIndex;
import com.emqnuele.quickresourcepack.ui.Paint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.util.ARGB;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.List;

public final class ActivePacksHud {
    private static final long REFRESH_MS = 250;

    private static final int TILE = 24;

    private record Row(String packId, boolean on) {
    }

    private static List<Row> rows = List.of();
    private static long refreshedAt;

    private ActivePacksHud() {
    }

    public static void render(GuiGraphicsExtractor g) {
        QrpConfig config = ConfigStore.get();
        if (!config.appearance.hud) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getDebugOverlay().showDebugScreen()) {
            return;
        }
        long now = Util.getMillis();
        if (now - refreshedAt > REFRESH_MS) {
            rows = buildRows(config);
            refreshedAt = now;
        }
        if (rows.isEmpty()) {
            return;
        }
        QrpConfig.HudCorner corner = config.appearance.hudCorner;
        boolean right = corner == QrpConfig.HudCorner.TOP_RIGHT || corner == QrpConfig.HudCorner.BOTTOM_RIGHT;
        boolean bottom = corner == QrpConfig.HudCorner.BOTTOM_LEFT || corner == QrpConfig.HudCorner.BOTTOM_RIGHT;
        // same tile, spacing and margin as vanilla's status effect icons
        int y = bottom ? g.guiHeight() - TILE - 1 : 1;
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            int x = right ? g.guiWidth() - (TILE + 1) * (i + 1) : 1 + (TILE + 1) * i;
            float alpha = row.on() ? 1f : 0.45f;
            g.blitSprite(RenderPipelines.GUI_TEXTURED, Paint.EFFECT_BACKGROUND, x, y, TILE, TILE, ARGB.white(alpha));
            Paint.packIcon(g, row.packId(), x + 3, y + 3, 18, alpha);
        }
    }

    public static void invalidate() {
        refreshedAt = 0;
    }

    private static List<Row> buildRows(QrpConfig config) {
        PackUniverse universe = PackIndex.universe();
        List<String> selected = PackIndex.selected();
        List<Row> result = new ArrayList<>();
        for (Slot slot : config.slots) {
            if (!slot.hasPacks()) {
                continue;
            }
            SelectionPlanner.Status status = SelectionPlanner.status(slot, selected, universe);
            if (config.appearance.hudOnlyActive && !status.active()) {
                continue;
            }
            String pack = status.cycleIndex() >= 0
                    ? SelectionPlanner.usablePacks(slot, universe).get(status.cycleIndex())
                    : slot.packs.getFirst();
            result.add(new Row(pack, status.active()));
        }
        return result;
    }
}
