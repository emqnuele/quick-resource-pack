package com.emqnuele.quickresourcepack.mixin;

import com.emqnuele.quickresourcepack.pack.SeamlessReload;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Overlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    // keybinds keep working while a seamless reload runs; reload bookkeeping still sees the overlay
    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Gui;overlay()Lnet/minecraft/client/gui/screens/Overlay;"))
    private Overlay qrp$hideSeamlessOverlay(Overlay overlay) {
        return SeamlessReload.hide(overlay);
    }
}
