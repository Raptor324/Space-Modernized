package com.space_m.celestial.client;

import java.util.HashMap;

import com.space_m.SpaceMod;
import com.space_m.celestial.CelestialBody;
import com.space_m.celestial.CelestialTraitsPacket;
import com.space_m.celestial.SolarSystemWorldSavedData;
import com.space_m.celestial.trait.CBT_Invasion;
import com.space_m.celestial.trait.CelestialBodyTrait;
import com.space_m.network.SpacePacketHandler;

import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

/**
 * Client-Teil des Himmelskoerper-Modells (nur physischer Client):
 * <ul>
 *   <li>Empfang von {@link CelestialTraitsPacket} (Original {@code PermaSyncHandler.readPacket}, CBT).</li>
 *   <li>{@code ModEventHandlerClient.onClientTick} (END, nicht pausiert): {@code trait.update(true, body)} fuer alle
 *       synchronisierten Traits.</li>
 *   <li>Invasion: Warnton {@code alarm.ping} + Chat "Incoming Invasion!" (Original {@code CBT_Invasion.update}).</li>
 *   <li>Beim Verlassen der Welt: Client-Traits leeren.</li>
 * </ul>
 */
public final class CelestialClient {
    private CelestialClient() {}

    private static boolean initialized;

    public static void init() {
        if (initialized) return;
        initialized = true;

        SpacePacketHandler.registerS2C(SpacePacketHandler.CELESTIAL_TRAITS, CelestialTraitsPacket::decode,
                (pkt, ctx) -> ctx.queue(pkt::apply));

        ClientTickEvent.CLIENT_POST.register(CelestialClient::onClientTick);
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> {
            SolarSystemWorldSavedData.updateClientTraits(null);
            SolarSystemWorldSavedData.clientStations.clear();
        });

        CBT_Invasion.clientHooks = new CBT_Invasion.ClientHooks() {
            @Override
            public void warn(CBT_Invasion invasion) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player == null) return;
                SoundEvent ping = sound(SpaceMod.id("alarm.ping"));
                if (ping != null) mc.player.playSound(ping, 10F, 1F);
                mc.player.sendSystemMessage(Component.literal("Incoming Invasion!").withStyle(ChatFormatting.RED));
            }
        };
    }

    private static SoundEvent sound(ResourceLocation id) {
        return BuiltInRegistries.SOUND_EVENT.get(id);
    }

    private static void onClientTick(Minecraft mc) {
        if (mc.isPaused()) return;
        for (CelestialBody body : CelestialBody.getAllBodies()) {
            HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> traits = SolarSystemWorldSavedData.getClientTraits(body.name);
            if (traits != null) {
                for (CelestialBodyTrait trait : traits.values().toArray(new CelestialBodyTrait[0])) {
                    trait.update(true, body);
                }
            }
        }
    }
}
