package com.space_m.satellite;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.satellite.Satellite;
import com.hbm_m.satellite.SatelliteDeathRay;
import com.hbm_m.satellite.SatelliteDetector;
import com.hbm_m.satellite.SatelliteHorizons;
import com.hbm_m.satellite.SatelliteLunarMiner;
import com.hbm_m.satellite.SatelliteMapper;
import com.hbm_m.satellite.SatelliteMiner;
import com.hbm_m.satellite.SatellitePrecisionLaser;
import com.hbm_m.satellite.SatelliteRadar;
import com.hbm_m.satellite.SatelliteRayScan;
import com.hbm_m.satellite.SatelliteRelay;
import com.hbm_m.satellite.SatelliteResonator;
import com.hbm_m.satellite.SatelliteScanner;
import com.space_m.celestial.SolarSystem;
import com.space_m.main.SpaceRegistry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Die Space-Fassung von {@code XSatelliteRegistry} (NTM Space) ueber den oeffentlichen HBM-Erweiterungspunkten
 * {@code Satellite.ID_TO_CLASS}/{@code ITEM_TO_CLASS}:
 * <ul>
 *   <li>Neue Satellitenklassen {@link SatelliteDysonRelay} und {@link SatelliteRailgun} (IDs {@link #ID_DYSON_RELAY},
 *       {@link #ID_RAILGUN}; HBM belegt 0..12, das Addon ab 100, damit spaetere HBM-Satelliten nicht kollidieren).</li>
 *   <li>Satelliten-Gegenstaende wie im Original: die {@code sat_*}-Raketennutzlasten (HBM {@code hbm_m:sat_*} und die
 *       Space-neuen {@code space_m:sat_*}) werden ihrer Klasse zugeordnet. HBM selbst ordnet jede Klasse nur ihrem
 *       {@code satellite_*}-Gegenstand zu (Master-Stand, Soyuz); beide Zuordnungen bestehen nebeneinander.</li>
 *   <li>Farbe je Klasse ({@code registerSatellite(..., r, g, b)}), Zielkoerper je Klasse ({@link #getTargetBody}).</li>
 * </ul>
 * Die Zuordnung muss nach HBMs {@code Satellite.register()} (HBM-Setup) laufen, sonst wuerde HBMs Regel
 * "Klasse schon vergeben" die {@code satellite_*}-Gegenstaende verdraengen; {@link #ensureRegistered()} prueft das und ist
 * mehrfach aufrufbar (Setup, Serverstart, Client bei Bedarf).
 * Original-Klasse: XSatelliteRegistry
 */
public final class SpaceSatelliteRegistry {

    private SpaceSatelliteRegistry() {}

    public static final int ID_DYSON_RELAY = 100;
    public static final int ID_RAILGUN = 101;

    private static final Map<Class<? extends Satellite>, float[]> SATELLITE_COLORS = new HashMap<>();
    /** Fuer Klassen ohne Original-Farbe (HBM-eigene, z.B. {@code SatelliteScience}): weiss. */
    public static final float[] FALLBACK_COLOR = {1.0F, 1.0F, 1.0F};

    private static boolean registered;

    /** Original {@code XSatelliteRegistry.register()} (Reihenfolge und Farben 1:1). */
    public static synchronized void ensureRegistered() {
        if (registered) return;
        // HBM-Registrierung abwarten (ID 0 = SatelliteMapper)
        if (!Satellite.ID_TO_CLASS.containsKey(0)) return;
        if (!Satellite.ID_TO_CLASS.containsKey(ID_DYSON_RELAY)) Satellite.ID_TO_CLASS.put(ID_DYSON_RELAY, SatelliteDysonRelay.class);
        if (!Satellite.ID_TO_CLASS.containsKey(ID_RAILGUN)) Satellite.ID_TO_CLASS.put(ID_RAILGUN, SatelliteRailgun.class);

        // no enum items, sorry
        registerSatellite(SatelliteMapper.class, "hbm_m:sat_mapper", 0.538F, 1.0F, 0.523F);
        registerSatellite(SatelliteScanner.class, "hbm_m:sat_scanner", 0.544F, 0.680F, 1.0F);
        registerSatellite(SatelliteRadar.class, "hbm_m:sat_radar", 0.134F, 1.0F, 0.134F);
        registerSatellite(SatelliteDeathRay.class, "hbm_m:sat_laser", 0.221F, 0.663F, 1.0F);
        registerSatellite(SatelliteResonator.class, "hbm_m:sat_resonator", 1.0F, 0.646F, 0.181F);
        registerSatellite(SatelliteRelay.class, "hbm_m:sat_foeq", 1.0F, 0.15F, 0.15F);
        registerSatellite(SatelliteMiner.class, "hbm_m:sat_miner", 0.46F, 0.56F, 0.68F);
        registerSatellite(SatelliteLunarMiner.class, "hbm_m:sat_lunar_miner", 0.42F, 0.54F, 0.82F);
        registerSatellite(SatelliteDysonRelay.class, "space_m:sat_dyson_relay", 1.0F, 0.9F, 0.8F);
        registerSatellite(SatelliteHorizons.class, "hbm_m:sat_gerald", 0.0F, 0.0F, 0.0F);
        registerSatellite(SatelliteRailgun.class, "space_m:sat_war", 0.0F, 0.0F, 0.0F);
        registerSatellite(SatellitePrecisionLaser.class, "space_m:sat_precision_laser", 0.221F, 1.0F, 0.663F);
        registerSatellite(SatelliteDetector.class, "space_m:sat_detector", 0.1F, 0.2F, 0.3F);
        registerSatellite(SatelliteRayScan.class, "space_m:sat_ray_scan", 0.1F, 0.2F, 0.3F);

        registered = true;
        SpaceRegistry.LOGGER.info("[space_m] Satellites registered ({} classes, {} items)", Satellite.ID_TO_CLASS.size(), Satellite.ITEM_TO_CLASS.size());
    }

    /**
     * Original {@code registerSatellite(sat, item, r, g, b)}. Abweichung: das Original vergibt eine Klasse nur einmal;
     * im Port haben die HBM-Klassen schon ihren {@code satellite_*}-Gegenstand, die Space-Nutzlast kommt hinzu.
     */
    private static void registerSatellite(Class<? extends Satellite> sat, String itemId, float r, float g, float b) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(itemId));
        if (item == null || item == Items.AIR) {
            SpaceRegistry.LOGGER.warn("[space_m] Satellite item {} missing", itemId);
        } else if (!Satellite.ITEM_TO_CLASS.containsKey(item) || Satellite.ITEM_TO_CLASS.get(item) == sat) {
            Satellite.ITEM_TO_CLASS.put(item, sat);
        }
        SATELLITE_COLORS.putIfAbsent(sat, new float[] {r, g, b});
    }

    /** Original {@code isSatelliteItem}. */
    public static boolean isSatelliteItem(@Nullable Item item) {
        ensureRegistered();
        return item != null && Satellite.ITEM_TO_CLASS.containsKey(item);
    }

    @Nullable
    public static Class<? extends Satellite> classOf(@Nullable Item item) {
        ensureRegistered();
        return item == null ? null : Satellite.ITEM_TO_CLASS.get(item);
    }

    /**
     * Original {@code getTargetDimensionId}: Relais (FOEQ) kreist um Duna, Mondbergbau um die Mun, Asteroidenbergbau um
     * Dres; alle anderen um den Koerper, von dem aus gestartet wird ({@code fallback}).
     */
    public static SolarSystem.Body getTargetBody(@Nullable Class<? extends Satellite> satelliteClass, SolarSystem.Body fallback) {
        if (satelliteClass == null) return fallback;
        if (SatelliteRelay.class.isAssignableFrom(satelliteClass)) return SolarSystem.Body.DUNA;
        if (SatelliteLunarMiner.class.isAssignableFrom(satelliteClass)) return SolarSystem.Body.MUN;
        if (SatelliteMiner.class.isAssignableFrom(satelliteClass)) return SolarSystem.Body.DRES;
        return fallback;
    }

    public static SolarSystem.Body getTargetBody(@Nullable ItemStack stack, SolarSystem.Body fallback) {
        if (stack == null || stack.isEmpty()) return fallback;
        return getTargetBody(classOf(stack.getItem()), fallback);
    }

    /** Original {@code createFromId}: neuer Satellit mit registrierter Farbe (Farbe liegt im {@link SatelliteOrbit}). */
    @Nullable
    public static Satellite createFromId(int id) {
        ensureRegistered();
        return Satellite.createFromId(id);
    }

    /** Original {@code getRegisteredColor(Item)}; wirft wie das Original bei unbekanntem Gegenstand. */
    public static float[] getRegisteredColor(Item item) {
        Class<? extends Satellite> satelliteClass = classOf(item);
        if (satelliteClass == null) {
            throw new IllegalStateException("No satellite class registered for item: " + item);
        }
        return getRegisteredColor(satelliteClass);
    }

    /** Original {@code getRegisteredColor(Class)}; HBM-eigene Klassen ohne Original-Farbe -> {@link #FALLBACK_COLOR}. */
    public static float[] getRegisteredColor(@Nullable Class<? extends Satellite> satelliteClass) {
        ensureRegistered();
        float[] color = satelliteClass == null ? null : SATELLITE_COLORS.get(satelliteClass);
        return color != null ? color : FALLBACK_COLOR;
    }

    /** Wie {@link #getRegisteredColor(Item)}, aber ohne Ausnahme (Lesen von Gegenstandsdaten). */
    public static float[] getRegisteredColorOrDefault(@Nullable Item item) {
        return getRegisteredColor(item == null ? null : classOf(item));
    }
}
