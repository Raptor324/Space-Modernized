package com.space_m.dim.eve.genlayer;

import java.util.Random;

import com.hbm_m.world.gen.legacy.L;
import com.space_m.dim.eve.EveBlocks;
import com.space_m.dim.legacy.Legacy;

import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code com.hbm.dim.eve.genlayer.WorldGenEveSpike} (Vanilla-{@code WorldGenIceSpike} mit Eve-Gestein): Spitze aus
 * {@code eve_rock} auf {@code eve_silt}, 10-13 hoch, manchmal 10-39 Bloecke angehoben, mit Saeule bis y 50.
 * Weltzugriff ueber HBM {@code L} (1.7.10-Semantik, Schreiben nur im 3x3-Chunkfenster der Dekoration).
 */
public class WorldGenEveSpike {

    private static void func_150515_a(WorldGenLevel world, int x, int y, int z, BlockState state) {
        L.setState(world, x, y, z, state, 2);
    }

    public boolean generate(WorldGenLevel p_76484_1_, Random p_76484_2_, int p_76484_3_, int p_76484_4_, int p_76484_5_) {
        BlockState eveSilt = EveBlocks.EVE_SILT.get().defaultBlockState();
        BlockState eveRock = EveBlocks.EVE_ROCK.get().defaultBlockState();

        while (L.isAirBlock(p_76484_1_, p_76484_3_, p_76484_4_, p_76484_5_) && p_76484_4_ > 2) {
            --p_76484_4_;
        }

        if (!Legacy.is(L.getState(p_76484_1_, p_76484_3_, p_76484_4_, p_76484_5_), eveSilt)) {
            return false;
        } else {
            p_76484_4_ += p_76484_2_.nextInt(4);
            int l = p_76484_2_.nextInt(4) + 10;
            int i1 = l / 4 + p_76484_2_.nextInt(2);

            if (i1 > 1 && p_76484_2_.nextInt(2) == 0) {
                p_76484_4_ += 10 + p_76484_2_.nextInt(30);
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
                        float f2 = (float) Math.abs(i2) - 0.25F;

                        if ((l1 == 0 && i2 == 0 || f1 * f1 + f2 * f2 <= f * f) && (l1 != -k1 && l1 != k1 && i2 != -k1 && i2 != k1 || p_76484_2_.nextFloat() <= 0.75F)) {
                            BlockState block = L.getState(p_76484_1_, p_76484_3_ + l1, p_76484_4_ + j1, p_76484_5_ + i2);

                            if (block.isAir() || Legacy.is(block, eveSilt) || Legacy.is(block, eveRock)) {
                                func_150515_a(p_76484_1_, p_76484_3_ + l1, p_76484_4_ + j1, p_76484_5_ + i2, eveRock);
                            }

                            if (j1 != 0 && k1 > 1) {
                                block = L.getState(p_76484_1_, p_76484_3_ + l1, p_76484_4_ - j1, p_76484_5_ + i2);

                                if (block.isAir() || Legacy.is(block, eveSilt) || Legacy.is(block, eveRock)) {
                                    func_150515_a(p_76484_1_, p_76484_3_ + l1, p_76484_4_ - j1, p_76484_5_ + i2, eveRock);
                                }
                            }
                        }
                    }
                }
            }

            j1 = i1 - 1;

            if (j1 < 0) {
                j1 = 0;
            } else if (j1 > 1) {
                j1 = 1;
            }

            for (int j2 = -j1; j2 <= j1; ++j2) {
                k1 = -j1;

                while (k1 <= j1) {
                    l1 = p_76484_4_ - 1;
                    int k2 = 50;

                    if (Math.abs(j2) == 1 && Math.abs(k1) == 1) {
                        k2 = p_76484_2_.nextInt(5);
                    }

                    while (true) {
                        if (l1 > 50) {
                            BlockState block1 = L.getState(p_76484_1_, p_76484_3_ + j2, l1, p_76484_5_ + k1);

                            if (block1.isAir() || Legacy.is(block1, eveSilt) || Legacy.is(block1, eveRock)) {
                                func_150515_a(p_76484_1_, p_76484_3_ + j2, l1, p_76484_5_ + k1, eveRock);
                                --l1;
                                --k2;

                                if (k2 <= 0) {
                                    l1 -= p_76484_2_.nextInt(5) + 1;
                                    k2 = p_76484_2_.nextInt(5);
                                }

                                continue;
                            }
                        }

                        ++k1;
                        break;
                    }
                }
            }

            return true;
        }
    }
}
