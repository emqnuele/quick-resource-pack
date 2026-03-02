package com.emqnuele.quickresourcepack.mixin;

import com.emqnuele.quickresourcepack.handler.ResourcePackHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Overlay;
import net.minecraft.client.gui.screen.SplashOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    @Inject(method = "setOverlay", at = @At("HEAD"), cancellable = true)
    private void onSetOverlay(Overlay overlay, CallbackInfo ci) {
        if (ResourcePackHandler.isSilentReload) {
            if (overlay instanceof SplashOverlay) {
                ci.cancel();
            }
        }
    }
}
