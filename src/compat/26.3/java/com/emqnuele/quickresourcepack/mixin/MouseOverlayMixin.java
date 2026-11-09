package com.emqnuele.quickresourcepack.mixin;

import com.emqnuele.quickresourcepack.pack.SeamlessReload;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.Overlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MouseHandler.class)
public abstract class MouseOverlayMixin {
    @ModifyExpressionValue(method = {"onButton", "onScroll", "handleAccumulatedMovement"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Gui;overlay()Lnet/minecraft/client/gui/screens/Overlay;"))
    private Overlay qrp$hideSeamlessOverlay(Overlay overlay) {
        return SeamlessReload.hide(overlay);
    }
}
