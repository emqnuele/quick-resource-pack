package com.emqnuele.quickresourcepack;

import com.emqnuele.quickresourcepack.config.ModConfig;
import com.emqnuele.quickresourcepack.handler.KeybindHandler;
import com.emqnuele.quickresourcepack.handler.ResourcePackHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuickResourcePackMod implements ClientModInitializer {
    public static final String MOD_ID = "quick-resource-pack";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        ModConfig.load();
        KeybindHandler.register();

        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            if (ModConfig.getInstance().autoApplyOnStart) {
                ResourcePackHandler.applyPack();
            }
        });
    }
}
