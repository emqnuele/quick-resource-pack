package com.emqnuele.quickresourcepack.compat;

import com.mojang.blaze3d.Blaze3D;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.server.packs.PackMetadataResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.resources.IoSupplier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

// minecraft 26.3: root files come from the lighter metadata view of a pack
public final class PackCompat {
    private PackCompat() {
    }

    public static NativeImage readRootImage(Pack pack, String name) throws IOException {
        try (PackMetadataResources resources = pack.openMetadata()) {
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
        Blaze3D.openPath(path);
    }
}
