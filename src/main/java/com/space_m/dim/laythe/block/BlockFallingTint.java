package com.space_m.dim.laythe.block;

import net.minecraft.world.level.block.FallingBlock;

/**
 * {@code com.hbm.blocks.generic.BlockFallingTint} ({@code laythe_silt}): fallender Block. Die Original-Textur-
 * varianten je Position ({@code extraTextures}) gibt es fuer Laythe-Schlick nicht (nur die Grundtextur), die
 * Toenung {@code HSBtoRGB(0, 0, 1 - meta/15)} ist bei Metadatum 0 weiss - beides entfaellt daher ohne Unterschied.
 */
public class BlockFallingTint extends FallingBlock {

    public BlockFallingTint(Properties properties) {
        super(properties);
    }

    //? if >= 1.21.1 {
    /*public static final com.mojang.serialization.MapCodec<BlockFallingTint> CODEC = simpleCodec(BlockFallingTint::new);
    @Override protected com.mojang.serialization.MapCodec<? extends FallingBlock> codec() { return CODEC; }
    *///?}
}
