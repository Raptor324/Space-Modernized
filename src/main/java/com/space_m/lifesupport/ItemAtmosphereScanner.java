package com.space_m.lifesupport;

import com.hbm_m.api.fluids.FluidLocalization;
import com.hbm_m.network.InfoToastPacket;
import com.hbm_m.util.BobMathUtil;
import com.space_m.atmosphere.ChunkAtmosphereManager;
import com.space_m.celestial.trait.CBT_Atmosphere;
import com.space_m.celestial.trait.CBT_Atmosphere.FluidEntry;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 1:1 NTM Space {@code ItemAtmosphereScanner}: solange im Inventar, alle 5 Ticks die Atmosphaere am Kopf des Spielers
 * als Info-Zeilen (je Gas eine Zeile ab ID 969, 4 s) - Partialdruck mit 4 Nachkommastellen, sonst "NEAR VACUUM".
 * Original-Klasse: ItemAtmosphereScanner
 * <p>{@code PlayerInformPacket(..., 4000 ms)} -> HBM {@code InfoToastPacket} mit 80 Ticks.
 */
public class ItemAtmosphereScanner extends Item {

    public ItemAtmosphereScanner(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean inHand) {

        if (!(entity instanceof ServerPlayer player) || world.getGameTime() % 5 != 0) return;

        CBT_Atmosphere atmosphere = ChunkAtmosphereManager.proxy.getAtmosphere(entity);

        boolean hasAtmosphere = false;
        if (atmosphere != null) {
            for (int i = 0; i < atmosphere.fluids.size(); i++) {
                FluidEntry entry = atmosphere.fluids.get(i);
                if (entry.pressure > 0.0001) {
                    String pressure = String.format("%.4f", BobMathUtil.roundDecimal(entry.pressure, 4));
                    Component text = FluidLocalization.nameFromFluidId(entry.fluid).copy().withStyle(ChatFormatting.AQUA)
                            .append(Component.literal(": "))
                            .append(Component.literal(pressure + "atm").withStyle(ChatFormatting.WHITE));
                    InfoToastPacket.sendTo(player, text, 80, 969 + i, 0xFFFFFF);
                    hasAtmosphere = true;
                }
            }
        }

        if (!hasAtmosphere) {
            InfoToastPacket.sendTo(player, Component.literal("NEAR VACUUM").withStyle(ChatFormatting.YELLOW), 80, 969, 0xFFFFFF);
        }
    }
}
