package com.emqnuele.quickresourcepack.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.resource.ResourcePackSource;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ClothConfigScreen {
    public static Screen createConfigScreen(Screen parent) {
        ModConfig config = ModConfig.getInstance();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.translatable("config.quickresourcepack.title"));

        builder.setSavingRunnable(ModConfig::save);

        ConfigCategory general = builder.getOrCreateCategory(Text.translatable("category.quickresourcepack"));
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        MinecraftClient client = MinecraftClient.getInstance();
        client.getResourcePackManager().scanPacks();

        LinkedHashMap<String, String> packNameToId = new LinkedHashMap<>();
        for (ResourcePackProfile profile : client.getResourcePackManager().getProfiles()) {
            if (!profile.isPinned() && profile.getSource() == ResourcePackSource.NONE) {
                String displayName = profile.getDisplayName().getString();
                if (packNameToId.containsKey(displayName)) {
                    packNameToId.put(displayName + " [" + profile.getId() + "]", profile.getId());
                } else {
                    packNameToId.put(displayName, profile.getId());
                }
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
                        Text.translatable("config.quickresourcepack.selected"),
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
                        Text.translatable("config.quickresourcepack.notifications"),
                        config.showNotifications)
                .setDefaultValue(true)
                .setSaveConsumer(v -> config.showNotifications = v)
                .build());

        general.addEntry(entryBuilder
                .startBooleanToggle(
                        Text.translatable("config.quickresourcepack.autoapply"),
                        config.autoApplyOnStart)
                .setDefaultValue(false)
                .setSaveConsumer(v -> config.autoApplyOnStart = v)
                .build());

        return builder.build();
    }
}
