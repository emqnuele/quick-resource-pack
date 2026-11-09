package com.emqnuele.quickresourcepack.mixin;

import com.emqnuele.quickresourcepack.input.InputRouter;
import com.emqnuele.quickresourcepack.pack.SeamlessReload;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
    @Inject(method = "keyPress", at = @At("HEAD"))
    private void qrp$routeKey(long handle, int action, KeyEvent event, CallbackInfo ci) {
        InputRouter.onInput(handle, InputConstants.getKey(event), action,
                InputRouter.modifiers(event.hasShiftDown(), event.hasControlDown(), event.hasAltDown()));
    }

    @ModifyExpressionValue(method = {"charTyped", "textEditing"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Gui;overlay()Lnet/minecraft/client/gui/screens/Overlay;"))
    private Overlay qrp$hideSeamlessOverlay(Overlay overlay) {
        return SeamlessReload.hide(overlay);
    }
}
