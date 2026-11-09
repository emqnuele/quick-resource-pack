package com.emqnuele.quickresourcepack.command;

import com.emqnuele.quickresourcepack.config.ConfigStore;
import com.emqnuele.quickresourcepack.core.config.QrpConfig;
import com.emqnuele.quickresourcepack.core.config.Slot;
import com.emqnuele.quickresourcepack.core.logic.PackUniverse;
import com.emqnuele.quickresourcepack.core.logic.RuleEngine;
import com.emqnuele.quickresourcepack.core.logic.SelectionPlanner;
import com.emqnuele.quickresourcepack.input.InputRouter;
import com.emqnuele.quickresourcepack.pack.PackIndex;
import com.emqnuele.quickresourcepack.pack.PackSwitcher;
import com.emqnuele.quickresourcepack.pack.SlotNames;
import com.emqnuele.quickresourcepack.rules.RuleWatcher;
import com.emqnuele.quickresourcepack.ui.HubScreen;
import com.emqnuele.quickresourcepack.ui.WheelScreen;
import com.emqnuele.quickresourcepack.compat.ScreenCompat;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** {@code /qrp}: everything the keys do, scriptable and reachable without binding anything. */
public final class QrpCommand {
    private QrpCommand() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> dispatcher.register(
                ClientCommands.literal("qrp")
                        .executes(c -> openLater(c.getSource().getClient(), () -> HubScreen.open(null)))
                        .then(ClientCommands.literal("wheel")
                                .executes(c -> openLater(c.getSource().getClient(), QrpCommand::openWheel)))
                        .then(ClientCommands.literal("list").executes(QrpCommand::list))
                        .then(ClientCommands.literal("reload").executes(QrpCommand::reload))
                        .then(ClientCommands.literal("where").executes(QrpCommand::where))
                        .then(ClientCommands.literal("toggle")
                                .then(ClientCommands.argument("slot", StringArgumentType.greedyString())
                                        .suggests((c, builder) -> SharedSuggestionProvider.suggest(
                                                ConfigStore.get().slots.stream().map(SlotNames::plain), builder))
                                        .executes(QrpCommand::toggle)))));
    }

    private static int openLater(Minecraft client, Runnable action) {
        // the chat screen closes after the command runs; open ours on the next tick
        client.execute(action);
        return 1;
    }

    private static void openWheel() {
        Minecraft minecraft = Minecraft.getInstance();
        if (ConfigStore.get().wheelSlots().isEmpty()) {
            HubScreen.open(null);
        } else {
            ScreenCompat.setScreen(minecraft, new WheelScreen(InputRouter.mainKeyBound(), false));
        }
    }

    private static int toggle(CommandContext<FabricClientCommandSource> c) {
        String query = StringArgumentType.getString(c, "slot");
        Optional<Slot> slot = find(ConfigStore.get(), query);
        if (slot.isEmpty()) {
            c.getSource().sendError(Component.translatable("qrp.command.no_slot", query));
            return 0;
        }
        PackSwitcher.press(slot.get(), PackSwitcher.Cause.COMMAND);
        return 1;
    }

    /** By name (case-insensitive), then by 1-based position, so scripts can use either. */
    static Optional<Slot> find(QrpConfig config, String query) {
        String wanted = query.trim().toLowerCase(Locale.ROOT);
        for (Slot slot : config.slots) {
            if (SlotNames.plain(slot).toLowerCase(Locale.ROOT).equals(wanted)) {
                return Optional.of(slot);
            }
        }
        try {
            int index = Integer.parseInt(wanted) - 1;
            if (index >= 0 && index < config.slots.size()) {
                return Optional.of(config.slots.get(index));
            }
        } catch (NumberFormatException ignored) {
            // not a number: fall through to "not found"
        }
        return Optional.empty();
    }

    private static int list(CommandContext<FabricClientCommandSource> c) {
        QrpConfig config = ConfigStore.get();
        if (config.slots.isEmpty()) {
            c.getSource().sendFeedback(Component.translatable("qrp.command.empty"));
            return 1;
        }
        PackUniverse universe = PackIndex.universe();
        List<String> selected = PackIndex.selected();
        for (int i = 0; i < config.slots.size(); i++) {
            Slot slot = config.slots.get(i);
            boolean on = SelectionPlanner.status(slot, selected, universe).active();
            c.getSource().sendFeedback(Component.literal((i + 1) + ". ").withStyle(ChatFormatting.DARK_GRAY)
                    .append(SlotNames.of(slot).copy().withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.translatable(on ? "qrp.notify.on" : "qrp.notify.off")
                            .withStyle(on ? ChatFormatting.GREEN : ChatFormatting.GRAY)));
        }
        return 1;
    }

    private static int reload(CommandContext<FabricClientCommandSource> c) {
        PackIndex.refresh();
        c.getSource().sendFeedback(Component.translatable("qrp.command.reloaded", PackIndex.selectable().size()));
        return 1;
    }

    private static int where(CommandContext<FabricClientCommandSource> c) {
        RuleEngine.Context context = RuleWatcher.currentContext(c.getSource().getClient());
        c.getSource().sendFeedback(Component.translatable("qrp.command.where",
                orDash(context.server()), orDash(context.world()), orDash(context.dimension())));
        return 1;
    }

    private static String orDash(String value) {
        return value == null ? "-" : value;
    }
}
