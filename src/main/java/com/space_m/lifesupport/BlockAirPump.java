package com.space_m.lifesupport;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.FluidLocalization;
import com.hbm_m.interfaces.IItemFluidIdentifier;
import com.hbm_m.interfaces.ILookOverlay;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.trait.FluidTraitSimple.FT_Gaseous;
import com.hbm_m.inventory.fluid.trait.FluidTraitSimple.FT_Gaseous_ART;
import com.hbm_m.util.BobMathUtil;
import com.space_m.atmosphere.IBlockSealable;
import com.space_m.blockentity.SpaceBlockEntities;
import com.space_m.celestial.trait.CBT_Atmosphere;
import com.space_m.celestial.trait.CBT_Atmosphere.FluidEntry;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1:1 NTM Space {@code BlockAirPump} ({@code air_vent}, "Air Vent"): Vollblock (oben {@code vent_chlorine_seal_top},
 * Seiten {@code vent_chlorine_seal_side}), dichtet selbst nicht ab ({@link IBlockSealable#isSealed} = false, damit der
 * Raum vom Block aus geflutet wird). Rechtsklick mit Fluid-Identifier setzt ein gasfoermiges Gas
 * ({@code FT_Gaseous}/{@code FT_Gaseous_ART}). Blick-Overlay: Tank, Warnungen, Raumatmosphaere, Recycling.
 * Original-Klasse: BlockAirPump
 */
public class BlockAirPump extends BaseEntityBlock implements ILookOverlay, IBlockSealable {

    public BlockAirPump(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityAirPump(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, SpaceBlockEntities.AIR_VENT.get(), TileEntityAirPump::tick);
    }

    @Override
    public void printHook(net.minecraft.client.gui.GuiGraphics guiGraphics, Level world, BlockPos pos) {

        BlockEntity tile = world.getBlockEntity(pos);

        if (!(tile instanceof TileEntityAirPump pump)) return;

        CBT_Atmosphere atmosphere = pump.currentAtmosphere;

        List<Component> text = new ArrayList<>();

        text.add(FluidLocalization.nameFromFluidId(BuiltInRegistries.FLUID.getKey(pump.tank.getTankType())).copy()
                .append(": " + pump.tank.getFill() + "/" + pump.tank.getMaxFill() + "mB"));

        int blink = BobMathUtil.getBlink() ? 0xff0000 : 0xffff00;
        if (pump.tank.getFill() <= 10) {
            text.add(Component.literal("! ! ! ").append(Component.translatable("atmosphere.noTank")).append(" ! ! !")
                    .withStyle(s -> s.withColor(TextColor.fromRgb(blink))));
        } else if (!pump.hasSeal()) {
            text.add(Component.literal("! ! ! ").append(Component.translatable("atmosphere.noSeal")).append(" ! ! !")
                    .withStyle(s -> s.withColor(TextColor.fromRgb(blink))));
        }

        text.add(Component.translatable("atmosphere.name").append(": "));

        boolean hasPressure = false;

        if (atmosphere != null) {
            for (FluidEntry entry : atmosphere.fluids) {
                if (entry.pressure > 0.01) {
                    double pressure = BobMathUtil.roundDecimal(entry.pressure, 3);
                    text.add(Component.literal(" - ").append(FluidLocalization.nameFromFluidId(entry.fluid))
                            .append(" - " + pressure + "atm").withStyle(ChatFormatting.AQUA));
                    hasPressure = true;
                }
            }
        }

        if (!hasPressure) {
            text.add(Component.literal(" - ").append(Component.translatable("atmosphere.vacuum")).withStyle(ChatFormatting.AQUA));
        }

        if (pump.isRecycling()) {
            text.add(Component.translatable("atmosphere.recycling").withStyle(ChatFormatting.GREEN));
        }

        LookOverlayText.print(guiGraphics, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return onBlockActivated(world, pos, player, player.getItemInHand(hand));
    }
    //?} else {
    /*@Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        InteractionResult r = onBlockActivated(world, pos, player, stack);
        return r == InteractionResult.FAIL ? net.minecraft.world.ItemInteractionResult.FAIL : net.minecraft.world.ItemInteractionResult.sidedSuccess(world.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        return onBlockActivated(world, pos, player, ItemStack.EMPTY);
    }
    *///?}

    /** Original {@code onBlockActivated}: Server immer "behandelt"; ohne Pumpe false. */
    private InteractionResult onBlockActivated(Level world, BlockPos pos, Player player, ItemStack held) {
        if (world.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity tile = world.getBlockEntity(pos);

        if (!(tile instanceof TileEntityAirPump pump)) return InteractionResult.FAIL;

        if (!held.isEmpty() && held.getItem() instanceof IItemFluidIdentifier id) {
            Fluid type = id.getType(world, pos, held);
            if (type != null && (FluidType.hasTrait(type, FT_Gaseous.class) || FluidType.hasTrait(type, FT_Gaseous_ART.class))) {
                pump.tank.setTankType(type);
                pump.setChanged();
                player.sendSystemMessage(Component.literal("Changed type to ").withStyle(ChatFormatting.YELLOW)
                        .append(FluidLocalization.nameFromFluidId(BuiltInRegistries.FLUID.getKey(type)))
                        .append(Component.literal("!")));
            }
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public boolean isSealed(Level world, BlockPos pos) {
        return false;
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<BlockAirPump> CODEC = simpleCodec(BlockAirPump::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
