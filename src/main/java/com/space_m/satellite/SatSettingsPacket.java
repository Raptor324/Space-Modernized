package com.space_m.satellite;

import com.space_m.network.SpacePacket;
import com.space_m.network.SpacePacketHandler;

import dev.architectury.networking.NetworkManager.PacketContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/**
 * C2S: Einstellungen des gehaltenen Satelliten aus {@code GUIScreenSatSettings}, = NTM {@code NBTItemControlPacket}
 * an {@code ItemSatellite.receiveControl} (Werte 1:1 geklemmt). Gilt fuer alle Satelliten-Gegenstaende
 * ({@link SpaceSatelliteRegistry#isSatelliteItem}), auch die HBM-{@code sat_*}.
 * Original-Klassen: NBTItemControlPacket, ItemSatellite.receiveControl
 */
public final class SatSettingsPacket implements SpacePacket {

    public static final ResourceLocation ID = SpacePacketHandler.c2s("sat_settings");

    private final CompoundTag data;

    public SatSettingsPacket(CompoundTag data) {
        this.data = data;
    }

    public static SatSettingsPacket decode(FriendlyByteBuf buf) {
        CompoundTag tag = buf.readNbt();
        return new SatSettingsPacket(tag != null ? tag : new CompoundTag());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeNbt(data);
    }

    public static void handle(SatSettingsPacket message, PacketContext ctx) {
        ctx.queue(() -> {
            if (!(ctx.getPlayer() instanceof ServerPlayer player)) return;
            ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
            if (held.isEmpty() || !SpaceSatelliteRegistry.isSatelliteItem(held.getItem())) return;
            receiveControl(held, message.data);
        });
    }

    /** Original {@code ItemSatellite.receiveControl}. */
    public static void receiveControl(ItemStack stack, CompoundTag data) {
        int r = Mth.clamp(data.getInt("satColorR"), 0, 255);
        int g = Mth.clamp(data.getInt("satColorG"), 0, 255);
        int b = Mth.clamp(data.getInt("satColorB"), 0, 255);
        SatelliteItemData.setColor(stack, r / 255F, g / 255F, b / 255F);
        SatelliteItemData.setAltitude(stack, Mth.clamp(data.getFloat("satAltitude"), SatelliteOrbit.MIN_ALTITUDE_KM, SatelliteOrbit.MAX_ALTITUDE_KM));
        SatelliteItemData.setPhaseOffset(stack, data.getFloat("satPhaseOffset"));
        SatelliteItemData.setInclination(stack, Mth.clamp(data.getFloat("satInclination"), SatelliteOrbit.MIN_INCLINATION, SatelliteOrbit.MAX_INCLINATION));
        SatelliteItemData.setOwner(stack, data.getString("satOwner"));
        SatelliteItemData.setBlinking(stack, data.getBoolean("satIsBlinking"));
        SatelliteItemData.setBlinkPeriod(stack, data.getFloat("satBlink"));
    }

    public static void register() {
        SpacePacketHandler.registerC2S(ID, SatSettingsPacket::decode, SatSettingsPacket::handle);
    }
}
