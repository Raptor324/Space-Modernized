package com.space_m.dim.legacy;

import java.util.Random;

import com.space_m.dim.CelestialWorld;

import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code net.minecraft.world.gen.MapGenBase} (1.7.10): fragt fuer den aktuellen Chunk jeden Start-Chunk im
 * Umkreis {@link #range} mit einem nur vom Start-Chunk abhaengigen Zufall ab; die Unterklasse darf die Bloecke des
 * aktuellen Chunks veraendern. Arbeitet auf dem Chunk-Puffer ({@link Legacy}) statt auf {@code Block[]}.
 *
 * <p>Die Instanzen haben Zustand ({@link #rand}, {@link #worldObj}); der Generator haelt deshalb je Thread eine
 * eigene Instanz (siehe {@code CelestialChunkGenerator}).
 */
public class MapGenBase {

    /** Radius in Chunks, in dem Start-Chunks befragt werden. */
    protected int range = 8;
    /** Zufall dieses Generators; vor jedem Start-Chunk neu geseedet. */
    protected Random rand = new Random();
    /** Die Welt, in der zuletzt generiert wurde. */
    protected CelestialWorld worldObj;

    /** {@code func_151539_a(IChunkProvider, World, chunkX, chunkZ, Block[])}. */
    public void func_151539_a(CelestialWorld world, int chunkX, int chunkZ, BlockState[] blocks) {
        int k = this.range;
        this.worldObj = world;
        this.rand.setSeed(world.getSeed());
        long l = this.rand.nextLong();
        long i1 = this.rand.nextLong();

        for (int j1 = chunkX - k; j1 <= chunkX + k; ++j1) {
            for (int k1 = chunkZ - k; k1 <= chunkZ + k; ++k1) {
                long l1 = (long) j1 * l;
                long i2 = (long) k1 * i1;
                this.rand.setSeed(l1 ^ i2 ^ world.getSeed());
                this.func_151538_a(world, j1, k1, chunkX, chunkZ, blocks);
            }
        }
    }

    /** Wird fuer jeden Start-Chunk ({@code offsetX}, {@code offsetZ}) aufgerufen. */
    protected void func_151538_a(CelestialWorld world, int offsetX, int offsetZ, int chunkX, int chunkZ, BlockState[] blocks) {
    }
}
