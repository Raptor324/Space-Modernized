package com.space_m.machine.atmo;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.interfaces.IItemFluidIdentifier;
import com.hbm_m.interfaces.ILookOverlay;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.util.BobMathUtil;
import com.space_m.blockentity.SpaceBlockEntities;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
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

/**
 * Atmospheric Emitter, 1:1 NTM Space {@code MachineAtmosphericEmitter}: {@code getDimensions {2, 0, 1, 0, 1, 0}},
 * Offset 0; {@code makeExtra} (nach Aufloesung der gemischten Achsen im Original) bei Kern - dir + rot, Kern + rot und
 * Kern - dir - die drei uebrigen Sockelfelder ({@code TileEntityProxyCombo().power().fluid()}). Fluid-Identifikator
 * (nicht schleichend) stellt den Tank auf ein Gas ({@code FT_Gaseous}/{@code FT_Gaseous_ART}).
 * Darstellung {@code RenderAtmosphericEmitter} (BER, Modell {@code outtake.obj}, Luefter).
 * Original-Klasse: MachineAtmosphericEmitter
 */
public class AtmosphericEmitterBlock extends DummyableMachineBlock implements ILookOverlay {

    public AtmosphericEmitterBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        return DummyableStructureBuilder.create()
                .box(2, 0, 1, 0, 1, 0)
                .extra(-1, 0, 1)
                .extra(0, 0, 1)
                .extra(-1, 0, 0)
                .placementOffset(0)
                .build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AtmosphericEmitterBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, SpaceBlockEntities.ATMO_EMITTER.get(), AtmosphericEmitterBlockEntity::tick);
    }

    /** Original {@code getRenderType() = -1} (TESR). */
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public void printHook(GuiGraphics g, Level world, BlockPos pos) {
        if (!(world.getBlockEntity(pos) instanceof AtmosphericEmitterBlockEntity drain)) return;

        List<Component> text = new ArrayList<>();
        text.add(Component.literal("Power: " + BobMathUtil.getShortNumber(drain.getEnergyStored()) + "HE")
                .withStyle(drain.getEnergyStored() < Math.max(drain.tank.getFill() / 2, 1) * 10L ? ChatFormatting.RED : ChatFormatting.GREEN));
        text.add(Component.literal("-> ").withStyle(ChatFormatting.GREEN)
                .append(Component.literal("").withStyle(ChatFormatting.RESET)
                        .append(FluidType.forFluid(drain.tank.getTankType()).getLocalizedName())
                        .append(": " + drain.tank.getFill() + "/" + drain.tank.getMaxFill() + "mB")));

        ILookOverlay.printGeneric(g, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return onActivated(level, pos, player, player.getItemInHand(hand));
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return onActivated(level, pos, player, player.getItemInHand(InteractionHand.MAIN_HAND));
    }
    *///?}

    /**
     * Original {@code onBlockActivated}: auf dem Server ohne Schleichen nur mit Identifikator (Gas -> Typwechsel +
     * Chatmeldung, sonst nichts, aber verbraucht); ohne Identifikator nicht verbraucht. Client/Schleichen: verbraucht.
     */
    private InteractionResult onActivated(Level level, BlockPos pos, Player player, ItemStack held) {
        if (level.isClientSide || player.isShiftKeyDown()) return InteractionResult.sidedSuccess(level.isClientSide);
        if (!(held.getItem() instanceof IItemFluidIdentifier identifier)) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof AtmosphericEmitterBlockEntity drain)) return InteractionResult.PASS;

        var type = identifier.getType(level, pos, held);
        if (AtmoFluids.isGas(type)) {
            drain.tank.setTankType(type);
            drain.setChanged();
            player.displayClientMessage(Component.literal("Changed type to ").withStyle(ChatFormatting.YELLOW)
                    .append(FluidType.forFluid(type).getLocalizedName()).append(Component.literal("!")), false);
        }
        return InteractionResult.CONSUME;
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<AtmosphericEmitterBlock> CODEC = simpleCodec(AtmosphericEmitterBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
