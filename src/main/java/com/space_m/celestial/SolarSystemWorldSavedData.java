package com.space_m.celestial;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.jetbrains.annotations.Nullable;

import com.space_m.celestial.trait.CelestialBodyTrait;
import com.space_m.platform.SavedDataHooks;
import com.space_m.platform.SpaceSavedData;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;

/**
 * Global sichtbarer Sonnensystem-Zustand (Trait-Ueberschreibungen je Koerper fuer Terraforming/Krieg/Dyson ...
 * und die Raumstationen), 1:1 NTM Space {@code SolarSystemWorldSavedData}.
 * Original-Klasse: SolarSystemWorldSavedData
 *
 * <p>Gespeichert in der Overworld ({@link SavedDataHooks#global}), Original {@code world.mapStorage} (global).
 * NBT-Format wie im Original: je Koerper {@code b_<name>} mit einem Unterschluessel je Trait-Name.
 * Stationen ({@code OrbitalStation}) kommen in R6; bis dahin wird die Liste {@code stations} unveraendert
 * mitgefuehrt ({@link #getStationsTag()}).
 */
public class SolarSystemWorldSavedData extends SpaceSavedData {

    /*
     * Similar to CelestialBodyWorldSavedData but for anything that must be visible from
     * other bodies, like atmospheric data or UTTER ANNIHILATION
     */

    public static final String DATA_NAME = "space_m_solar_system";

    private final HashMap<String, HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait>> traitMap = new HashMap<>();
    private ListTag stations = new ListTag();

    public SolarSystemWorldSavedData() {}

    /** Daten des laufenden Servers (Original {@code get()} = Welt 0), null ohne Server. */
    @Nullable
    public static SolarSystemWorldSavedData get() {
        MinecraftServer server = CelestialBody.server();
        return server == null ? null : get(server);
    }

    public static SolarSystemWorldSavedData get(MinecraftServer server) {
        return SavedDataHooks.global(server, DATA_NAME, SolarSystemWorldSavedData::new, SolarSystemWorldSavedData::load);
    }

    /** Original {@code get(World)}; auf dem Client (kein Server-Level) null. */
    @Nullable
    public static SolarSystemWorldSavedData get(@Nullable Level level) {
        if (level == null || level.isClientSide || level.getServer() == null) return null;
        return get(level.getServer());
    }

    public static SolarSystemWorldSavedData load(CompoundTag nbt) {
        SolarSystemWorldSavedData data = new SolarSystemWorldSavedData();
        data.readFromNBT(nbt);
        return data;
    }

    public void readFromNBT(CompoundTag nbt) {
        for (CelestialBody body : CelestialBody.getAllBodies()) {
            if (nbt.contains("b_" + body.name)) {
                CompoundTag data = nbt.getCompound("b_" + body.name);
                HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> traits = new HashMap<>();

                for (Entry<String, Class<? extends CelestialBodyTrait>> entry : CelestialBodyTrait.traitMap.entrySet()) {
                    if (data.contains(entry.getKey())) {
                        try {
                            CelestialBodyTrait trait = CelestialBodyTrait.create(entry.getValue());
                            trait.readFromNBT(data.getCompound(entry.getKey()));
                            traits.put(trait.getClass(), trait);
                        } catch (Exception ex) { }
                    }
                }

                traitMap.put(body.name, traits);
            }
        }

        stations = nbt.getList("stations", Tag.TAG_COMPOUND);
    }

    @Override
    protected CompoundTag write(CompoundTag nbt) {
        for (Entry<String, HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait>> entry : traitMap.entrySet()) {
            CompoundTag data = new CompoundTag();

            for (CelestialBodyTrait trait : entry.getValue().values()) {
                String name = CelestialBodyTrait.traitMap.inverse().get(trait.getClass());
                if (name == null) continue;
                CompoundTag traitData = new CompoundTag();
                trait.writeToNBT(traitData);
                data.put(name, traitData);
            }

            nbt.put("b_" + entry.getKey(), data);
        }

        nbt.put("stations", stations);
        return nbt;
    }

    /** Gesamter Zustand als NBT (Tests/Werkzeuge; Speichern laeuft ueber {@code save}). */
    public CompoundTag toNBT() {
        return write(new CompoundTag());
    }

    public void setTraits(String bodyName, CelestialBodyTrait... traits) {
        if (traits.length == 0) {
            clearTraits(bodyName);
            return;
        }

        HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> newTraits = new HashMap<>();
        for (CelestialBodyTrait trait : traits) {
            newTraits.put(trait.getClass(), trait);
        }

        this.traitMap.put(bodyName, newTraits);

        setDirty();
    }

    public void clearTraits(String bodyName) {
        this.traitMap.remove(bodyName);

        setDirty();
    }

    @Nullable
    public HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> getTraits(String bodyName) {
        return traitMap.get(bodyName);
    }

    /** Rohdaten der Stationen (Original-Felder x, z, orbiting, target, state, ...); typisiert ab R6. */
    public ListTag getStationsTag() {
        return stations;
    }

    public void setStationsTag(ListTag stations) {
        this.stations = stations;
        setDirty();
    }


    // Client sync
    public static HashMap<String, HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait>> clientTraits = new HashMap<>();
    /** Original {@code OrbitalStation.orbitingStations} (Stations-Rasterpositionen ueber dem eigenen Koerper). */
    public static final List<int[]> clientStations = new ArrayList<>();

    public static void updateClientTraits(@Nullable HashMap<String, HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait>> traits) {
        clientTraits = traits;
        if (clientTraits == null) clientTraits = new HashMap<>();
    }

    @Nullable
    public static HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> getClientTraits(String bodyName) {
        return clientTraits.get(bodyName);
    }
}
