package com.emqnuele.quickresourcepack.mixin;

import com.emqnuele.quickresourcepack.pack.SeamlessReload;
import net.minecraft.client.gui.screens.LoadingOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LoadingOverlay.class)
public abstract class LoadingOverlayPauseMixin {
    // a seamless reload must not pause the game behind it
    @Inject(method = "isPauseScreen", at = @At("HEAD"), cancellable = true)
    private void qrp$keepPlaying(CallbackInfoReturnable<Boolean> cir) {
        if (((SeamlessReload.Tracked) (Object) this).qrp$isSeamless()) {
            cir.setReturnValue(false);
        }
    }
}
