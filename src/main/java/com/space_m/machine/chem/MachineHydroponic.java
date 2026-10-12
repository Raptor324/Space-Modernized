package com.space_m.machine.chem;

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.interfaces.ILookOverlay;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 1:1 NTM Space {@code MachineHydroponic} ({@code hydrobay}, "Industrial Greenhouse"): hohle Maschine 5 breit, 3 tief,
 * 3 hoch ({@code getDimensions {2, 0, 0, 0, 2, 2}}, Offset 0, Pruefraum {@code {2, -1, 1, 1, 2, 2}}), gefuellt wie das
 * Original-{@code fillSpace} (Vorder-/Rueckwand, Dach, Seitenkappen, Sockel, Dachanschluss). Die drei Pflanzfelder ueber
 * dem Sockel (y+1, rot -1..1) bleiben frei; die zwei Lampenzellen im Dach ({@code +-rot}, y+2) sind keine Phantomzellen,
 * sondern {@link BlockDummyableBeam} ({@code dummy_beam}), deren Licht die BE schaltet (Original: Blockwechsel
 * {@code hydrobay} <-> {@code dummy_beam}). Anschluesse: Fluid an beiden Seitenzellen (+-rot*2), Strom oben in der Mitte.
 * Hitbox = Original-{@code bounding}. {@code canSustainPlant}: Kern und seine waagrechten Nachbarn tragen Pflanzen
 * (s. {@link HydroponicSoil}).
 * Original-Klasse: MachineHydroponic
 */
public class MachineHydroponic extends DummyableMachineBlock implements ILookOverlay {

    /** Original {@code bounding} (Mitte, Seiten, Seiten-Sockel). */
    private static final Map<Direction, VoxelShape> BOUNDING = MultiblockStructureHelper.boundingMasters(new double[][] {
            // Middle
            {-0.5D, 0D, -1.5D, 0.5D, 3D, 1.5D},
            // Sides
            {-1.125D, 1D, -2.5D, 1.125D, 3D, -1.5D},
            {-1.125D, 1D, 1.5D, 1.125D, 3D, 2.5D},
            // Side butts
            {-0.75D, 0D, -2.5D, 0.75D, 1D, -1.5D},
            {-0.75D, 0D, 1.5D, 0.75D, 1D, 2.5D},
    });

    public MachineHydroponic(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getCustomMasterVoxelShape(BlockState state) {
        return BOUNDING.get(state.getValue(FACING));
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        DummyableStructureBuilder b = DummyableStructureBuilder.create();
        // Front/Back
        b.boxAt(1, 2, 2, 0, 1, 0, 0, 0, 4);
        b.boxAt(-1, 2, 2, 0, 1, 0, 0, 0, 4);
        // Top (ohne die beiden Lampenzellen +-rot, die setzt BlockDummyableBeam)
        b.cell(0, 2, 2);
        b.cell(0, 2, -2);
        // Side caps
        b.boxAt(0, 1, 2, 1, 0, 1, 1, 0, 0);
        b.boxAt(0, 1, -2, 1, 0, 1, 1, 0, 0);
        b.box(2, 0, 0, 0, -2, 2);
        b.box(2, 0, 0, 0, 2, -2);
        // Top connector needs to point laterally to avoid disconnecting when light blocks are added
        b.boxAt(1, 2, 0, 0, 0, 1, 0, 0, 0);
        // Base
        b.box(0, 0, 0, 0, 2, 2);

        b.extra(0, 0, 2);
        b.extra(0, 0, -2);
        b.extra(0, 2, 0);
        return b.placementOffset(0).build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    /** Lampenzellen ({@code xCoord +- rot, yCoord + 2}). */
    public static BlockPos[] lampCells(BlockPos core, Direction dir) {
        return new BlockPos[] { ChemUtil.offset(core, dir, 0, -1, 2), ChemUtil.offset(core, dir, 0, 1, 2) };
    }

    /** Pflanzfelder ({@code xCoord + rot * (i - 1), yCoord + 1}). */
    public static BlockPos plantCell(BlockPos core, Direction dir, int i) {
        return ChemUtil.offset(core, dir, 0, i - 1, 1);
    }

    /** Original {@code checkRequirement}: zusaetzlich {@code checkSpace {2, -1, 1, 1, 2, 2}} (Pflanz- und Lampenfelder). */
    @Override
    public boolean canSurviveMultiblockPlacement(BlockState state, LevelReader level, BlockPos pos) {
        if (level.getBlockState(pos).is(this)) return true;
        Direction dir = state.getValue(FACING);
        for (int i = 0; i < 3; i++) {
            if (!level.getBlockState(plantCell(pos, dir, i)).canBeReplaced()) return false;
        }
        for (BlockPos lamp : lampCells(pos, dir)) {
            if (!level.getBlockState(lamp).canBeReplaced()) return false;
        }
        return true;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!state.is(oldState.getBlock()) && !level.isClientSide()) {
            Direction dir = state.getValue(FACING);
            for (BlockPos lamp : lampCells(pos, dir)) {
                level.setBlock(lamp, ChemMachines.DUMMY_BEAM.get().defaultBlockState(), 3);
            }
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        boolean removed = !state.is(newState.getBlock()) && !level.isClientSide();
        super.onRemove(state, level, pos, newState, isMoving);
        if (removed) {
            for (BlockPos lamp : lampCells(pos, state.getValue(FACING))) {
                if (level.getBlockState(lamp).getBlock() instanceof BlockDummyableBeam) {
                    level.setBlock(lamp, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityHydroponic(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ChemMachines.HYDROBAY_BE.get(), TileEntityHydroponic::tick);
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return ChemMachines.standardOpen(level, pos, player);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return ChemMachines.standardOpen(level, pos, player);
    }
    *///?}

    /** Original {@code printHook}: Strom (rot bis 200 HE), CO2 hinein, O2 hinaus. */
    @Override
    public void printHook(GuiGraphics g, Level world, BlockPos pos) {
        if (!(world.getBlockEntity(pos) instanceof TileEntityHydroponic hydro)) return;

        List<Component> text = ChemUtil.lines();
        text.add(ChemUtil.powerLine(hydro.getEnergyStored(), hydro.getEnergyStored() <= 200));
        text.add(ChemUtil.tankLine(hydro.tanks[0], true));
        text.add(ChemUtil.tankLine(hydro.tanks[1], false));

        ILookOverlay.printGeneric(g, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }

    /** Original {@code addInformation} -> {@code addStandardInfo} ({@code tile.hydrobay.desc}). */
    @Override
    //? if < 1.21.1 {
    public void appendHoverText(ItemStack stack, @Nullable net.minecraft.world.level.BlockGetter level,
                                List<Component> list, net.minecraft.world.item.TooltipFlag flag) {
    //?} else {
    /*public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext hbmTooltipCtx, List<Component> list, net.minecraft.world.item.TooltipFlag flag) {
    *///?}
        com.hbm_m.util.StandardInfo.add(list, getDescriptionId() + ".desc");
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineHydroponic> CODEC = simpleCodec(MachineHydroponic::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
