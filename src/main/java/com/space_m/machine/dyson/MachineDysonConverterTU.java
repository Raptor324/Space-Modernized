package com.space_m.machine.dyson;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.space_m.machine.misc.R7MachineBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 NTM Space {@code MachineDysonConverterTU} ({@code dyson_converter_tu}): {@code getDimensions {2, 0, 1, 6, 1, 1}},
 * Offset 6, Waermequellen-Zellen ueber dem Kern ({@code y+1}, {@code y+2}; Original
 * {@code TileEntityProxyCombo().heatSource()}).
 * Original-Klasse: MachineDysonConverterTU
 */
public class MachineDysonConverterTU extends R7MachineBlock {

    public MachineDysonConverterTU(Properties properties) {
        super(properties);
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        return DummyableStructureBuilder.create().box(2, 0, 1, 6, 1, 1).extra(0, 1, 0).extra(0, 2, 0)
                .placementOffset(6).build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityDysonConverterTU(pos, state);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineDysonConverterTU> CODEC = simpleCodec(MachineDysonConverterTU::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
