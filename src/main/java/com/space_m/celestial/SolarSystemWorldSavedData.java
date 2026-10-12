package com.space_m.celestial;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;

import org.jetbrains.annotations.Nullable;

import com.space_m.celestial.trait.CelestialBodyTrait;
import com.space_m.platform.SavedDataHooks;
import com.space_m.platform.SpaceSavedData;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

/**
 * Global sichtbarer Sonnensystem-Zustand (Trait-Ueberschreibungen je Koerper fuer Terraforming/Krieg/Dyson ...
 * und die Raumstationen), 1:1 NTM Space {@code SolarSystemWorldSavedData}.
 * Original-Klasse: SolarSystemWorldSavedData
 *
 * <p>Gespeichert in der Overworld ({@link SavedDataHooks#global}), Original {@code world.mapStorage} (global).
 * NBT-Format wie im Original: je Koerper {@code b_<name>} mit einem Unterschluessel je Trait-Name.
 * Stationen ({@link com.space_m.orbit.OrbitalStation}, R6) als Liste {@code stations} mit den Original-Feldern.
 */
public class SolarSystemWorldSavedData extends SpaceSavedData {

    /*
     * Similar to CelestialBodyWorldSavedData but for anything that must be visible from
     * other bodies, like atmospheric data or UTTER ANNIHILATION
     */

    public static final String DATA_NAME = "space_m_solar_system";

    private final HashMap<String, HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait>> traitMap = new HashMap<>();
    private final Random rand = new Random();

    private final HashMap<ChunkPos, com.space_m.orbit.OrbitalStation> stations = new HashMap<>();

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

        ListTag stationList = nbt.getList("stations", Tag.TAG_COMPOUND);
        for (int i = 0; i < stationList.size(); i++) {
            CompoundTag stationTag = stationList.getCompound(i);
            int x = stationTag.getInt("x");
            int z = stationTag.getInt("z");
            CelestialBody orbiting = CelestialBody.getBody(stationTag.getString("orbiting"));
            CelestialBody target = CelestialBody.getBody(stationTag.getString("target"));
            com.space_m.orbit.OrbitalStation.StationState state = com.space_m.orbit.OrbitalStation.StationState.values()[stationTag.getInt("state")];
            int stateTimer = stationTag.getInt("stateTimer");
            int maxStateTimer = stationTag.getInt("maxStateTimer");
            boolean hasStation = stationTag.getBoolean("hasStation");
            String name = stationTag.getString("name");
            float gravityMultiplier = stationTag.contains("gravity") ? stationTag.getFloat("gravity") : 1;

            ChunkPos pos = new ChunkPos(x, z);
            com.space_m.orbit.OrbitalStation station = new com.space_m.orbit.OrbitalStation(orbiting, x, z);
            station.target = target;
            station.state = state;
            station.stateTimer = stateTimer;
            station.maxStateTimer = maxStateTimer;
            station.hasStation = hasStation;
            station.name = name;
            station.gravityMultiplier = gravityMultiplier;

            stations.put(pos, station);
        }
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

        ListTag stationList = new ListTag();
        for (com.space_m.orbit.OrbitalStation station : stations.values()) {
            CompoundTag stationTag = new CompoundTag();
            stationTag.putInt("x", station.dX);
            stationTag.putInt("z", station.dZ);
            stationTag.putString("orbiting", station.orbiting.name);
            stationTag.putString("target", station.target.name);
            stationTag.putInt("state", station.state.ordinal());
            stationTag.putInt("stateTimer", station.stateTimer);
            stationTag.putInt("maxStateTimer", station.maxStateTimer);
            stationTag.putBoolean("hasStation", station.hasStation);
            stationTag.putString("name", station.name);
            stationTag.putFloat("gravity", station.gravityMultiplier);

            stationList.add(stationTag);
        }
        nbt.put("stations", stationList);
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

    // ── Stationen (R6, 1:1 Original) ──

    public HashMap<ChunkPos, com.space_m.orbit.OrbitalStation> getStations() {
        return stations;
    }

    // Grabs an existing station
    @Nullable
    public com.space_m.orbit.OrbitalStation getStationFromPosition(int x, int z) {
        // yeah they aren't exactly chunks but this is a nice little hashable that already exists
        ChunkPos pos = new ChunkPos(Mth.floor((float) x / com.space_m.orbit.OrbitalStation.STATION_SIZE), Mth.floor((float) z / com.space_m.orbit.OrbitalStation.STATION_SIZE));
        return stations.get(pos);
    }

    public ChunkPos findFreeSpace() {
        int size = com.space_m.config.SpaceConfig.get().maxStationDistance / com.space_m.orbit.OrbitalStation.STATION_SIZE;

        // First half of the loop will attempt to find a completely empty station spot
        // Second half will accept reserved but unlaunched station spots
        ChunkPos pos = null;
        for (int i = 0; i < 512; i++) {
            pos = new ChunkPos(rand.nextInt(size * 2) - size, rand.nextInt(size * 2) - size);
            if (!stations.containsKey(pos)) return pos;
            if (i > 256 && !stations.get(pos).hasStation) return pos;
        }

        return pos;
    }

    // Finds an unoccupied space and adds a new station
    public com.space_m.orbit.OrbitalStation addStation(CelestialBody orbiting) {
        ChunkPos pos = findFreeSpace();

        return addStation(pos.x, pos.z, orbiting);
    }

    // Adds a station at a given set of coordinates (used for debug stations)
    // Won't overwrite existing stations
    public com.space_m.orbit.OrbitalStation addStation(int x, int z, CelestialBody orbiting) {
        ChunkPos pos = new ChunkPos(x, z);

        com.space_m.orbit.OrbitalStation station = stations.get(pos);

        if (station == null) {
            station = new com.space_m.orbit.OrbitalStation(orbiting, x, z);
            stations.put(pos, station);
        }

        setDirty();

        return station;
    }

    public void removeStation(com.space_m.orbit.OrbitalStation station) {
        removeStation(station.dX, station.dZ);
    }

    public void removeStation(int x, int z) {
        ChunkPos pos = new ChunkPos(x, z);

        // Don't remove stations with launched cores, only temporary stations!
        com.space_m.orbit.OrbitalStation station = stations.get(pos);
        if (station == null || station.hasStation) return;

        stations.remove(pos);
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
