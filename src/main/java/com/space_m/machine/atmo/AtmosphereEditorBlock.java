package com.space_m.machine.atmo;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.block.IToolable;
import com.hbm_m.interfaces.IItemFluidIdentifier;
import com.hbm_m.interfaces.ILookOverlay;
import com.hbm_m.inventory.fluid.FluidType;
import com.space_m.blockentity.SpaceBlockEntities;
import com.space_m.celestial.AstronomyUtil;
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
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Atmosphere Editor, 1:1 NTM Space {@code BlockAtmosphereEditor} (Kreativ-/Debugblock, {@code BlockContainer}):
 * Schraubenzieher an/aus, Handbohrer Durchsatz +1 (schleichend -1, Exponent zur Basis 10), Entschaerfer Abgabe/Aufnahme,
 * Fluid-Identifikator waehlt das Gas. Laeuft er, gibt er alle 5 Ticks {@code 10^throughput} mB des Gases an die
 * Koerper-Atmosphaere ab bzw. nimmt sie auf ({@code FT_Gaseous.release/capture}).
 * Original-Klasse: BlockAtmosphereEditor
 */
public class AtmosphereEditorBlock extends BaseEntityBlock implements IToolable, ILookOverlay {

    public AtmosphereEditorBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AtmosphereEditorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, SpaceBlockEntities.ATMOSPHERE_EDITOR.get(), AtmosphereEditorBlockEntity::tick);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /** Original {@code addInformation} (ITooltipProvider). */
    public static void addInformation(List<Component> list) {
        list.add(Component.literal("Use screwdriver to turn on and off").withStyle(ChatFormatting.GOLD));
        list.add(Component.literal("Use hand drill to increase/decrease throughput").withStyle(ChatFormatting.GOLD));
        list.add(Component.literal("Use defuser to switch emission/capture mode").withStyle(ChatFormatting.GOLD));
        list.add(Component.literal("Use fluid identifier to change fluid").withStyle(ChatFormatting.GOLD));
    }

    //? if < 1.21.1 {
    @Override
    public void appendHoverText(ItemStack stack, @Nullable net.minecraft.world.level.BlockGetter level, List<Component> list, TooltipFlag flag) {
        addInformation(list);
    }
    //?} else {
    /*@Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context, List<Component> list, TooltipFlag flag) {
        addInformation(list);
    }
    *///?}

    @Override
    public void printHook(GuiGraphics g, Level world, BlockPos pos) {
        if (CelestialBody.inOrbit(world)) return;
        if (!(world.getBlockEntity(pos) instanceof AtmosphereEditorBlockEntity editor)) return;

        CBT_Atmosphere atmosphere = CelestialBody.getTrait(world, CBT_Atmosphere.class);
        double pressure = atmosphere != null ? atmosphere.getPressure(AtmoFluids.id(editor.fluid)) : 0;
        if (pressure < 0.0001) pressure = 0;
        pressure = Math.round(pressure * 1_000.0) / 1_000.0;

        List<Component> text = new ArrayList<>();
        text.add(Component.literal("State: " + (editor.isOn ? "RUNNING" : "OFF")));
        text.add(Component.literal("Current gas: ").append(FluidType.forFluid(editor.fluid).getLocalizedName()).append(" - " + pressure));
        text.add(Component.literal("Current mode: " + (editor.isEmitting ? "EMITTING" : "CAPTURING")));
        text.add(Component.literal("Current throughput: " + Math.pow(10, editor.throughputFactor) / AstronomyUtil.MB_PER_ATM));

        ILookOverlay.printGeneric(g, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }

    @Override
    public boolean onScrew(Level world, Player player, BlockPos pos, Direction side, float fX, float fY, float fZ, InteractionHand hand, ToolType tool) {
        if (!(world.getBlockEntity(pos) instanceof AtmosphereEditorBlockEntity editor)) return false;

        if (tool == ToolType.SCREWDRIVER) {
            if (!world.isClientSide) {
                editor.isOn = !editor.isOn;
                editor.markChanged();
            }
            return true;
        } else if (tool == ToolType.HAND_DRILL) {
            if (!world.isClientSide) {
                editor.throughputFactor += (player.isShiftKeyDown() ? -1 : 1);
                editor.markChanged();
            }
            return true;
        } else if (tool == ToolType.DEFUSER) {
            if (!world.isClientSide) {
                editor.isEmitting = !editor.isEmitting;
                editor.markChanged();
            }
            return true;
        }

        return false;
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

    /** Original {@code onBlockActivated}: Client immer verbraucht; Server nur mit Identifikator. */
    private InteractionResult onActivated(Level level, BlockPos pos, Player player, ItemStack held) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(held.getItem() instanceof IItemFluidIdentifier identifier)) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof AtmosphereEditorBlockEntity editor)) return InteractionResult.PASS;

        var type = identifier.getType(level, pos, held);
        editor.fluid = type;
        editor.markChanged();
        player.displayClientMessage(Component.literal("Changed type to ").withStyle(ChatFormatting.YELLOW)
                .append(FluidType.forFluid(type).getLocalizedName()).append(Component.literal("!")), false);
        return InteractionResult.CONSUME;
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<AtmosphereEditorBlock> CODEC = simpleCodec(AtmosphereEditorBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
