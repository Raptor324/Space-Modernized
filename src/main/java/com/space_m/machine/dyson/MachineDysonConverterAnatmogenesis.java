package com.space_m.machine.dyson;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.block.IToolable;
import com.hbm_m.api.fluids.FluidLocalization;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.interfaces.IItemFluidIdentifier;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.space_m.celestial.AstronomyUtil;
import com.space_m.celestial.CelestialBody;
import com.space_m.celestial.trait.CBT_Atmosphere;
import com.space_m.machine.atmo.AtmoFluids;
import com.space_m.machine.misc.R7MachineBlock;

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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

/**
 * 1:1 NTM Space {@code MachineDysonConverterAnatmogenesis} ({@code dyson_converter_anatmogenesis}):
 * {@code getDimensions {2, 0, 5, 5, 1, 1}}, Offset 5. Rechtsklick mit Fluid-Identifikator: Gas waehlen (nur
 * {@code FT_Gaseous}/{@code FT_Gaseous_ART}, Chat "Changed type to X!"), Schraubenzieher: Ausstossen/Einfangen umschalten.
 * Overlay: Rate in atm/h, aktuelles Gas mit Teildruck, Modus.
 * Original-Klasse: MachineDysonConverterAnatmogenesis
 */
public class MachineDysonConverterAnatmogenesis extends R7MachineBlock implements IToolable {

    public MachineDysonConverterAnatmogenesis(Properties properties) {
        super(properties);
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        return DummyableStructureBuilder.create().box(2, 0, 5, 5, 1, 1)
                .placementOffset(5).build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityDysonConverterAnatmogenesis(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, DysonBlockEntities.CONVERTER_ANATMOGENESIS.get(), TileEntityDysonConverterAnatmogenesis::tick);
    }

    @Override
    public void printHook(GuiGraphics g, Level world, BlockPos pos) {
        if (!(world.getBlockEntity(pos) instanceof TileEntityDysonConverterAnatmogenesis converter)) return;

        CBT_Atmosphere atmosphere = CelestialBody.getTrait(world, CBT_Atmosphere.class);
        double pressure = atmosphere != null ? atmosphere.getPressure(AtmoFluids.id(converter.fluid)) : 0;
        if (pressure < 0.0001) pressure = 0;
        pressure = Math.round(pressure * 1_000.0) / 1_000.0;

        List<Component> text = new ArrayList<>();

        text.add(Component.literal("Current rate: " + ((double) converter.gasProduced * 20 * 60 * 60 / AstronomyUtil.MB_PER_ATM) + "atm per hour"));
        text.add(Component.literal("Current gas: " + FluidLocalization.nameFromFluidId(AtmoFluids.id(converter.fluid)).getString() + " - " + pressure));
        text.add(Component.literal("Current mode: " + (converter.isEmitting ? "EMITTING" : "CAPTURING")));

        print(g, text);
    }

    @Override
    public boolean onScrew(Level world, Player player, BlockPos pos, Direction side, float fX, float fY, float fZ, InteractionHand hand, ToolType tool) {
        if (tool != ToolType.SCREWDRIVER) return false;

        if (world.isClientSide) return true;

        if (!(world.getBlockEntity(pos) instanceof TileEntityDysonConverterAnatmogenesis converter)) return false;

        converter.isEmitting = !converter.isEmitting;
        converter.setChanged();

        return true;
    }

    @Override
    protected InteractionResult onActivated(Level world, BlockPos pos, BlockState state, Player player, ItemStack held, InteractionHand hand) {
        if (world.isClientSide)
            return held.getItem() instanceof IItemFluidIdentifier ? InteractionResult.SUCCESS : InteractionResult.PASS;

        if (!held.isEmpty() && held.getItem() instanceof IItemFluidIdentifier identifier) {
            if (!(world.getBlockEntity(pos) instanceof TileEntityDysonConverterAnatmogenesis converter)) return InteractionResult.FAIL;

            Fluid type = identifier.getType(world, pos, held);
            if (AtmoFluids.isGas(type)) {
                converter.fluid = type;
                converter.setChanged();
                player.sendSystemMessage(Component.literal("Changed type to ").withStyle(ChatFormatting.YELLOW)
                        .append(FluidLocalization.nameFromFluidId(AtmoFluids.id(type))).append(Component.literal("!")));
            }

            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineDysonConverterAnatmogenesis> CODEC = simpleCodec(MachineDysonConverterAnatmogenesis::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
