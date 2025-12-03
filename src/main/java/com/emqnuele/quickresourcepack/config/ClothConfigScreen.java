package com.emqnuele.quickresourcepack.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourcePackProfile;

public class ClothConfigScreen {
        public static Screen createConfigScreen(Screen parent) {
                ModConfig config = ModConfig.getInstance();
                ConfigBuilder builder = ConfigBuilder.create()
                                .setParentScreen(parent)
                                .setTitle(Text.translatable("config.quickresourcepack.title"));

                builder.setSavingRunnable(ModConfig::save);

                ConfigCategory general = builder.getOrCreateCategory(Text.translatable("category.quickresourcepack"));
                ConfigEntryBuilder entryBuilder = builder.entryBuilder();

                // Resource Pack Selection
                List<String> availablePacks = new ArrayList<>();
                availablePacks.add(""); // Option for no pack
                for (ResourcePackProfile profile : MinecraftClient.getInstance().getResourcePackManager()
                                .getProfiles()) {
                        if (!profile.isPinned()) {
                                availablePacks.add(profile.getId());
                        }
                }

                general.addEntry(entryBuilder
                                .startStringDropdownMenu(Text.translatable("config.quickresourcepack.selected"),
                                                config.selectedResourcePack)
                                .setDefaultValue("")
                                .setSelections(availablePacks)
                                .setSaveConsumer(newValue -> config.selectedResourcePack = newValue)
                                .build());

                // Notifications
                general.addEntry(entryBuilder
                                .startBooleanToggle(Text.translatable("config.quickresourcepack.notifications"),
                                                config.showNotifications)
                                .setDefaultValue(true)
                                .setSaveConsumer(newValue -> config.showNotifications = newValue)
                                .build());

                // Auto Apply
                general.addEntry(entryBuilder
                                .startBooleanToggle(Text.translatable("config.quickresourcepack.autoapply"),
                                                config.autoApplyOnStart)
                                .setDefaultValue(false)
                                .setSaveConsumer(newValue -> config.autoApplyOnStart = newValue)
                                .build());

                // Keybind
                general.addEntry(entryBuilder
                                .startKeyCodeField(Text.translatable("config.quickresourcepack.keybind"),
                                                net.minecraft.client.util.InputUtil.Type.KEYSYM
                                                                .createFromCode(config.toggleKeyCode))
                                .setDefaultValue(net.minecraft.client.util.InputUtil.Type.KEYSYM
                                                .createFromCode(org.lwjgl.glfw.GLFW.GLFW_KEY_R))
                                .setKeySaveConsumer(newValue -> config.toggleKeyCode = newValue.getCode())
                                .build());

                return builder.build();
        }
}
