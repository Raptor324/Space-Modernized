package com.space_m.satellite.client;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.item.ISatChip;
import com.space_m.celestial.CelestialBody;
import com.space_m.client.sky.SkyHooks;
import com.space_m.satellite.ClientSatellite;
import com.space_m.satellite.SatelliteClientData;
import com.space_m.satellite.SatelliteEvents;
import com.space_m.satellite.SatelliteItemData;
import com.space_m.satellite.SatelliteOrbit;
import com.space_m.satellite.SpaceSatelliteRegistry;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Satelliten am Himmel (Original {@code SkyProviderCelestial.render}, Block "visibility > 0.2"): Bobmazon-Paketsatellit
 * ueber Kerbin, alle Client-Satelliten des Koerpers, Vorschau des gehaltenen Satelliten. Dekorator um die vorherigen
 * {@link SkyHooks} (Abstimmung R6: Station/Shocks bleiben beim Vorgaenger).
 * Original-Klasse: SkyProviderCelestial (Satelliten-Teil), renderHeldSatellitePreview
 */
public class SatelliteSkyHooks implements SkyHooks {

    private final SkyHooks prev;

    public SatelliteSkyHooks(SkyHooks prev) {
        this.prev = prev;
    }

    @Override
    public void renderSatellites(float partialTicks, ClientLevel world, float solarAngle) {
        prev.renderSatellites(partialTicks, world, solarAngle);

        // JEFF BOZOS WOULD LIKE TO KNOW YOUR LOCATION
        // ... to send you a pakedge :)))
        if (world.dimension() == Level.OVERWORLD) {
            SatelliteRenderer.renderDefault(partialTicks, solarAngle, 1916169, 1.0F, 0.534F, 0.385F, SatelliteOrbit.DEFAULT_INCLINATION, SatelliteOrbit.DEFAULT_ALTITUDE_KM, SatelliteOrbit.DEFAULT_IS_BLINKING, SatelliteOrbit.DEFAULT_BLINK_PERIOD);
        }

        // Light up the sky
        for (ClientSatellite sat : SatelliteClientData.getClientSats().values()) {
            SatelliteRenderer.render(sat, partialTicks, world, solarAngle);
        }

        renderHeldSatellitePreview(partialTicks, world, solarAngle);
    }

    /** Original {@code renderHeldSatellitePreview}. */
    protected void renderHeldSatellitePreview(float partialTicks, ClientLevel world, float solarAngle) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        ItemStack held = mc.player.getMainHandItem();
        if (held.isEmpty() || !SatelliteEvents.isPayloadSatellite(held.getItem())) return;
        CelestialBody current = currentBody(world);
        if (SpaceSatelliteRegistry.getTargetBody(held, current.getEnum()) != current.getEnum()) return;

        SatelliteOrbit o = SatelliteItemData.read(held);

        SatelliteRenderer.renderOrbitLine(solarAngle, o.colorR, o.colorG, o.colorB, o.inclination, o.altitude, o.isBlinking, o.blinkPeriod);
        SatelliteRenderer.renderDefault(partialTicks, solarAngle, ISatChip.getFreqS(held), o.colorR, o.colorG, o.colorB, o.inclination, o.altitude, o.phaseOffset, o.isBlinking, o.blinkPeriod);
    }

    /** Client: {@code CelestialBody.getTarget(world, x, z).body} (Orbit: umkreister Koerper der Client-Station). */
    public static CelestialBody currentBody(@Nullable Level world) {
        if (world == null) return CelestialBody.getBody("kerbin");
        if (CelestialBody.inOrbit(world)) {
            CelestialBody orbiting = SkyHooks.get().stationOrbiting();
            return orbiting != null ? orbiting : CelestialBody.getBody("kerbin");
        }
        return CelestialBody.getBody(world);
    }

    // ── alles andere an den Vorgaenger ──

    @Override @Nullable public CelestialBody stationOrbiting() { return prev.stationOrbiting(); }

    @Override @Nullable public CelestialBody stationTarget() { return prev.stationTarget(); }

    @Override public StationState stationState() { return prev.stationState(); }

    @Override public double stationTransferProgress(float partialTicks) { return prev.stationTransferProgress(partialTicks); }

    @Override public double stationUnscaledProgress(float partialTicks) { return prev.stationUnscaledProgress(partialTicks); }

    @Override public List<StationPos> orbitingStations() { return prev.orbitingStations(); }

    @Override public List<ShockStatus> clientShocks(CelestialBody body) { return prev.clientShocks(body); }
}
