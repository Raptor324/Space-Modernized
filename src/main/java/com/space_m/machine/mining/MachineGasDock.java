package com.space_m.machine.mining;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.interfaces.ILookOverlay;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.space_m.machine.chem.ChemUtil;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 1:1 NTM Space {@code MachineGasDock} ({@code gas_dock}, "Gas Giant Harvester"): {@code getDimensions
 * {0, 0, 1, 1, 1, 1}} (3x3 flach), Offset 1; alle acht Nebenzellen sind {@code TileEntityProxyCombo().fluid()}
 * (hier Anschlusszellen). Hitbox jeder Zelle 12/16 hoch. TESR ({@code RenderDecoBlock}, Zweig {@code gas_dock}).
 * Overlay: Gastank ({@code <-} rot) und die beiden Treibstofftanks ({@code ->} gruen).
 * Original-Klasse: MachineGasDock
 */
public class MachineGasDock extends DummyableMachineBlock implements ILookOverlay {

    /** Original {@code setBlockBounds(0, 0, 0, 1, 12/16, 1)} fuer jede der 3x3 Zellen, als Masterform um die Kernecke. */
    private static final VoxelShape BOUNDS = Shapes.box(-1.0D, 0.0D, -1.0D, 2.0D, 12.0D / 16.0D, 2.0D);

    public MachineGasDock(Properties properties) {
        super(properties);
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        DummyableStructureBuilder b = DummyableStructureBuilder.create().box(0, 0, 1, 1, 1, 1);
        // createNewTileEntity: jede Nebenzelle ist ein TileEntityProxyCombo().fluid()
        for (int f = -1; f <= 1; f++) {
            for (int s = -1; s <= 1; s++) {
                if (f != 0 || s != 0) b.extra(f, 0, s);
            }
        }
        return b.placementOffset(1).build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Override
    public VoxelShape getCustomMasterVoxelShape(BlockState state) {
        return BOUNDS;
    }

    /** Original {@code getRenderType() = -1}. */
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineGasDock(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, MiningMachines.GAS_DOCK_BE.get(), TileEntityMachineGasDock::tick);
    }

    @Override
    public void printHook(GuiGraphics g, Level world, BlockPos pos) {
        if (!(world.getBlockEntity(pos) instanceof TileEntityMachineGasDock tower)) return;

        List<Component> text = new ArrayList<>();

        for (int i = 0; i < tower.tanks.length; i++)
            text.add(ChemUtil.tankLine(tower.tanks[i], i >= 1));

        ILookOverlay.printGeneric(g, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineGasDock> CODEC = simpleCodec(MachineGasDock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
