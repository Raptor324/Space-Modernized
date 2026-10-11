package com.space_m.celestial;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.space_m.celestial.trait.CelestialBodyTrait;
import com.space_m.network.SpacePacket;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/**
 * Trait-Sync Server -> Client, der {@code /// CBT ///}-Abschnitt von NTM Space {@code PermaSyncHandler}
 * (alle 5 Ticks, {@code getTotalWorldTime() % 5 == 1}). Inhalt wie im Original: je Koerper die
 * SavedData-Traits (Index in Registrierungsreihenfolge + Bytes) oder "keine Ueberschreibung", dazu die
 * Stationspositionen ueber dem eigenen Koerper.
 *
 * <p>Abweichung: der Koerpername steht vor jedem Eintrag (statt reiner Reihenfolge von {@code getAllBodies()}),
 * damit eine abweichende Iterationsreihenfolge den Puffer nicht verschiebt. Der Empfang wird auf dem
 * Client-Thread ausgefuehrt ({@link #apply()}).
 */
public final class CelestialTraitsPacket implements SpacePacket {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** Fertige Bytes (Server) bzw. empfangene Bytes (Client). */
    private final FriendlyByteBuf payload;

    private CelestialTraitsPacket(FriendlyByteBuf payload) {
        this.payload = payload;
    }

    /** Serverseitig: Zustand fuer einen Spieler schreiben. */
    public static CelestialTraitsPacket create(ServerPlayer player, SolarSystemWorldSavedData data) {
        FriendlyByteBuf buf = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        writeTraits(buf, data);

        List<int[]> stations = OrbitHooks.get().syncedStations(player);
        buf.writeInt(stations.size());
        for (int[] station : stations) {
            buf.writeInt(station[0]);
            buf.writeInt(station[1]);
        }
        return new CelestialTraitsPacket(buf);
    }

    /** Trait-Teil (auch fuer Tests). */
    public static void writeTraits(FriendlyByteBuf buf, SolarSystemWorldSavedData data) {
        List<CelestialBody> bodies = new ArrayList<>(CelestialBody.getAllBodies());
        buf.writeInt(bodies.size());
        for (CelestialBody body : bodies) {
            buf.writeUtf(body.name);
            HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> traits = data.getTraits(body.name);
            if (traits != null) {
                buf.writeBoolean(true); // Has traits marker (since we can have an empty list)
                buf.writeInt(traits.size());

                for (int i = 0; i < CelestialBodyTrait.traitList.size(); i++) {
                    Class<? extends CelestialBodyTrait> traitClass = CelestialBodyTrait.traitList.get(i);
                    CelestialBodyTrait trait = traits.get(traitClass);

                    if (trait != null) {
                        buf.writeInt(i); // ID of the trait, in order registered
                        trait.writeToBytes(buf);
                    }
                }
            } else {
                buf.writeBoolean(false);
            }
        }
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBytes(payload, payload.readerIndex(), payload.readableBytes());
    }

    public static CelestialTraitsPacket decode(FriendlyByteBuf buf) {
        FriendlyByteBuf copy = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer(buf.readableBytes()));
        copy.writeBytes(buf);
        return new CelestialTraitsPacket(copy);
    }

    /** Clientseitig auswerten (Original {@code PermaSyncHandler.readPacket}, Abschnitt CBT). */
    public void apply() {
        try {
            readTraits(payload);

            SolarSystemWorldSavedData.clientStations.clear();
            int count = payload.readInt();
            for (int i = 0; i < count; i++) {
                SolarSystemWorldSavedData.clientStations.add(new int[] { payload.readInt(), payload.readInt() });
            }
        } catch (Exception ex) {
            // If any exception occurs, stop parsing any more bytes, they'll be unaligned
            // We'll unset the client trait set to prevent any issues
            LOGGER.error("[space_m] celestial trait sync failed", ex);
            SolarSystemWorldSavedData.updateClientTraits(null);
        }
    }

    /** Trait-Teil lesen und in {@link SolarSystemWorldSavedData#clientTraits} uebernehmen. */
    public static void readTraits(FriendlyByteBuf buf) {
        HashMap<String, HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait>> traitMap = SolarSystemWorldSavedData.clientTraits;

        if (traitMap == null) {
            traitMap = new HashMap<>();
            SolarSystemWorldSavedData.updateClientTraits(traitMap);
        }

        int bodies = buf.readInt();
        for (int b = 0; b < bodies; b++) {
            String name = buf.readUtf();
            if (buf.readBoolean()) {
                HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> traits = traitMap.get(name);

                if (traits == null) {
                    traits = new HashMap<>();
                    traitMap.put(name, traits);
                }

                List<Class<? extends CelestialBodyTrait>> sentTraits = new ArrayList<>();

                int cbtSize = buf.readInt();
                for (int i = 0; i < cbtSize; i++) {
                    int index = buf.readInt();
                    Class<? extends CelestialBodyTrait> clazz = CelestialBodyTrait.traitList.get(index);
                    sentTraits.add(clazz);

                    CelestialBodyTrait trait = traits.get(clazz);
                    if (trait == null) trait = CelestialBodyTrait.create(index);
                    trait.readFromBytes(buf);

                    traits.put(trait.getClass(), trait);
                }

                traits.keySet().removeIf(traitClass -> !sentTraits.contains(traitClass));
            } else {
                traitMap.remove(name);
            }
        }
    }
}
