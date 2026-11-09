package com.emqnuele.quickresourcepack.mixin;

import com.emqnuele.quickresourcepack.compat.GuiCompat;
import com.emqnuele.quickresourcepack.compat.ScreenCompat;
import com.emqnuele.quickresourcepack.pack.SeamlessReload;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.function.Consumer;

@Mixin(LoadingOverlay.class)
public abstract class LoadingOverlayMixin extends Overlay implements SeamlessReload.Tracked {
    @Shadow
    @Final
    private Minecraft minecraft;
    @Shadow
    @Final
    private ReloadInstance reload;
    @Shadow
    @Final
    private Consumer<Optional<Throwable>> onFinish;
    @Shadow
    private float currentProgress;
    @Shadow
    private long fadeOutStart;

    @Unique
    private SeamlessReload.Claim qrp$claim = SeamlessReload.Claim.NONE;
    @Unique
    private boolean qrp$reported;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void qrp$claimReload(Minecraft minecraft, ReloadInstance reload, Consumer<Optional<Throwable>> onFinish,
                                 boolean fadeIn, CallbackInfo ci) {
        // fadeIn is false only for the initial load and recovery reloads, never ours
        if (fadeIn) {
            this.qrp$claim = SeamlessReload.claim(this);
        }
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void qrp$renderSeamless(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        if (!qrp$isSeamless()) {
            return;
        }
        ci.cancel();
        this.currentProgress = Mth.clamp(this.currentProgress * 0.95F + this.reload.getActualProgress() * 0.05F, 0.0F, 1.0F);
        // gui skips the screen while any overlay is set, so draw it ourselves
        Screen screen = ScreenCompat.getScreen(this.minecraft);
        if (screen != null) {
            screen.extractRenderStateWithTooltipAndSubtitles(graphics, mouseX, mouseY, a);
        }
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void qrp$tickSeamless(CallbackInfo ci) {
        if (!qrp$isSeamless()) {
            return;
        }
        ci.cancel();
        if (this.fadeOutStart != -1L || !this.reload.isDone()) {
            return;
        }
        this.fadeOutStart = Util.getMillis();
        boolean failed = false;
        try {
            this.reload.checkExceptions();
            this.onFinish.accept(Optional.empty());
        } catch (Throwable t) {
            failed = true;
            this.onFinish.accept(Optional.of(t));
        }
        Screen screen = ScreenCompat.getScreen(this.minecraft);
        if (screen != null) {
            Window window = this.minecraft.getWindow();
            screen.init(window.getGuiScaledWidth(), window.getGuiScaledHeight());
        }
        // a failed reload installs its own recovery overlay, which must survive
        if (GuiCompat.overlay(this.minecraft) == (Object) this) {
            GuiCompat.setOverlay(this.minecraft, null);
        }
        qrp$report(failed);
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V"))
    private void qrp$reportVanillaFinish(Consumer<Object> consumer, Object result, Operation<Void> original) {
        original.call(consumer, result);
        if (this.qrp$claim == SeamlessReload.Claim.OWNED) {
            qrp$report(result instanceof Optional<?> optional && optional.isPresent());
        }
    }

    @Unique
    private void qrp$report(boolean failed) {
        if (!this.qrp$reported) {
            this.qrp$reported = true;
            SeamlessReload.finished(this, failed);
        }
    }

    @Override
    public boolean qrp$isOwned() {
        return this.qrp$claim != SeamlessReload.Claim.NONE;
    }

    @Override
    public boolean qrp$isSeamless() {
        return this.qrp$claim == SeamlessReload.Claim.SEAMLESS;
    }

    @Override
    public float qrp$progress() {
        return this.currentProgress;
    }
}
