package com.emqnuele.quickresourcepack.hud;

import com.emqnuele.quickresourcepack.pack.SeamlessReload;
import com.emqnuele.quickresourcepack.ui.Paint;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

public final class PackToast implements Toast {
    public enum Kind {
        LOADING,
        DONE,
        INFO,
        ERROR
    }

    private static final Object TOKEN = new Object();
    private static final long DONE_MS = 2500;
    private static final long ERROR_MS = 5000;

    private Kind kind;
    private Component headline;
    private Component name;
    private @Nullable String packId;
    private boolean changed;
    private long changedAt;
    private float shownProgress;
    private Visibility wanted = Visibility.SHOW;

    private PackToast(Kind kind, Component headline, Component name, @Nullable String packId) {
        reset(kind, headline, name, packId);
    }

    public static void show(ToastManager manager, Kind kind, Component headline, Component name, @Nullable String packId) {
        PackToast existing = manager.getToast(PackToast.class, TOKEN);
        if (existing == null) {
            manager.addToast(new PackToast(kind, headline, name, packId));
        } else {
            existing.reset(kind, headline, name, packId);
        }
    }

    private void reset(Kind newKind, Component newHeadline, Component newName, @Nullable String newPackId) {
        if (newKind == Kind.LOADING && kind != Kind.LOADING) {
            shownProgress = 0f;
        }
        kind = newKind;
        headline = newHeadline;
        name = newName;
        packId = newPackId;
        changed = true;
    }

    @Override
    public Visibility getWantedVisibility() {
        return wanted;
    }

    @Override
    public void update(ToastManager manager, long fullyVisibleForMs) {
        if (changed) {
            changedAt = fullyVisibleForMs;
            changed = false;
        }
        if (kind == Kind.LOADING) {
            // a reload that never reports back must not pin the toast forever
            wanted = SeamlessReload.isRunning() || fullyVisibleForMs - changedAt < 2000 ? Visibility.SHOW : Visibility.HIDE;
            return;
        }
        long shownFor = kind == Kind.ERROR ? ERROR_MS : DONE_MS;
        wanted = fullyVisibleForMs - changedAt < shownFor * manager.getNotificationDisplayTimeMultiplier()
                ? Visibility.SHOW : Visibility.HIDE;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, Font font, long fullyVisibleForMs) {
        g.blitSprite(RenderPipelines.GUI_TEXTURED, Paint.TOAST, 0, 0, width(), height());
        int textX = 8;
        if (packId != null && !packId.isEmpty()) {
            Paint.packIcon(g, packId, 8, 8, 16, 1f);
            textX = 30;
        }
        int maxWidth = width() - textX - 6;
        int headlineColor = kind == Kind.ERROR ? Paint.RED : Paint.YELLOW;
        g.text(font, Paint.ellipsize(font, headline.getString(), maxWidth), textX, 7, headlineColor, false);
        g.text(font, Paint.ellipsize(font, name.getString(), maxWidth), textX, 18, Paint.WHITE, false);

        if (kind == Kind.LOADING) {
            float target = SeamlessReload.isRunning() ? SeamlessReload.progress() : 1f;
            shownProgress = Mth.lerp(0.25f, shownProgress, Math.max(shownProgress, target));
            int left = 3;
            int right = width() - 3;
            int y = height() - 4;
            g.fill(left, y, right, y + 1, 0xFF3B3B3B);
            g.fill(left, y, left + Math.round((right - left) * shownProgress), y + 1, Paint.GREEN);
        }
    }

    @Override
    public Object getToken() {
        return TOKEN;
    }
}
