package com.space_m.lifesupport;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.interfaces.ILookOverlay;
import com.hbm_m.interfaces.IMultiblockController;
import com.hbm_m.multiblock.MultiblockSideTuples;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.multiblock.PartRole;
import com.hbm_m.util.BobMathUtil;
import com.space_m.blockentity.SpaceBlockEntities;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 1:1 NTM Space {@code BlockAirScrubber} ({@code air_scrubber}, "CO2 Scrubber"): {@code BlockDummyable} mit
 * {@code getDimensions {1,0,0,0,0,0}}, Offset 0 - Kern unten, ein Teil darueber ({@code TileEntityProxyCombo(false,
 * power, fluid)} -> HBM {@code UNIVERSAL_CONNECTOR}, alle Seiten). Shift-Info {@code .desc}, Blick-Overlay Strom + Tank.
 * Als Dummyable dichtet er keinen Raum ab (Original {@code AtmosphereBlob.isBlockSealed}: "Machines can't seal").
 * Original-Klasse: BlockAirScrubber
 */
public class BlockAirScrubber extends BaseEntityBlock implements IMultiblockController, ILookOverlay {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    private final MultiblockStructureHelper structureHelper;

    public BlockAirScrubber(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
        this.structureHelper = defineStructure();
    }

    private static MultiblockStructureHelper defineStructure() {
        String[] layer0 = { "C" };
        String[] layer1 = { "B" };

        Map<Character, PartRole> roleMap = Map.of(
            'C', PartRole.CONTROLLER,
            'B', PartRole.UNIVERSAL_CONNECTOR
        );

        Map<Character, Supplier<BlockState>> symbolMap = Map.of();

        Map<Character, boolean[]> energySideMap = Map.of(
            'B', MultiblockSideTuples.energy(true, true, true, true, true, true),
            'C', MultiblockSideTuples.energy(true, true, true, true, true, true)
        );
        Map<Character, boolean[]> fluidSideMap = Map.of(
            'B', MultiblockSideTuples.fluid(true, true, true, true, true, true),
            'C', MultiblockSideTuples.fluid(true, true, true, true, true, true)
        );

        return MultiblockStructureHelper.createFromLayersWithRolesAndSides(
            new String[][] { layer0, layer1 },
            symbolMap,
            () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState(),
            roleMap,
            null,
            energySideMap,
            fluidSideMap
        );
    }

    @Override
    public MultiblockStructureHelper getStructureHelper() {
        return structureHelper;
    }

    @Override
    public PartRole getPartRole(BlockPos localOffset) {
        return structureHelper.resolvePartRole(localOffset, this);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!state.is(oldState.getBlock()) && !level.isClientSide()) {
            placeMultiblockStructure(level, pos, state);
        }
    }

    @Override
    public boolean canSurvive(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos) {
        return super.canSurvive(state, level, pos) && canSurviveMultiblockPlacement(state, level, pos);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide()) {
            structureHelper.destroyStructure(level, pos, state.getValue(FACING));
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return structureHelper.getControllerCellShape(state.getValue(FACING));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // Original RenderAirScrubber (TESR, Luefter dreht) -> RenderAirScrubber (BER)
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityAirScrubber(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, SpaceBlockEntities.AIR_SCRUBBER.get(), TileEntityAirScrubber::tick);
    }

    @Override
    public void printHook(net.minecraft.client.gui.GuiGraphics guiGraphics, Level world, BlockPos pos) {
        BlockEntity tile = world.getBlockEntity(pos);
        if (!(tile instanceof TileEntityAirScrubber scrubber)) return;

        List<Component> text = new ArrayList<>();

        text.add(Component.literal("Power: " + BobMathUtil.getShortNumber(scrubber.getPower()) + "HE")
                .withStyle(scrubber.getPower() <= 200 ? ChatFormatting.RED : ChatFormatting.GREEN));
        text.add(Component.empty()
                .append(Component.literal("<- ").withStyle(ChatFormatting.RED))
                .append(com.hbm_m.api.fluids.FluidLocalization.nameFromFluidId(
                        net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(scrubber.tank.getTankType())))
                .append(": " + scrubber.tank.getFill() + "/" + scrubber.tank.getMaxFill() + "mB"));

        LookOverlayText.print(guiGraphics, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }

    /** Original {@code addInformation}: {@code addStandardInfo} (Umschalttaste zeigt {@code .desc}). */
    @Override
    //? if < 1.21.1 {
    public void appendHoverText(net.minecraft.world.item.ItemStack stack, @Nullable BlockGetter level,
                                List<Component> list, net.minecraft.world.item.TooltipFlag flag) {
    //?} else {
    /*public void appendHoverText(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.Item.TooltipContext ctx, List<Component> list, net.minecraft.world.item.TooltipFlag flag) {
    *///?}
        com.hbm_m.util.StandardInfo.add(list, getDescriptionId() + ".desc");
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<BlockAirScrubber> CODEC = simpleCodec(BlockAirScrubber::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
