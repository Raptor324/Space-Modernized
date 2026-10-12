package com.space_m.entity.mob.block;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 1:1 {@code BlockStalagmite} Metadatum 2 ({@code EnumStalagmiteType.FLESH}, NTM Space): "Fleshy Spindles" an Decke
 * ({@code stalactite}) bzw. Boden ({@code stalagmite}) der Fleischhoehlen ({@code getMetaFromResource(tumor) = 2}).
 * Kreuzmodell ohne Kollision, haelt nur an festem Untergrund, droppt {@code flesh} ({@code getItemDropped} Fall 2).
 * Wie HBMs {@code NTMPlants.Stalagmite}, nur mit der in HBM fehlenden Fleisch-Variante.
 */
public class BlockStalagmiteFlesh extends Block {

    public final boolean ceiling;

    public BlockStalagmiteFlesh(Properties props, boolean ceiling) {
        super(props);
        this.ceiling = ceiling;
    }

    /** Material.rock, Haerte 0.5, Widerstand 2 (-> 1.2), Stein-Geraeusch (wie HBMs Tropfsteine). */
    public static BlockBehaviour.Properties props() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(0.5F, 1.2F).sound(SoundType.STONE)
                .noCollission().noOcclusion().requiresCorrectToolForDrops();
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        if (ceiling) return world.getBlockState(pos.above()).isFaceSturdy(world, pos.above(), Direction.DOWN);
        return world.getBlockState(pos.below()).isFaceSturdy(world, pos.below(), Direction.UP);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return canSurvive(defaultBlockState(), ctx.getLevel(), ctx.getClickedPos()) ? defaultBlockState() : null;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos from, boolean moving) {
        if (!world.isClientSide && !canSurvive(state, world, pos)) world.destroyBlock(pos, true);
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getCollisionShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return Shapes.empty();
    }

    @Override
    @SuppressWarnings("deprecation")
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return List.of(new ItemStack(com.space_m.item.SpaceMaterialItems.FLESH.get()));
    }
}
