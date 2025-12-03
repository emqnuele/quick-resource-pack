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
            // If we are in silent reload mode, prevent the overlay from being set.
            // This effectively hides the loading screen.
            // We might want to allow some overlays, but definitely not the resource reload
            // one.
            // Usually resource reload uses SplashOverlay or similar.

            if (overlay instanceof SplashOverlay) {
                ci.cancel();
            }
            // If it's null (clearing overlay), we should probably allow it,
            // or maybe we don't care because we never set it in the first place.
            // But to be safe, if we are silent reloading, we just don't want *new* loading
            // screens.
            // If the game tries to clear the overlay (overlay == null), we let it pass
            // just in case there was one before (though there shouldn't be).
            // Actually, if we cancel setting it to null, we might get stuck with an old
            // overlay?
            // No, because we prevented the old one.

            // Let's be specific: if it's a SplashOverlay (loading screen), cancel it.
            // If it's null, let it happen (clearing).
        }
    }
}
