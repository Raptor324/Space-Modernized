package com.space_m.orbit.propulsion;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.interfaces.ILookOverlay;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.item.ModItems;
import com.space_m.celestial.CelestialBody;
import com.space_m.orbit.BlockOrbitalStation;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1:1 NTM Space {@code BlockOrbitalStationLauncher} ({@code orbital_station_launcher}, "Orbital Station Launching Port"):
 * gleiche Struktur wie der Stationsport ({@code BlockOrbitalStation}, R6-A), aber mit
 * {@link TileEntityOrbitalStationLauncher}. Nur im Orbit; Randzellen ignorieren den Klick (Platzieren erlaubt); mit
 * angedockter Rakete steigt man ein, sonst GUI ({@code standardOpenBehavior}, geschlichen: nichts). Beim Abbau werden
 * die Laufwerk-Geisterslots nicht fallengelassen ({@code isBreaking}).
 * Original-Klasse: BlockOrbitalStationLauncher
 */
public class BlockOrbitalStationLauncher extends BlockOrbitalStation {

    public BlockOrbitalStationLauncher(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityOrbitalStationLauncher(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, PropulsionBlockEntities.ORBITAL_STATION_LAUNCHER.get(), TileEntityOrbitalStationLauncher::tick);
    }

    @Override
    public boolean forwardPartExplosions() {
        return false;
    }

    /** Original {@code breakBlock}: {@code isBreaking = true} vor dem Fallenlassen des Inventars. */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof TileEntityOrbitalStationLauncher te) {
            te.isBreaking = true;
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    protected InteractionResult onActivated(Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!CelestialBody.inOrbit(world)) return InteractionResult.PASS;

        // If activating the side blocks, ignore, to allow placing
        if (isSideCell(pos, clickedCell(hit))) return InteractionResult.PASS;

        if (!(world.getBlockEntity(pos) instanceof TileEntityOrbitalStationLauncher station)) return InteractionResult.PASS;

        if (world.isClientSide) return InteractionResult.SUCCESS;

        if (station.hasDocked) {
            if (!station.hasRider) {
                station.enterCapsule(player);
            }
            return InteractionResult.SUCCESS;
        }

        // standardOpenBehavior: geschlichen -> nichts
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        MenuRegistry.openExtendedMenu((ServerPlayer) player, station, buf -> buf.writeBlockPos(pos));
        return InteractionResult.SUCCESS;
    }

    @Override
    public void printHook(GuiGraphics g, Level world, BlockPos pos) {
        Component title = Component.translatable(getDescriptionId());

        if (!CelestialBody.inOrbit(world)) {
            List<Component> text = new ArrayList<>();
            text.add(PropulsionUtil.blinkWarning("atmosphere.noOrbit"));
            ILookOverlay.printGeneric(g, title, 0xffff00, 0x404000, text);
            return;
        }

        if (!(world.getBlockEntity(pos) instanceof TileEntityOrbitalStationLauncher pad)) return;

        List<Component> text = new ArrayList<>();
        if (pad.hasDocked) {
            if (!pad.hasRider) {
                text.add(Component.translatable("station.enterRocket"));
            } else {
                text.add(Component.translatable("station.occupiedRocket").withStyle(ChatFormatting.YELLOW));
            }

            ILookOverlay.printGeneric(g, title, 0xffff00, 0x404000, text);
            return;
        }

        if (pad.rocket == null || !pad.rocket.validate()) return;

        text.add(Component.literal("Required fuels:"));

        for (FluidTank tank : pad.tanks) {
            if (tank.getTankType() == com.hbm_m.inventory.fluid.ModFluids.NONE.getSource()) continue;
            text.add(PropulsionUtil.tankLine(tank, true));
        }

        if (pad.solidFuel.max > 0) {
            text.add(Component.literal("-> ").withStyle(ChatFormatting.GREEN)
                    .append(Component.translatable(ModItems.ROCKET_FUEL.get().getDescriptionId()).withStyle(ChatFormatting.RESET))
                    .append(Component.literal(": " + pad.solidFuel.level + "/" + pad.solidFuel.max + "kg").withStyle(ChatFormatting.RESET)));
        }

        if (text.size() <= 1) return;

        ILookOverlay.printGeneric(g, title, 0xffff00, 0x404000, text);
    }

    /** Original {@code addInformation}: {@code addStandardInfo} (Umschalttaste zeigt {@code .desc}). */
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
    /*public static final com.mojang.serialization.MapCodec<BlockOrbitalStationLauncher> CODEC = simpleCodec(BlockOrbitalStationLauncher::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
