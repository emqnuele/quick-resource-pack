package com.emqnuele.quickresourcepack.ui;

import com.emqnuele.quickresourcepack.core.config.KeyCombo;
import com.emqnuele.quickresourcepack.input.InputRouter;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

public final class KeyCapture {
    private @Nullable Object owner;
    private @Nullable Consumer<@Nullable KeyCombo> target;
    private @Nullable String pendingModifier;

    public void start(Object newOwner, Consumer<@Nullable KeyCombo> onCaptured) {
        owner = newOwner;
        target = onCaptured;
        pendingModifier = null;
    }

    public boolean isActive() {
        return target != null;
    }

    public boolean isActiveFor(Object candidate) {
        return target != null && candidate.equals(owner);
    }

    public void cancel() {
        owner = null;
        target = null;
        pendingModifier = null;
    }

    public boolean keyPressed(KeyEvent event) {
        if (target == null) {
            return false;
        }
        if (event.isEscape()) {
            finish(null);
            return true;
        }
        String name = InputConstants.getKey(event).getName();
        if (KeyCombo.isModifierKey(name)) {
            pendingModifier = name;
            return true;
        }
        int modifiers = InputRouter.modifiers(event.hasShiftDown(), event.hasControlDown(), event.hasAltDown());
        finish(new KeyCombo(name, modifiers));
        return true;
    }

    public boolean keyReleased(KeyEvent event) {
        if (target == null || pendingModifier == null) {
            return false;
        }
        String name = InputConstants.getKey(event).getName();
        if (name.equals(pendingModifier)) {
            finish(new KeyCombo(name, 0));
            return true;
        }
        return false;
    }

    public boolean mouseClicked(MouseButtonEvent event) {
        if (target == null) {
            return false;
        }
        // a slot on the attack or use button would fire on every swing
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT || event.button() == InputConstants.MOUSE_BUTTON_RIGHT) {
            cancel();
            return true;
        }
        String name = InputConstants.Type.MOUSE.getOrCreate(event.button()).getName();
        int modifiers = InputRouter.modifiers(event.hasShiftDown(), event.hasControlDown(), event.hasAltDown());
        finish(new KeyCombo(name, modifiers));
        return true;
    }

    private void finish(@Nullable KeyCombo combo) {
        Consumer<@Nullable KeyCombo> done = target;
        cancel();
        if (done != null) {
            done.accept(combo);
        }
    }
}
