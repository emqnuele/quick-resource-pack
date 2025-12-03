package com.emqnuele.quickresourcepack;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.emqnuele.quickresourcepack.config.ModConfig;
import com.emqnuele.quickresourcepack.handler.KeybindHandler;

public class QuickResourcePackMod implements ClientModInitializer {
	public static final String MOD_ID = "quick-resource-pack";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitializeClient() {
		LOGGER.info("Quick Resource Pack Mod Initializing...");
		ModConfig.getInstance(); // Load config
		KeybindHandler.register();
	}
}
