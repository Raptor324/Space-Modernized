package com.space_m.machine.atmo;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.block.IToolable;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.interfaces.IItemFluidIdentifier;
import com.hbm_m.interfaces.ILookOverlay;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.util.BobMathUtil;
import com.space_m.blockentity.SpaceBlockEntities;
import com.space_m.celestial.CelestialBody;
import com.space_m.celestial.trait.CBT_Atmosphere;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
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

/**
 * Atmospheric Compressor ({@code machine_atmo_vent}), 1:1 NTM Space {@code BlockAtmosphericCompressor}:
 * {@code getDimensions {3, 0, 1, 0, 0, 1}}, Offset 0, {@code makeExtra} wie der Turm (Strom + Fluid an den drei
 * Sockelfeldern). Schraubenzieher: naechstes Gas der Atmosphaere ({@code cycleGas}); Fluid-Identifikator: Gas waehlen,
 * wenn die Atmosphaere es enthaelt ({@code switchGas}). Overlay zeigt im Vakuum {@code CANNOT COLLECT IN VACUUM}.
 * Darstellung {@code RenderAtmosphericCompressor} (BER, drehender Luefter).
 * Original-Klasse: BlockAtmosphericCompressor
 */
public class AtmosphericCompressorBlock extends DummyableMachineBlock implements ILookOverlay, IToolable {

    public AtmosphericCompressorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        return DummyableStructureBuilder.create()
                .box(3, 0, 1, 0, 0, 1)
                .extra(-1, 0, -1)
                .extra(-1, 0, 0)
                .extra(0, 0, -1)
                .placementOffset(0)
                .build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AtmosphericCompressorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, SpaceBlockEntities.ATMOSPHERIC_VENT.get(), AtmosphericCompressorBlockEntity::tick);
    }

    /** Original {@code getRenderType() = -1} (TESR). */
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public void printHook(GuiGraphics g, Level world, BlockPos pos) {
        if (!(world.getBlockEntity(pos) instanceof AtmosphericCompressorBlockEntity tower)) return;

        List<Component> text = new ArrayList<>();
        if (!CelestialBody.hasTrait(world, CBT_Atmosphere.class)) {
            text.add(Component.literal("ERROR: ").withStyle(ChatFormatting.RED)
                    .append(Component.literal("CANNOT COLLECT IN VACUUM").withStyle(ChatFormatting.RESET)));
        } else {
            text.add(Component.literal("Power: " + BobMathUtil.getShortNumber(tower.getEnergyStored()) + "HE")
                    .withStyle(tower.getEnergyStored() < tower.getMaxEnergyStored() / 20 ? ChatFormatting.RED : ChatFormatting.GREEN));
            text.add(Component.literal("<- ").withStyle(ChatFormatting.RED)
                    .append(Component.literal("").withStyle(ChatFormatting.RESET)
                            .append(FluidType.forFluid(tower.tank.getTankType()).getLocalizedName())
                            .append(": " + tower.tank.getFill() + "/" + tower.tank.getMaxFill() + "mB")));
        }

        ILookOverlay.printGeneric(g, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }

    /** Original {@code onScrew}: nur Schraubenzieher, wechselt auf das naechste Gas der Atmosphaere. */
    @Override
    public boolean onScrew(Level world, Player player, BlockPos pos, Direction side, float fX, float fY, float fZ, InteractionHand hand, ToolType tool) {
        if (tool != ToolType.SCREWDRIVER) return false;
        if (world.isClientSide) return true;
        if (!(world.getBlockEntity(pos) instanceof AtmosphericCompressorBlockEntity tile)) return false;

        tile.cycleGas();
        tile.setChanged();
        return true;
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

    /** Original {@code onBlockActivated}: Schleichen/kein Identifikator -> nichts; sonst Gas wechseln + Chatmeldung. */
    private InteractionResult onActivated(Level level, BlockPos pos, Player player, ItemStack held) {
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        if (!(held.getItem() instanceof IItemFluidIdentifier identifier)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;

        if (!(level.getBlockEntity(pos) instanceof AtmosphericCompressorBlockEntity compressor)) return InteractionResult.CONSUME;

        var type = identifier.getType(level, pos, held);
        if (compressor.switchGas(type)) {
            compressor.setChanged();
            player.displayClientMessage(Component.literal("Changed type to ").withStyle(ChatFormatting.YELLOW)
                    .append(FluidType.forFluid(type).getLocalizedName()).append(Component.literal("!")), false);
        }
        return InteractionResult.CONSUME;
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<AtmosphericCompressorBlock> CODEC = simpleCodec(AtmosphericCompressorBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
