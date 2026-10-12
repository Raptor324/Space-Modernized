package com.space_m.dim.laythe.block;

import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * {@code com.hbm.blocks.generic.BlockFallingTint} ({@code laythe_silt}, Tekto-Sand): fallender Block. Die Original-
 * Texturvarianten je Position ({@code extraTextures}) gibt es fuer diese Bloecke nicht (nur die Grundtextur).
 *
 * <p>R9-B: das Metadatum toent den Block ({@code colorMultiplier = HSBtoRGB(0, 0, 1 - meta/15)}); die Laythe-Schlickhoehlen
 * ({@code OreCave(laythe_silt, 6/3)}) setzen dunklere Schichten. Im Port Blockzustand {@link #TINT} (0..15, Standard 0 =
 * weiss) + Blockfarbe {@link #color}; Beute/Item wie im Original immer Metadatum 0 ({@code damageDropped} nicht
 * ueberschrieben).
 */
public class BlockFallingTint extends FallingBlock {

    public static final IntegerProperty TINT = IntegerProperty.create("tint", 0, 15);

    public BlockFallingTint(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(TINT, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(TINT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState();
    }

    /** {@code Color.HSBtoRGB(0F, 0F, 1F - meta / 15F)} (Grauwert, Saettigung 0). */
    public static int color(BlockState state) {
        if (!state.hasProperty(TINT)) return 0xFFFFFF;
        float b = 1F - state.getValue(TINT) / 15F;
        int v = (int) (b * 255.0F + 0.5F);
        return (v << 16) | (v << 8) | v;
    }

    //? if >= 1.21.1 {
    /*public static final com.mojang.serialization.MapCodec<BlockFallingTint> CODEC = simpleCodec(BlockFallingTint::new);
    @Override protected com.mojang.serialization.MapCodec<? extends FallingBlock> codec() { return CODEC; }
    *///?}
}
