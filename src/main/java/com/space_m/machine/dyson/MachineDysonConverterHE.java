package com.space_m.machine.dyson;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.space_m.machine.misc.R7MachineBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 NTM Space {@code MachineDysonConverterHE} ({@code dyson_converter_he}): {@code getDimensions {2, 0, 4, 4, 1, 1}},
 * Offset 4, Stromausgang ueber die Anschlusszelle {@code -dir*4} (Original {@code TileEntityProxyCombo(false, true, false)}).
 * Original-Klasse: MachineDysonConverterHE
 */
public class MachineDysonConverterHE extends R7MachineBlock {

    public MachineDysonConverterHE(Properties properties) {
        super(properties);
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        return DummyableStructureBuilder.create().box(2, 0, 4, 4, 1, 1).extra(-4, 0, 0)
                .placementOffset(4).build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityDysonConverterHE(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, DysonBlockEntities.CONVERTER_HE.get(), TileEntityDysonConverterHE::tick);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineDysonConverterHE> CODEC = simpleCodec(MachineDysonConverterHE::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
