package com.emqnuele.quickresourcepack.hud;

import com.emqnuele.quickresourcepack.compat.GuiCompat;
import com.emqnuele.quickresourcepack.config.ConfigStore;
import com.emqnuele.quickresourcepack.core.config.QrpConfig.NotificationStyle;
import com.emqnuele.quickresourcepack.core.config.Slot;
import com.emqnuele.quickresourcepack.core.config.SlotMode;
import com.emqnuele.quickresourcepack.pack.Feedback;
import com.emqnuele.quickresourcepack.pack.PackIndex;
import com.emqnuele.quickresourcepack.pack.PackSwitcher;
import com.emqnuele.quickresourcepack.pack.SlotNames;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jspecify.annotations.Nullable;

import java.util.List;

public final class Notifier {
    private Notifier() {
    }

    private static NotificationStyle style() {
        return ConfigStore.get().appearance.notifications;
    }

    public static void loading(Feedback feedback) {
        if (style() == NotificationStyle.TOAST) {
            toast(PackToast.Kind.LOADING, Component.translatable("qrp.notify.loading"), headline(feedback), feedback.packId());
        }
    }

    public static void done(Feedback feedback) {
        Component name = headline(feedback);
        Component state = Component.translatable(feedback.enabled() ? "qrp.notify.on" : "qrp.notify.off");
        Component headline = feedback.missing().isEmpty()
                ? state
                : Component.translatable("qrp.notify.partial", state, feedback.missing().size());
        switch (style()) {
            case TOAST -> toast(PackToast.Kind.DONE, headline, name, feedback.packId());
            case ACTION_BAR -> actionBar(line(name, headline, feedback.enabled()));
            case CHAT -> chat(line(name, headline, feedback.enabled()));
            case NONE -> {
            }
        }
    }

    public static void failed(@Nullable Feedback feedback) {
        Component title = Component.translatable("qrp.notify.failed");
        Component name = feedback == null ? Component.empty() : headline(feedback);
        error(title, name, feedback == null ? null : feedback.packId());
    }

    public static void missing(Slot slot, List<String> missing) {
        String first = missing.isEmpty() ? "" : PackIndex.prettyId(missing.getFirst());
        error(Component.translatable("qrp.notify.missing"), Component.literal(first), null);
    }

    public static void emptySlot(Slot slot) {
        info(Component.translatable("qrp.notify.empty_slot"), SlotNames.of(slot));
    }

    public static void info(Component headline, Component detail) {
        switch (style()) {
            case TOAST -> toast(PackToast.Kind.INFO, headline, detail, null);
            case ACTION_BAR -> actionBar(headline.copy().append(" ").append(detail.copy().withStyle(ChatFormatting.GRAY)));
            case CHAT -> chat(headline.copy().append(" ").append(detail.copy().withStyle(ChatFormatting.GRAY)));
            case NONE -> {
            }
        }
    }

    // errors surface even with messages off: a silent failure is worse
    private static void error(Component headline, Component detail, @Nullable String packId) {
        if (style() == NotificationStyle.CHAT) {
            chat(headline.copy().withStyle(ChatFormatting.RED).append(" ").append(detail));
        } else {
            toast(PackToast.Kind.ERROR, headline, detail, packId);
        }
    }

    private static Component headline(Feedback feedback) {
        boolean profile = ConfigStore.get().slot(feedback.slotId()).map(s -> s.mode == SlotMode.PROFILE).orElse(false);
        if (profile || feedback.cause() == PackSwitcher.Cause.RULE && feedback.packId().isEmpty()) {
            return feedback.slotName();
        }
        return feedback.packTitle();
    }

    private static Component line(Component name, Component state, boolean enabled) {
        MutableComponent stateText = state.copy().withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.GRAY);
        return name.copy().append(Component.literal(": ").withStyle(ChatFormatting.GRAY)).append(stateText);
    }

    private static void toast(PackToast.Kind kind, Component headline, Component name, @Nullable String packId) {
        PackToast.show(GuiCompat.toasts(Minecraft.getInstance()), kind, headline, name, packId);
    }

    private static void actionBar(Component message) {
        GuiCompat.actionBar(Minecraft.getInstance(), message);
    }

    private static void chat(Component message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            MutableComponent prefix = Component.literal("[Quick Resource Pack] ").withStyle(ChatFormatting.GOLD);
            minecraft.player.sendSystemMessage(prefix.append(message));
        }
    }
}
