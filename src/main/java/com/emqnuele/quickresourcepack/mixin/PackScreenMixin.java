package com.emqnuele.quickresourcepack.mixin;

import com.emqnuele.quickresourcepack.config.ModConfig;
import com.emqnuele.quickresourcepack.handler.NotificationHandler;
import com.emqnuele.quickresourcepack.handler.ResourcePackHandler;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.pack.PackListWidget;
import net.minecraft.client.gui.screen.pack.PackScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PackScreen.class)
public abstract class PackScreenMixin extends Screen {

    @Shadow
    private PackListWidget availablePackList;
    @Shadow
    private PackListWidget selectedPackList;

    protected PackScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void init(CallbackInfo ci) {
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("button.quickresourcepack.set"), button -> {
            PackListWidget.ResourcePackEntry entry = this.availablePackList.getSelectedOrNull();
            if (entry == null) {
                entry = this.selectedPackList.getSelectedOrNull();
            }

            if (entry != null) {
                String packId = entry.getName();
                String displayName = ResourcePackHandler.getPackDisplayName(packId);
                ModConfig.getInstance().selectedResourcePack = packId;
                ModConfig.save();
                NotificationHandler.notify(Text.translatable("notification.quickresourcepack.set", displayName));
            } else {
                NotificationHandler.notify(Text.translatable("notification.quickresourcepack.nopack"));
            }
        })
                .dimensions(this.width / 2 - 75, this.height - 24, 150, 20)
                .build());
    }
}
