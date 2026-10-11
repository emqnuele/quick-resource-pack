package com.emqnuele.quickresourcepack.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import com.emqnuele.quickresourcepack.core.ModConfigData;
import com.emqnuele.quickresourcepack.core.PackDisplayNames;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.Pack;

public final class ClothConfigScreen {
    private ClothConfigScreen() {
    }

    public static Screen createConfigScreen(Screen parent) {
        ModConfigData config = ModConfig.getInstance();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("config.quickresourcepack.title"));

        builder.setSavingRunnable(ModConfig::save);

        ConfigCategory general = builder.getOrCreateCategory(Component.translatable("category.quickresourcepack"));
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        LinkedHashMap<String, String> packNameToId = collectSelectablePacks();

        String currentDisplayName = PackDisplayNames.displayNameFor(config.selectedResourcePack, packNameToId);
        List<String> selections = PackDisplayNames.selectionsWithNone(packNameToId);

        general.addEntry(entryBuilder
                .startStringDropdownMenu(
                        Component.translatable("config.quickresourcepack.selected"),
                        currentDisplayName)
                .setDefaultValue("")
                .setSelections(selections)
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

    private static LinkedHashMap<String, String> collectSelectablePacks() {
        Minecraft client = Minecraft.getInstance();
        client.getResourcePackRepository().reload();

        List<PackDisplayNames.PackEntry> entries = new ArrayList<>();
        for (Pack pack : client.getResourcePackRepository().getAvailablePacks()) {
            if (pack.isRequired()) {
                continue;
            }
            entries.add(new PackDisplayNames.PackEntry(pack.getId(), pack.getTitle().getString()));
        }
        return PackDisplayNames.buildDisplayMap(entries);
    }
}
