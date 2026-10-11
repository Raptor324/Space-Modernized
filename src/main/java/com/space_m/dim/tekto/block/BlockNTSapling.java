package com.space_m.dim.tekto.block;

import java.util.Random;

import org.jetbrains.annotations.Nullable;

import com.space_m.atmosphere.IPlantableBreathing;
import com.space_m.celestial.CelestialFluids;
import com.space_m.celestial.trait.CBT_Atmosphere;
import com.space_m.dim.tekto.TTree;
import com.space_m.dim.tekto.TektoBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 1:1 {@code com.hbm.blocks.BlockNTSapling} ({@code sapling_pvc}, {@code EnumSapling} VINYL/PVC -> je Sorte ein Block
 * {@code sapling_vinyl}/{@code sapling_pvc}): waechst auf Tekto-Boeden (Licht &gt;= 9, 1/7 je Zufallstick, Knochenmehl
 * 45 %) zu {@link TTree}: Vinyl {@code TTree(false, 2, 4, 5, 3, 2)} aus {@code vinyl_log}/{@code pet_leaves}, PVC
 * {@code TTree(true, 2, 5, 7, 4, 3)} aus {@code pvc_log}/{@code rubber_leaves}. Atmet Tekto-Luft oder Chlor
 * ({@link IPlantableBreathing}, je &gt;= 0.1 atm).
 */
public class BlockNTSapling extends BushBlock implements BonemealableBlock, IPlantableBreathing {

    /** Original {@code EnumSapling}. */
    public enum EnumSapling { VINYL, PVC }

    protected static final VoxelShape SHAPE = BushBlock.box(2.0D, 0.0D, 2.0D, 14.0D, 12.0D, 14.0D);

    private static final ResourceLocation CHLORINE = ResourceLocation.fromNamespaceAndPath("hbm_m", "chlorine");

    private final EnumSapling type;

    public BlockNTSapling(Properties properties, EnumSapling type) {
        super(properties.randomTicks());
        this.type = type;
    }

    @Override
    public boolean canBreathe(@Nullable CBT_Atmosphere atmosphere) {
        return atmosphere != null && (atmosphere.hasFluid(CelestialFluids.TEKTOAIR, 0.1) || atmosphere.hasFluid(CHLORINE, 0.1));
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(TektoBlocks.RUBBER_SILT.get()) || state.is(TektoBlocks.RUBBER_GRASS.get());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getMaxLocalRawBrightness(pos.above()) >= 9 && random.nextInt(7) == 0) {
            grow(level, pos, random);
        }
    }

    /** {@code func_149878_d}. */
    public void grow(ServerLevel world, BlockPos pos, RandomSource rand) {
        TTree treeGen = switch (type) {
            case VINYL -> new TTree(false, 2, 4, 5, 3, 2, false, TektoBlocks.VINYL_LOG.get(), TektoBlocks.PET_LEAVES.get());
            case PVC -> new TTree(true, 2, 5, 7, 4, 3, false, TektoBlocks.PVC_LOG.get(), TektoBlocks.RUBBER_LEAVES.get());
        };

        BlockState own = world.getBlockState(pos);
        world.removeBlock(pos, false);
        if (!treeGen.generate(world, new Random(rand.nextLong()), pos.getX(), pos.getY(), pos.getZ())) {
            world.setBlock(pos, own, 2);
        }
    }

    @Override
    //? if < 1.21.1 {
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, boolean client) {
    //?} else {
    /*public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
    *///?}
        return true;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return level.random.nextFloat() < 0.45D;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        grow(level, pos, random);
    }

    //? if >= 1.21.1 {
    /*public static final com.mojang.serialization.MapCodec<BlockNTSapling> CODEC = simpleCodec(p -> new BlockNTSapling(p, EnumSapling.VINYL));
    @Override protected com.mojang.serialization.MapCodec<? extends BushBlock> codec() { return CODEC; }
    *///?}
}
