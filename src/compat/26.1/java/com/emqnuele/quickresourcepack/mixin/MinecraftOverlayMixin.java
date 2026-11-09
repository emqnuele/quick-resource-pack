package com.emqnuele.quickresourcepack.mixin;

import com.emqnuele.quickresourcepack.pack.SeamlessReload;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Overlay;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Minecraft.class)
public abstract class MinecraftOverlayMixin {
    // the third read gates keybinds; the first two tick the overlay and must still see it
    @ModifyExpressionValue(method = "tick", at = @At(value = "FIELD", opcode = Opcodes.GETFIELD, ordinal = 2,
            target = "Lnet/minecraft/client/Minecraft;overlay:Lnet/minecraft/client/gui/screens/Overlay;"))
    private Overlay qrp$hideSeamlessOverlay(Overlay overlay) {
        return SeamlessReload.hide(overlay);
    }
}
