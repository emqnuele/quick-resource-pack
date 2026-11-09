package com.emqnuele.quickresourcepack.mixin;

import com.emqnuele.quickresourcepack.pack.SeamlessReload;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.Overlay;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LoadingOverlay.class)
public abstract class LoadingOverlayPauseMixin extends Overlay {
    // a seamless reload must not pause the game behind it
    @Override
    public boolean isPausing() {
        return !((SeamlessReload.Tracked) this).qrp$isSeamless() && super.isPausing();
    }
}
