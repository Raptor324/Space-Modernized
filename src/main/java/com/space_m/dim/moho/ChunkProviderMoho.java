package com.space_m.dim.moho;

import com.space_m.dim.CelestialWorld;
import com.space_m.dim.ChunkProviderCelestial;
import com.space_m.dim.mapgen.ExperimentalCaveGenerator;
import com.space_m.dim.mapgen.MapGenCrater;
import com.space_m.dim.mapgen.MapGenPlateau;
import com.space_m.dim.mapgen.MapGenVolcano;
import com.space_m.dim.mapgen.MapgenRavineButBased;
import com.space_m.dim.moho.biome.BiomeGenBaseMoho;
import com.space_m.dim.noise.MapGenVNoise;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code com.hbm.dim.moho.ChunkProviderMoho}: Moho-Gestein mit Lavameeren bis y 63, Gesteinsplatten im Lavameer
 * ({@code MapGenVNoise}, Zellen 27, Riss 0.5, ab y 62 / 25 dick), Stufenplateaus ({@code MapGenPlateau}, +6, Stufe 2),
 * Experimental-Hoehlen (1, 52, 5.0), Schluchten, kleine (r 8-32, 1/6) und grosse Krater (r 96-128, 1/64) mit
 * Regolith, Schildvulkane (r 64-128, 1/72, {@code volcano_core} in Basalt). Reihenfolge der MapGens wie im Original.
 *
 * <p>{@code getFlatWorldBlock} (Superflach) entfaellt (siehe {@link ChunkProviderCelestial}).
 */
public class ChunkProviderMoho extends ChunkProviderCelestial {

    private final ExperimentalCaveGenerator caveGenV2 = new ExperimentalCaveGenerator(1, 52, 5.0F);
    private final MapgenRavineButBased rgen = new MapgenRavineButBased();

    private final MapGenVNoise noise = new MapGenVNoise();
    private final MapGenCrater smallCrater = new MapGenCrater(6);
    private final MapGenCrater largeCrater = new MapGenCrater(64);
    private final MapGenVolcano volcano = new MapGenVolcano(72);
    private final MapGenPlateau plateau;

    public ChunkProviderMoho(CelestialWorld world, long seed) {
        super(world, seed);
        this.plateau = new MapGenPlateau(worldObj);

        BlockState mohoStone = MohoBlocks.MOHO_STONE.get().defaultBlockState();
        BlockState mohoRegolith = MohoBlocks.MOHO_REGOLITH.get().defaultBlockState();
        BlockState basalt = com.hbm_m.block.ModBlocks.BASALT.get().defaultBlockState();
        BlockState lava = Blocks.LAVA.defaultBlockState();

        smallCrater.setSize(8, 32);
        largeCrater.setSize(96, 128);
        volcano.setSize(64, 128);
        volcano.setMaterial(com.hbm_m.block.ModBlocks.VOLCANO_CORE.get().defaultBlockState(), basalt);

        smallCrater.regolith = largeCrater.regolith = mohoRegolith;
        smallCrater.rock = largeCrater.rock = mohoStone;

        caveGenV2.stoneBlock = mohoStone;
        rgen.stoneBlock = mohoStone;
        stoneBlock = mohoStone;
        seaBlock = lava;

        noise.surfBlock = mohoStone;
        noise.rockBlock = mohoStone;
        noise.fluidBlock = lava;
        noise.crackSize = 0.5;
        noise.cellSize = 27;
        noise.plateStartY = 62;
        noise.plateThickness = 25;
        noise.applyToBiome = BiomeGenBaseMoho.mohoLavaSea;

        plateau.maxPlateauAddition = 6;
        plateau.surfrock = mohoRegolith;
        plateau.stoneBlock = mohoStone;
        plateau.fillblock = lava;
        plateau.maxPlateauAddition = 6;
        plateau.stepHeight = 2;
        plateau.noiseScale = 0.03;
        plateau.applyToBiome = BiomeGenBaseMoho.mohoPlateau;
    }

    @Override
    public BlockMetaBuffer getChunkPrimer(int x, int z) {
        BlockMetaBuffer buffer = super.getChunkPrimer(x, z);

        boolean hasLavaSea = false;
        boolean hasPlateau = false;

        for (int i = 0; i < biomesForGeneration.length; i++) {
            if (biomesForGeneration[i] == BiomeGenBaseMoho.mohoLavaSea) hasLavaSea = true;
            if (biomesForGeneration[i] == BiomeGenBaseMoho.mohoPlateau) hasPlateau = true;
            if (hasLavaSea && hasPlateau) break;
        }

        if (hasLavaSea) noise.func_151539_a(worldObj, x, z, buffer.blocks);

        if (hasPlateau) plateau.func_151539_a(worldObj, x, z, buffer.blocks);

        caveGenV2.func_151539_a(worldObj, x, z, buffer.blocks);
        rgen.func_151539_a(worldObj, x, z, buffer.blocks);
        smallCrater.func_151539_a(worldObj, x, z, buffer.blocks);
        largeCrater.func_151539_a(worldObj, x, z, buffer.blocks);
        volcano.func_151539_a(worldObj, x, z, buffer.blocks);
        return buffer;
    }
}
