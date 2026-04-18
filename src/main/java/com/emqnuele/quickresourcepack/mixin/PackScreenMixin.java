package com.emqnuele.quickresourcepack.mixin;

import com.emqnuele.quickresourcepack.config.ModConfig;
import com.emqnuele.quickresourcepack.handler.NotificationHandler;
import com.emqnuele.quickresourcepack.handler.ResourcePackHandler;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.pack.PackListWidget;
import net.minecraft.client.gui.screen.pack.PackScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;
import net.minecraft.text.TranslatableTextContent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PackScreen.class)
public abstract class PackScreenMixin extends Screen {

    @Shadow
    private PackListWidget availablePackList;
    @Shadow
    private PackListWidget selectedPackList;

    @Unique
    private ButtonWidget quickResourcePack$button;

    protected PackScreenMixin(Text title) {
        super(title);
    }

    @Unique
    private PackListWidget.Entry quickResourcePack$getSelectedEntry() {
        PackListWidget.Entry entry = this.availablePackList.getSelectedOrNull();
        if (entry == null) {
            entry = this.selectedPackList.getSelectedOrNull();
        }
        return entry;
    }

    @Unique
    private String quickResourcePack$getTranslationKey(Text text) {
        TextContent content = text.getContent();
        if (content instanceof TranslatableTextContent translatableContent) {
            return translatableContent.getKey();
        }
        return null;
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void init(CallbackInfo ci) {
        int buttonWidth = 150;
        int buttonHeight = 20;
        int buttonX = this.width / 2 - buttonWidth / 2;
        int buttonY = this.height - 52;

        for (Element element : this.children()) {
            if (element instanceof ButtonWidget candidate && "pack.openFolder".equals(this.quickResourcePack$getTranslationKey(candidate.getMessage()))) {
                buttonX = candidate.getX();
                int preferredBelow = candidate.getY() + candidate.getHeight() + 4;
                int fallbackAbove = candidate.getY() - buttonHeight - 4;
                if (preferredBelow + buttonHeight <= this.height - 4) {
                    buttonY = preferredBelow;
                } else {
                    buttonY = Math.max(4, fallbackAbove);
                }
                break;
            }
        }

        this.quickResourcePack$button = this.addDrawableChild(ButtonWidget.builder(Text.translatable("button.quickresourcepack.set"), button -> {
            PackListWidget.Entry entry = this.quickResourcePack$getSelectedEntry();

            if (entry instanceof PackListWidget.ResourcePackEntry resourcePackEntry) {
                String packId = resourcePackEntry.getName();
                String displayName = ResourcePackHandler.getPackDisplayName(packId);
                ModConfig.getInstance().selectedResourcePack = packId;
                ModConfig.save();
                NotificationHandler.notify(Text.translatable("notification.quickresourcepack.set", displayName));
            } else {
                NotificationHandler.notify(Text.translatable("notification.quickresourcepack.nopack"));
            }
        })
                .dimensions(buttonX, buttonY, buttonWidth, buttonHeight)
                .build());

        this.quickResourcePack$button.active = this.quickResourcePack$getSelectedEntry() != null;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void tick(CallbackInfo ci) {
        if (this.quickResourcePack$button != null) {
            this.quickResourcePack$button.active = this.quickResourcePack$getSelectedEntry() != null;
        }
    }
}
