package com.emqnuele.quickresourcepack.rules;

import com.emqnuele.quickresourcepack.config.ConfigStore;
import com.emqnuele.quickresourcepack.core.config.QrpConfig;
import com.emqnuele.quickresourcepack.core.config.Slot;
import com.emqnuele.quickresourcepack.core.logic.RuleEngine;
import com.emqnuele.quickresourcepack.pack.PackSwitcher;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Applies rules when the player's place changes. Nothing is reverted on the
 * title screen: the next place decides, so hopping between two servers that
 * want the same pack never reloads at all.
 */
public final class RuleWatcher {
    private static RuleEngine.@Nullable Context last;

    private RuleWatcher() {
    }

    public static void tick(Minecraft minecraft) {
        if (minecraft.level == null || minecraft.player == null) {
            last = null;
            return;
        }
        RuleEngine.Context context = currentContext(minecraft);
        if (Objects.equals(context, last)) {
            return;
        }
        last = context;
        evaluate(context);
    }

    /** Re-applies rules now, e.g. after the player edited them. */
    public static void reevaluate() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            last = currentContext(minecraft);
            evaluate(last);
        }
    }

    private static void evaluate(RuleEngine.Context context) {
        QrpConfig config = ConfigStore.get();
        Set<String> wanted = config.behavior.rules
                ? RuleEngine.wantedSlots(config.rules, context)
                : Set.of();
        Set<String> active = new LinkedHashSet<>(config.ruleActivated);

        List<Slot> turnOn = new ArrayList<>();
        List<Slot> turnOff = new ArrayList<>();
        for (String id : wanted) {
            if (!active.contains(id)) {
                config.slot(id).ifPresent(turnOn::add);
            }
        }
        for (String id : active) {
            if (!wanted.contains(id)) {
                config.slot(id).ifPresent(turnOff::add);
            }
        }
        if (turnOn.isEmpty() && turnOff.isEmpty()) {
            return;
        }
        config.ruleActivated = new ArrayList<>(wanted);
        ConfigStore.save();
        PackSwitcher.ensure(turnOn, turnOff, PackSwitcher.Cause.RULE);
    }

    public static RuleEngine.Context currentContext(Minecraft minecraft) {
        String server = null;
        String world = null;
        IntegratedServer integrated = minecraft.getSingleplayerServer();
        if (integrated != null) {
            world = integrated.getWorldData().getLevelName();
        } else {
            ServerData data = minecraft.getCurrentServer();
            if (data != null) {
                server = data.ip;
            }
        }
        String dimension = minecraft.level == null ? null : minecraft.level.dimension().identifier().toString();
        return new RuleEngine.Context(server, world, dimension);
    }
}
