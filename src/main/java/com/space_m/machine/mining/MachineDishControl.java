package com.space_m.machine.mining;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 NTM Space {@code MachineDishControl} ({@code dish_control}, "StarDar Remote Access Console"): {@code getDimensions
 * {0, 0, 0, 0, 0, 1}}, Offset 0 (zwei Bloecke, keine Anschluesse). Rechtsklick (nicht geschlichen) oeffnet die GUI des
 * verknuepften StarDar ({@code stardarBlock.onBlockActivated} am StarDar-Kern); ohne Verknuepfung "Dish not linked!",
 * ohne StarDar am Ziel "Missing control target!" (Chat, Name dunkeltuerkis, Text rot). Tooltip {@code addStandardInfo}.
 * Original-Klasse: MachineDishControl
 */
public class MachineDishControl extends DummyableMachineBlock {

    public MachineDishControl(Properties properties) {
        super(properties);
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        return DummyableStructureBuilder.create()
                .box(0, 0, 0, 0, 0, 1)
                .placementOffset(0)
                .build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityDishControl(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, DishMachines.DISH_CONTROL_BE.get(), TileEntityDishControl::tick);
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return onActivated(level, pos, player, hand, hit);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return onActivated(level, pos, player, InteractionHand.MAIN_HAND, hit);
    }
    *///?}

    private InteractionResult onActivated(Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (world.isClientSide) {
            return InteractionResult.SUCCESS;
        } else if (!player.isShiftKeyDown()) {
            if (!(world.getBlockEntity(pos) instanceof TileEntityDishControl control)) return InteractionResult.PASS;

            // Check if a dish was not assigned
            if (!control.isLinked) {
                player.sendSystemMessage(message(Component.literal("Dish not linked!")));
                return InteractionResult.PASS;
            }

            // also check that you're still pointing at a stardar...
            BlockPos link = control.linkPosition;
            BlockState stardar = world.getBlockState(link);
            if (!DishMachines.isStardar(stardar)) {
                player.sendSystemMessage(message(Component.literal("Missing control target!")));
                return InteractionResult.PASS;
            }

            // Trigger the StarDar UI to open
            BlockHitResult stardarHit = new BlockHitResult(Vec3.atCenterOf(link), hit.getDirection(), link, false);
            //? if < 1.21.1 {
            return stardar.use(world, player, hand, stardarHit);
            //?} else {
            /*return stardar.useWithoutItem(world, player, stardarHit);
            *///?}
        } else {
            return InteractionResult.PASS;
        }
    }

    /** {@code "[" + name + "] " + text} (Klammern/Name dunkeltuerkis, Text rot). */
    private Component message(Component text) {
        return Component.literal("[").withStyle(ChatFormatting.DARK_AQUA)
                .append(Component.translatable(getDescriptionId()).withStyle(ChatFormatting.DARK_AQUA))
                .append(Component.literal("] ").withStyle(ChatFormatting.DARK_AQUA))
                .append(text.copy().withStyle(ChatFormatting.RED));
    }

    /** Original {@code addInformation} -> {@code addStandardInfo} ({@code tile.dish_control.desc}). */
    @Override
    //? if < 1.21.1 {
    public void appendHoverText(ItemStack stack, @Nullable net.minecraft.world.level.BlockGetter level,
                                List<Component> list, net.minecraft.world.item.TooltipFlag flag) {
    //?} else {
    /*public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext hbmTooltipCtx, List<Component> list, net.minecraft.world.item.TooltipFlag flag) {
    *///?}
        com.hbm_m.util.StandardInfo.add(list, getDescriptionId() + ".desc");
    }

    /** Unbenutzt (Original: Kern auf der Zelle selbst). */
    static Direction facing(BlockState state) {
        return state.getValue(FACING);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineDishControl> CODEC = simpleCodec(MachineDishControl::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
