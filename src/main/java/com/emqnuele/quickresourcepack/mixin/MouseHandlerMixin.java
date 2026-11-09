package com.emqnuele.quickresourcepack.mixin;

import com.emqnuele.quickresourcepack.input.InputRouter;
import com.emqnuele.quickresourcepack.pack.SeamlessReload;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Inject(method = "onButton", at = @At("HEAD"))
    private void qrp$routeButton(long handle, MouseButtonInfo info, int action, CallbackInfo ci) {
        InputRouter.onInput(handle, InputConstants.Type.MOUSE.getOrCreate(info.button()), action,
                InputRouter.modifiers(info.hasShiftDown(), info.hasControlDown(), info.hasAltDown()));
    }

    @ModifyExpressionValue(method = {"onButton", "onScroll", "handleAccumulatedMovement"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Gui;overlay()Lnet/minecraft/client/gui/screens/Overlay;"))
    private Overlay qrp$hideSeamlessOverlay(Overlay overlay) {
        return SeamlessReload.hide(overlay);
    }
}
