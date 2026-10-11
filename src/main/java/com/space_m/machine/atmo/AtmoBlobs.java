package com.space_m.machine.atmo;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Zugriff auf die abgedichteten Raeume (R2-A {@code com.space_m.atmosphere}), Original
 * {@code ChunkAtmosphereManager.proxy.hasAtmosphere(world, x, y, z)}: true, wenn ein Blob die Position enthaelt.
 */
public final class AtmoBlobs {
    private AtmoBlobs() {}

    public static boolean hasAtmosphere(Level level, BlockPos pos) {
        return com.space_m.atmosphere.ChunkAtmosphereManager.proxy.hasAtmosphere(level, pos);
    }
}
