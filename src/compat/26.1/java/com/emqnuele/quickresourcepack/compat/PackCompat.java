package com.emqnuele.quickresourcepack.compat;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.util.Util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

// minecraft 26.1: root files come from the full pack resources
public final class PackCompat {
    private PackCompat() {
    }

    public static NativeImage readRootImage(Pack pack, String name) throws IOException {
        try (PackResources resources = pack.open()) {
            IoSupplier<InputStream> resource = resources.getRootResource(name);
            if (resource == null) {
                return null;
            }
            try (InputStream stream = resource.get()) {
                return NativeImage.read(stream);
            }
        }
    }

    public static void openFolder(Path path) {
        Util.getPlatform().openPath(path);
    }
}
