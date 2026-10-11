package com.space_m.dim.eve;

import java.util.Random;

import com.space_m.config.SpaceConfig;
import com.space_m.dim.CelestialWorld;
import com.space_m.dim.ChunkProviderCelestial;
import com.space_m.dim.eve.biome.BiomeGenBaseEve;
import com.space_m.dim.legacy.Legacy;
import com.space_m.dim.legacy.NoiseGeneratorPerlin;
import com.space_m.dim.mapgen.MapGenBubble;
import com.space_m.dim.noise.MapGenVNoise;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code com.hbm.dim.eve.ChunkProviderEve}: Eve-Gestein ohne Rundung ({@code reclamp = false}), Quecksilber-Meere
 * bis y 63, Quecksilber-Platten im Explodium-Ozean ({@code MapGenVNoise}, Zellen 72, Riss 2.0, Platten ab y 57 / 35
 * dick), Lavarisse in der Seismischen Ebene und Erdgasblasen ({@code eveGasSpawn}, r 8-16, {@code ore_gas}).
 *
 * <p>Abweichung: {@code crackNoise} ist im Original mit {@code world.rand} (ungeseedeter Weltzufall) gebaut - jeder
 * Neustart haette andere Risse an Chunkgrenzen. Hier aus dem Weltseed, damit alle Generator-Instanzen denselben
 * Chunk liefern. {@code getFlatWorldBlock} (Superflach) entfaellt (siehe {@link ChunkProviderCelestial}).
 */
public class ChunkProviderEve extends ChunkProviderCelestial {

    private final NoiseGeneratorPerlin crackNoise;
    private final MapGenVNoise noise = new MapGenVNoise();

    private final MapGenBubble oil = new MapGenBubble(SpaceConfig.get().eveGasSpawn);

    public ChunkProviderEve(CelestialWorld world, long seed) {
        super(world, seed);
        BlockState eveRock = EveBlocks.EVE_ROCK.get().defaultBlockState();
        BlockState mercury = EveBlocks.MERCURY_BLOCK.get().defaultBlockState();

        reclamp = false;
        stoneBlock = eveRock;
        seaBlock = mercury;
        this.crackNoise = new NoiseGeneratorPerlin(new Random(seed), 4); // Original: world.rand

        noise.fluidBlock = mercury;
        noise.rockBlock = eveRock;
        noise.surfBlock = EveBlocks.EVE_SILT.get().defaultBlockState();
        noise.cellSize = 72;
        noise.crackSize = 2.0;
        noise.plateThickness = 35;
        noise.shapeExponent = 2.0;
        noise.plateStartY = 57;

        noise.applyToBiome = BiomeGenBaseEve.eveOcean;

        oil.block = EveBlocks.ORE_GAS.get().defaultBlockState(); // ore_gas, meta = CelestialBody.getMeta(world) (Eve)
        oil.replace = eveRock;
        oil.setSize(8, 16);
    }

    @Override
    public BlockMetaBuffer getChunkPrimer(int x, int z) {
        BlockMetaBuffer buffer = super.getChunkPrimer(x, z);

        boolean hasOcean = false;
        boolean hasSeismic = false;

        for (int i = 0; i < biomesForGeneration.length; i++) {
            if (biomesForGeneration[i] == BiomeGenBaseEve.eveOcean) hasOcean = true;
            if (biomesForGeneration[i] == BiomeGenBaseEve.eveSeismicPlains) hasSeismic = true;
            if (hasOcean && hasSeismic) break;
        }

        if (hasSeismic) generateCracks(x, z, buffer);
        if (hasOcean) noise.func_151539_a(worldObj, x, z, buffer.blocks);

        oil.func_151539_a(worldObj, x, z, buffer.blocks);

        return buffer;
    }

    private void generateCracks(int chunkX, int chunkZ, BlockMetaBuffer buffer) {
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState lava = Blocks.LAVA.defaultBlockState();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                if (biomesForGeneration[x + z * 16] != BiomeGenBaseEve.eveSeismicPlains) continue;

                double crackValue = crackNoise.func_151601_a((chunkX * 16 + x) * 0.3, (chunkZ * 16 + z) * 0.3);  // Lower scale value for more spread-out cracks

                if (crackValue > 0.9) {
                    int bedrockY = -1;
                    for (int y = 0; y < 256; y++) {
                        int index = (x * 16 + z) * 256 + y;
                        if (Legacy.is(buffer.blocks[index], Blocks.BEDROCK)) {
                            if (bedrockY == -1) bedrockY = y;
                        } else {
                            buffer.blocks[index] = air;
                        }
                    }
                    if (bedrockY != -1) {
                        for (int y = bedrockY + 1; y < Math.min(bedrockY + 10, 256); y++) {
                            int index = (x * 16 + z) * 256 + y;
                            buffer.blocks[index] = lava;
                        }
                    }
                }
            }
        }
    }
}
