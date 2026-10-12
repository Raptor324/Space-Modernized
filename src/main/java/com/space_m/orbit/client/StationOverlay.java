package com.space_m.orbit.client;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.interfaces.ILookOverlay;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.util.BobMathUtil;
import com.space_m.celestial.CelestialBody;
import com.space_m.orbit.BlockOrbitalStation;
import com.space_m.orbit.TileEntityOrbitalStation;
import com.space_m.rocket.ItemCustomRocket;
import com.space_m.rocket.SpaceRocketItems;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Blick-Overlay der Stationsports, 1:1 NTM Space {@code BlockOrbitalStation.printHook}: ausserhalb des Orbits blinkend
 * "MUST BE IN ORBIT"; sonst gelagerte Gegenstaende, Hinweise zum Betreten/Einlagern/Platzieren, Hydrazin-Fuellstand.
 * Nur Client (eigene Klasse, damit der Block auf dem Server keine Client-Klassen laedt).
 */
public final class StationOverlay {

    private StationOverlay() {}

    public static void print(BlockOrbitalStation block, GuiGraphics g, Level world, BlockPos pos) {
        Component title = Component.translatable(block.getDescriptionId());

        if (!CelestialBody.inOrbit(world)) {
            List<Component> text = new ArrayList<>();
            text.add(Component.literal("! ! ! ").append(Component.translatable("atmosphere.noOrbit")).append(" ! ! !")
                    .withStyle(s -> s.withColor(BobMathUtil.getBlink() ? 0xff0000 : 0xffff00)));
            ILookOverlay.printGeneric(g, title, 0xffff00, 0x404000, text);
            return;
        }

        Player player = net.minecraft.client.Minecraft.getInstance().player;
        if (player == null) return;

        // Randzellen: kein Overlay (Original: findCore-Abstand >= 2)
        net.minecraft.world.phys.HitResult look = net.minecraft.client.Minecraft.getInstance().hitResult;
        if (look instanceof BlockHitResult bhr && BlockOrbitalStation.isSideCell(pos, bhr.getBlockPos())) return;

        if (!(world.getBlockEntity(pos) instanceof TileEntityOrbitalStation station)) return;

        List<Component> text = new ArrayList<>();

        for (ItemStack stack : station.clientSlots()) {
            if (!stack.isEmpty()) {
                text.add(Component.literal("<- ").withStyle(ChatFormatting.RED)
                        .append(stack.getHoverName().copy().withStyle(ChatFormatting.RESET)));
            }
        }

        if (!text.isEmpty()) {
            text.add(Component.translatable("station.retrieveRocket"));
        } else {
            if (station.hasDocked) {
                if (!station.hasRider) {
                    if (player.isShiftKeyDown()) {
                        if (player.getMainHandItem().isEmpty()) {
                            text.add(Component.translatable("station.removeRocket"));
                        }
                    } else {
                        text.add(Component.translatable("station.enterRocket"));
                    }
                } else {
                    text.add(Component.translatable("station.occupiedRocket").withStyle(ChatFormatting.YELLOW));
                }

                if (station.needsFuel) {
                    for (FluidTank tank : station.getReceivingTanks()) {
                        text.add(Component.literal("-> ").withStyle(ChatFormatting.GREEN)
                                .append(FluidType.forFluid(tank.getTankType()).getLocalizedName().copy().withStyle(ChatFormatting.RESET))
                                .append(Component.literal(": " + tank.getFill() + "/" + tank.getMaxFill() + "mB").withStyle(ChatFormatting.RESET)));
                    }

                    if (!station.hasFuel) {
                        text.add(Component.translatable("station.emptyRocket").withStyle(ChatFormatting.RED));
                    }
                }
            } else if (!player.isShiftKeyDown()) {
                ItemStack held = player.getMainHandItem();
                if (!held.isEmpty()) {
                    if (held.getItem() == SpaceRocketItems.ROCKET_CUSTOM.get() && ItemCustomRocket.hasFuel(held)) {
                        text.add(Component.translatable("station.placeRocket"));
                    }
                    if (held.getItem() == SpaceRocketItems.RP_CAPSULE_20.get() || held.getItem() == SpaceRocketItems.RP_POD_20.get()) {
                        text.add(Component.translatable("station.placeRocket"));
                    }
                }
            }
        }

        if (text.isEmpty()) return;

        ILookOverlay.printGeneric(g, title, 0xffff00, 0x404000, text);
    }

}
