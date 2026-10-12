package com.space_m.satellite;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.satellite.Satellite;
import com.hbm_m.satellite.SatelliteManager;
import com.space_m.celestial.CelestialBody;
import com.space_m.celestial.SolarSystem;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Koerpergebundene Satelliten (NTM Space {@code SatelliteSavedData.getData(world, x, z)}, {@code getDataFromFreq},
 * {@code XSatelliteRegistry.orbit}). Satelliten gehoeren dem Himmelskoerper: HBMs {@link SatelliteManager} liegt je
 * Dimension, und aus der Orbit-Dimension wird auf den umkreisten Koerper umgeleitet (Station an x/z, R6-A ueber
 * {@code OrbitHooks}). Gasriesen/Koerper ohne Dimension -> Kerbin (Original {@code dimensionId 0}).
 *
 * <p>HBM leitet Verknuepfer, Dock, Horizons, {@code /ntmsatellites}, Soyuz und Lambda ueber die Space-API
 * ({@link SpaceSatelliteHooks}, W-R6C-2) hierher; Sat-Link/Schnittstelle/Designator/Linse/Radar fragen wie im Original
 * die eigene Welt. Bahndaten liegen im HBM-Satelliten ({@code Satellite.addonData}, W-R6C-1).
 * Original-Klassen: SatelliteSavedData (Space-Teil), XSatelliteRegistry.orbit
 */
public final class SpaceSatellites {

    private SpaceSatellites() {}

    /** Original {@code getData(world, x, z)}: Welt, deren Satelliten an x/z gelten. */
    public static ServerLevel getLevel(ServerLevel world, int x, int z) {
        if (CelestialBody.inOrbit(world)) {
            CelestialBody orbiting = CelestialBody.getTarget(world, x, z).body;
            ServerLevel orbitingWorld = levelOf(world, orbiting);
            if (orbitingWorld != null) return orbitingWorld;
        }
        return world;
    }

    public static SatelliteManager getData(ServerLevel world, int x, int z) {
        return SatelliteManager.get(getLevel(world, x, z));
    }

    /** Original {@code getDataFromFreq}: erst die eigene Welt, sonst alle geladenen Landekoerper (nicht im Nether/End). */
    public static ServerLevel getLevelFromFreq(ServerLevel world, int x, int z, int freq) {
        ServerLevel level = getLevel(world, x, z);
        if (SatelliteManager.get(level).getSatFromFreq(freq) != null) return level;
        if (world.dimension() == Level.NETHER || world.dimension() == Level.END) return level;

        for (CelestialBody body : CelestialBody.getLandableBodies()) {
            if (body.dimension == null) continue;
            ServerLevel bodyWorld = world.getServer().getLevel(body.dimension); // nur geladene Welten (DimensionManager.getWorld)
            if (bodyWorld == null) continue;
            if (SatelliteManager.get(bodyWorld).getSatFromFreq(freq) != null) return bodyWorld;
        }
        return level;
    }

    public static SatelliteManager getDataFromFreq(ServerLevel world, int x, int z, int freq) {
        return SatelliteManager.get(getLevelFromFreq(world, x, z, freq));
    }

    @Nullable
    public static Satellite getSatFromFreq(ServerLevel world, int x, int z, int freq) {
        return getDataFromFreq(world, x, z, freq).getSatFromFreq(freq);
    }

    /** Schluessel der Bahndaten in {@code Satellite.addonData} (Original-Felder {@code sat*} aus {@code sat_data_i}). */
    public static final String ORBIT_KEY = "space_m_orbit";

    /** Bahndaten eines Satelliten in {@code level} (registrierte Farbe, falls noch keine gespeichert). */
    @Nullable
    public static SatelliteOrbit getOrbit(ServerLevel level, int freq) {
        SatelliteManager manager = SatelliteManager.get(level);
        Satellite sat = manager.getSatFromFreq(freq);
        if (sat == null) return null;
        return getOrbit(level, manager, freq, sat);
    }

    private static SatelliteOrbit getOrbit(ServerLevel level, SatelliteManager manager, int freq, Satellite sat) {
        if (sat.addonData.contains(ORBIT_KEY)) {
            SatelliteOrbit o = new SatelliteOrbit();
            o.readFromNBT(sat.addonData.getCompound(ORBIT_KEY), SpaceSatelliteRegistry.getRegisteredColor(sat.getClass()));
            return o;
        }
        // Altbestand (vor W-R6C-1: eigene SavedData) bzw. Standardbahn mit registrierter Farbe -> in den Satelliten uebernehmen
        SatelliteOrbit o = SatelliteOrbitSavedData.get(level).getOrCreate(freq, sat);
        putOrbit(manager, sat, o);
        return o;
    }

    private static void putOrbit(SatelliteManager manager, Satellite sat, SatelliteOrbit orbit) {
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        orbit.writeToNBT(tag);
        sat.addonData.put(ORBIT_KEY, tag);
        manager.setDirty();
    }

    /**
     * 1:1 {@code XSatelliteRegistry.orbit}: belegt die Frequenz (Bahndaten aus dem Gegenstand) oder liefert ein Teil an den
     * vorhandenen Satelliten. Relais/Mond-/Asteroidenbergbau landen im Orbit ihres Zielkoerpers.
     */
    public static void orbit(ServerLevel world, ItemStack stack, int freq, double x, double y, double z) {
        SpaceSatelliteRegistry.ensureRegistered();
        SatelliteManager data = getData(world, (int) x, (int) z);
        Satellite existing = data.getSatFromFreq(freq);

        if (existing != null) {
            // HBM SatelliteManager.orbit: onPartDelivered + setDirty
            data.orbit(getLevel(world, (int) x, (int) z), stack, freq, x, y, z);
            return;
        }

        Satellite probe = Satellite.createFromItem(stack);
        if (probe == null) return;

        ServerLevel targetWorld = targetWorld(world, probe.getClass());
        if (targetWorld != null) world = targetWorld;

        SatelliteOrbit orbit = SatelliteItemData.read(stack);

        ServerLevel level = getLevel(world, (int) x, (int) z);
        SatelliteManager target = SatelliteManager.get(level);
        target.orbit(level, stack, freq, x, y, z); // createFromItem + put + onOrbit (RTTY) + setDirty
        Satellite placed = target.getSatFromFreq(freq);
        if (placed != null) putOrbit(target, placed, orbit);
    }

    /** Original {@code getTargetDimensionId} + {@code DimensionManager.getWorld}: Welt des Zielkoerpers oder null. */
    @Nullable
    private static ServerLevel targetWorld(ServerLevel world, Class<? extends Satellite> cls) {
        SolarSystem.Body fallback = null;
        SolarSystem.Body body = SpaceSatelliteRegistry.getTargetBody(cls, fallback);
        if (body == null) return null;
        CelestialBody celestial = body.getBody();
        if (celestial == null || celestial.dimension == null || celestial.dimension == world.dimension()) return null;
        return world.getServer().getLevel(celestial.dimension);
    }

    @Nullable
    private static ServerLevel levelOf(ServerLevel world, @Nullable CelestialBody body) {
        ResourceKey<Level> dim = body != null && body.dimension != null ? body.dimension : Level.OVERWORLD;
        return world.getServer().getLevel(dim);
    }

    /**
     * Original {@code PermaSyncHandler} (Abschnitt SATELLITES): Satelliten je Koerper - der Koerper unter dem Spieler
     * (im Orbit der umkreiste) und alle geladenen Landekoerper. Schluessel = Koerpername.
     */
    public static Map<String, ServerLevel> syncedLevels(ServerLevel world, int x, int z) {
        Map<String, ServerLevel> out = new java.util.LinkedHashMap<>();
        ServerLevel current = getLevel(world, x, z);
        out.put(bodyName(current), current);
        for (CelestialBody body : CelestialBody.getLandableBodies()) {
            if (body == null || body.dimension == null || out.containsKey(body.name)) continue;
            ServerLevel bodyWorld = world.getServer().getLevel(body.dimension);
            if (bodyWorld == null) continue;
            out.put(body.name, bodyWorld);
        }
        return out;
    }

    public static String bodyName(Level level) {
        return CelestialBody.getBody(level).name;
    }

    /** Alle Satelliten einer Welt mit Bahndaten (Server). */
    public static Map<Integer, SatelliteOrbit> orbits(ServerLevel level) {
        SatelliteManager manager = SatelliteManager.get(level);
        Map<Integer, SatelliteOrbit> out = new HashMap<>();
        for (Map.Entry<Integer, Satellite> e : manager.all().entrySet()) {
            out.put(e.getKey(), getOrbit(level, manager, e.getKey(), e.getValue()));
        }
        return out;
    }
}
