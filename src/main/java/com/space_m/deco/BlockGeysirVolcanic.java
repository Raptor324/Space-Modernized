package com.space_m.deco;

import com.hbm_m.util.ParticleUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * {@code geysir_volcanic} = {@code com.hbm.blocks.generic.BlockGeysir} (Material.rock, Licht 1.0, Haerte 2.0,
 * Stein-Klang, kein Kreativreiter, laesst nichts fallen). Oben {@code geysir_stone}, sonst Vanilla-Stein.
 * Anzeige: je Anzeige-Tick eine Gasflamme ({@code ParticleUtil.spawnGasFlame}) an zufaelliger Stelle im Block
 * (y + 0.5..1.5), Bewegung Gauss * 0.2 / 0.1 / Gauss * 0.2.
 *
 * <p>Die {@code TileEntityGeysir} des Originals ist fuer diesen Geysir wirkungslos ({@code getDelay} = 0 und
 * {@code perform} ohne Zweig fuer volcanic): sie kippt nur jeden Tick das Metadatum, das keine Textur nutzt.
 * Darum hier ohne Block-Entity (die HBM-Klasse {@code BlockGeysir}/{@code GeysirBlockEntity} kennt nur Chlor/Nether).
 */
public class BlockGeysirVolcanic extends Block {

    public BlockGeysirVolcanic(Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource rand) {
        ParticleUtil.spawnGasFlame(world, pos.getX() + world.random.nextDouble(), pos.getY() + 0.5 + world.random.nextDouble(),
                pos.getZ() + world.random.nextDouble(), world.random.nextGaussian() * 0.2, 0.1, world.random.nextGaussian() * 0.2);
    }

    //? if >= 1.21.1 {
    /*public static final com.mojang.serialization.MapCodec<BlockGeysirVolcanic> CODEC = simpleCodec(BlockGeysirVolcanic::new);
    @Override protected com.mojang.serialization.MapCodec<? extends Block> codec() { return CODEC; }
    *///?}
}
