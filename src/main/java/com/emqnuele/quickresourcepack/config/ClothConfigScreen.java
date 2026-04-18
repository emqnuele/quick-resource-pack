package com.emqnuele.quickresourcepack.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.Pack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ClothConfigScreen {
    public static Screen createConfigScreen(Screen parent) {
        ModConfig config = ModConfig.getInstance();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("config.quickresourcepack.title"));

        builder.setSavingRunnable(ModConfig::save);

        ConfigCategory general = builder.getOrCreateCategory(Component.translatable("category.quickresourcepack"));
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        Minecraft client = Minecraft.getInstance();
        client.getResourcePackRepository().reload();

        LinkedHashMap<String, String> packNameToId = new LinkedHashMap<>();
        for (Pack pack : client.getResourcePackRepository().getAvailablePacks()) {
            if (pack.isRequired()) {
                continue;
            }

            String displayName = pack.getTitle().getString();
            if (packNameToId.containsKey(displayName)) {
                packNameToId.put(displayName + " [" + pack.getId() + "]", pack.getId());
            } else {
                packNameToId.put(displayName, pack.getId());
            }
        }

        String currentDisplayName = "";
        for (Map.Entry<String, String> entry : packNameToId.entrySet()) {
            if (entry.getValue().equals(config.selectedResourcePack)) {
                currentDisplayName = entry.getKey();
                break;
            }
        }

        List<String> displayNames = new ArrayList<>();
        displayNames.add("");
        displayNames.addAll(packNameToId.keySet());

        general.addEntry(entryBuilder
                .startStringDropdownMenu(
                        Component.translatable("config.quickresourcepack.selected"),
                        currentDisplayName)
                .setDefaultValue("")
                .setSelections(displayNames)
                .setSuggestionMode(false)
                .setSaveConsumer(displayName -> {
                    if (displayName == null || displayName.isEmpty()) {
                        config.selectedResourcePack = "";
                    } else {
                        String id = packNameToId.get(displayName);
                        config.selectedResourcePack = id != null ? id : "";
                    }
                })
                .build());

        general.addEntry(entryBuilder
                .startBooleanToggle(
                        Component.translatable("config.quickresourcepack.notifications"),
                        config.showNotifications)
                .setDefaultValue(true)
                .setSaveConsumer(value -> config.showNotifications = value)
                .build());

        general.addEntry(entryBuilder
                .startBooleanToggle(
                        Component.translatable("config.quickresourcepack.autoapply"),
                        config.autoApplyOnStart)
                .setDefaultValue(false)
                .setSaveConsumer(value -> config.autoApplyOnStart = value)
                .build());

        return builder.build();
    }
}
