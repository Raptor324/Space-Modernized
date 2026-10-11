package com.space_m.dim.eve.genlayer;

import java.util.Random;

import com.hbm_m.world.gen.legacy.L;
import com.space_m.dim.eve.EveBlocks;
import com.space_m.dim.legacy.Legacy;

import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code com.hbm.dim.eve.genlayer.WorldGenElectricVolcano}: in 1/10 der Aufrufe ein Basaltkegel (Hoehe
 * {@code height}..+3, Breite bis {@code width}) auf {@code eve_silt}, darunter Neodym-Tiefenerz
 * ({@code ore_depth_nether_neodymium} = HBM {@code depth_nether_neodymium}), die Mittelsaeule wird ausgehoehlt und
 * fuenf Bloecke ueber dem Fuss sitzt der Entladungsgeysir ({@code geysir_electric}).
 *
 * <p>Der Kegel reicht ueber mehrere Chunks ("TODO ... fix this cascade!" im Original); in 1.20 schreibt eine
 * Dekoration nur in das 3x3-Chunkfenster - Bloecke ausserhalb entfallen still (HBM {@code L.setState}).
 */
public class WorldGenElectricVolcano {

    public BlockState volBlock;
    public BlockState mainMat;

    public int height;
    public int width;

    public BlockState surfaceBlock;
    public BlockState stoneBlock;

    public WorldGenElectricVolcano(int height, int width, BlockState surfaceBlock, BlockState stoneBlock) {
        this.height = height;
        this.width = width;
        this.surfaceBlock = surfaceBlock;
        this.stoneBlock = stoneBlock;

        this.volBlock = com.hbm_m.block.ModBlocks.BASALT.get().defaultBlockState();
        this.mainMat = EveBlocks.GEYSIR_ELECTRIC.get().defaultBlockState();
    }

    private static void func_150515_a(WorldGenLevel world, int x, int y, int z, BlockState state) {
        L.setState(world, x, y, z, state, 2);
    }

    private boolean replaceable(BlockState block) {
        return block.isAir() || Legacy.is(block, surfaceBlock) || Legacy.is(block, stoneBlock);
    }

    public boolean generate(WorldGenLevel p_76484_1_, Random p_76484_2_, int p_76484_3_, int p_76484_4_, int p_76484_5_) {
        BlockState basalt = com.hbm_m.block.ModBlocks.BASALT.get().defaultBlockState();
        BlockState neodymium = com.hbm_m.block.ModBlocks.DEPTH_NETHER_NEODYMIUM.get().defaultBlockState();
        BlockState geysir = EveBlocks.GEYSIR_ELECTRIC.get().defaultBlockState();

        if (p_76484_2_.nextInt(10) != 0) {
            return false;
        }
        while (L.isAirBlock(p_76484_1_, p_76484_3_, p_76484_4_, p_76484_5_) && p_76484_4_ > 2) {
            --p_76484_4_;
        }

        if (!Legacy.is(L.getState(p_76484_1_, p_76484_3_, p_76484_4_, p_76484_5_), surfaceBlock)) {
            return false;
        } else {
            p_76484_4_ += p_76484_2_.nextInt(1);
            int l = p_76484_2_.nextInt(4) + height;
            int i1 = l / 4 + p_76484_2_.nextInt(width);

            if (i1 > 1 && p_76484_2_.nextInt(5) == 0) {
                p_76484_2_.nextInt(30);
            }

            int j1;
            int k1;
            int l1;

            for (j1 = 0; j1 < l; ++j1) {
                float f = (1.0F - (float) j1 / (float) l) * (float) i1;
                k1 = Legacy.ceil(f);

                for (l1 = -k1; l1 <= k1; ++l1) {
                    float f1 = (float) Math.abs(l1) - 0.25F;

                    for (int i2 = -k1; i2 <= k1; ++i2) {
                        float f2 = (float) Math.abs(i2) - 0.75F;

                        if ((l1 == 0 && i2 == 0 || f1 * f1 + f2 * f2 <= f * f) && (l1 != -k1 && l1 != k1 && i2 != -k1 && i2 != k1 || p_76484_2_.nextFloat() <= 0.75F)) {
                            BlockState block = L.getState(p_76484_1_, p_76484_3_ + l1, p_76484_4_ + j1, p_76484_5_ + i2);

                            if (replaceable(block)) {
                                L.setState(p_76484_1_, p_76484_3_ + l1, p_76484_4_ + j1, p_76484_5_ + i2, basalt, 3);
                            }

                            if (j1 != 0 && k1 > 1) {
                                block = L.getState(p_76484_1_, p_76484_3_ + l1, p_76484_4_ - j1, p_76484_5_ + i2);

                                if (replaceable(block)) {
                                    func_150515_a(p_76484_1_, p_76484_3_ + l1, p_76484_4_ - j1, p_76484_5_ + i2, neodymium);
                                }
                            }
                        }
                        if ((l1 == 0 && j1 == 0) || f1 * f1 + f2 * f2 > f * f) {
                            BlockState block = L.getState(p_76484_1_, p_76484_3_ + l1, p_76484_4_ + j1, p_76484_5_ + i2);

                            if (replaceable(block)) {
                                func_150515_a(p_76484_1_, p_76484_3_, p_76484_4_ + j1, p_76484_5_, Blocks.AIR.defaultBlockState());
                            }
                        }
                        if ((l1 == 0 && j1 == 0) || f1 * f1 + f2 * f2 > f * f) {
                            BlockState block = L.getState(p_76484_1_, p_76484_3_ + l1, p_76484_4_ + j1, p_76484_5_ + i2);

                            if (replaceable(block)) {
                                func_150515_a(p_76484_1_, p_76484_3_, p_76484_4_ + 5, p_76484_5_, geysir);
                            }
                        }
                    }
                }
            }

            return true;
        }
    }
}
