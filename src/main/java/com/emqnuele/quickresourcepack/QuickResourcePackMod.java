package com.emqnuele.quickresourcepack;

import com.emqnuele.quickresourcepack.command.QrpCommand;
import com.emqnuele.quickresourcepack.config.ConfigStore;
import com.emqnuele.quickresourcepack.core.config.QrpConfig;
import com.emqnuele.quickresourcepack.core.config.Slot;
import com.emqnuele.quickresourcepack.hud.ActivePacksHud;
import com.emqnuele.quickresourcepack.hud.Notifier;
import com.emqnuele.quickresourcepack.input.InputRouter;
import com.emqnuele.quickresourcepack.integration.PackScreenHook;
import com.emqnuele.quickresourcepack.pack.PackSwitcher;
import com.emqnuele.quickresourcepack.rules.RuleWatcher;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class QuickResourcePackMod implements ClientModInitializer {
    public static final String MOD_ID = "quick-resource-pack";
    public static final String ASSET_NAMESPACE = "quickresourcepack";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        ConfigStore.load();
        InputRouter.register();
        QrpCommand.register();
        PackScreenHook.register();

        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(ASSET_NAMESPACE, "hud"), (graphics, delta) -> {
            InputRouter.frame();
            ActivePacksHud.render(graphics);
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            InputRouter.tick(client);
            PackSwitcher.tick();
            RuleWatcher.tick(client);
        });

        ClientLifecycleEvents.CLIENT_STARTED.register(client -> applyStartupSlots());
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> client.execute(QuickResourcePackMod::welcome));
    }

    private static void applyStartupSlots() {
        QrpConfig config = ConfigStore.get();
        List<Slot> startup = config.slots.stream().filter(s -> s.applyOnStartup && s.hasPacks()).toList();
        if (!startup.isEmpty()) {
            PackSwitcher.ensure(startup, List.of(), PackSwitcher.Cause.STARTUP);
        }
    }

    private static void welcome() {
        QrpConfig config = ConfigStore.get();
        if (config.welcomed) {
            return;
        }
        config.welcomed = true;
        ConfigStore.save();
        Notifier.info(Component.translatable("qrp.welcome.title"),
                Component.translatable("qrp.welcome.subtitle", InputRouter.mainKey().getTranslatedKeyMessage()));
    }
}
