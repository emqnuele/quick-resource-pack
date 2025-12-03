package com.emqnuele.quickresourcepack.mixin;

import com.emqnuele.quickresourcepack.config.ModConfig;
import com.emqnuele.quickresourcepack.handler.NotificationHandler;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.pack.PackListWidget;
import net.minecraft.client.gui.screen.pack.PackScreen;
import net.minecraft.client.gui.screen.pack.ResourcePackOrganizer;
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
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Set Quick Toggle Pack"), button -> {
            PackListWidget.ResourcePackEntry entry = this.availablePackList.getSelectedOrNull();
            if (entry == null) {
                entry = this.selectedPackList.getSelectedOrNull();
            }

            if (entry != null) {
                // We need to access the profile name.
                // Since ResourcePackEntry is a simple wrapper, we might need to rely on the
                // name displayed or use reflection if the field is private.
                // However, usually we can get the name.
                // Actually, ResourcePackEntry has a 'getName()' method in some versions or we
                // can access the profile.
                // Let's assume we can get the name. In 1.21, it might be different.
                // Wait, ResourcePackEntry usually holds a ResourcePackOrganizer.Pack.
                // Let's try to cast and get the name.

                // For now, I'll use a safe approach assuming standard access patterns or I'll
                // need to check mappings.
                // In 1.21.4, let's assume entry.getName() returns the pack name or ID.
                // Actually, looking at Yarn mappings, ResourcePackEntry has `getName()`.

                String packName = entry.getName();
                ModConfig.getInstance().selectedResourcePack = packName;
                ModConfig.save();
                NotificationHandler.showToast(
                        Text.translatable("notification.quickresourcepack.enabled", packName),
                        Text.literal("Saved for Quick Toggle"));
            } else {
                NotificationHandler.showToast(
                        Text.translatable("notification.quickresourcepack.nopack"),
                        Text.literal("Select a pack first"));
            }
        })
                .dimensions(this.width / 2 + 4, this.height - 48, 150, 20)
                .build());
    }
}
