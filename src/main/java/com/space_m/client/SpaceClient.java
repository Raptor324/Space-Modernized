package com.space_m.client;

import com.space_m.main.SpaceRegistry;

import dev.architectury.event.events.client.ClientLifecycleEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;

/**
 * Client-Einstieg des Addons (nur auf dem physischen Client geladen, ueber {@code EnvExecutor} aus
 * {@link SpaceRegistry#init()} im Mod-Konstruktor). Hier anmelden:
 * <ul>
 *   <li>S2C-Empfaenger: {@code SpacePacketHandler.registerS2C(id, Pkt::decode, Pkt::handle)} direkt in {@link #init()}.</li>
 *   <li>Screens/Renderer/Partikel-Provider/Himmel ({@code DimensionSpecialEffects}): in {@link #setup(Minecraft)}
 *       bzw. ueber Architectury-Registries ({@code MenuRegistry.registerScreenFactory},
 *       {@code BlockEntityRendererRegistry}, {@code EntityRendererRegistry}, {@code ParticleProviderRegistry}).</li>
 * </ul>
 */
public final class SpaceClient {
    private static boolean initialized;

    private SpaceClient() {}

    public static void init() {
        if (initialized) return;
        initialized = true;

        // ── S2C-Empfaenger (je Runde/Agent ein eigener Abschnitt) ──

        // R1-A Himmelskoerper: Trait-Sync, Client-Trait-Ticks
        com.space_m.celestial.client.CelestialClient.init();

        // R1-C Himmel: DimensionSpecialEffects, Nebel, HBM-Skybox-Override, Client-Tick
        com.space_m.client.sky.SkyRegistration.init();

        // R3-2 Duna: Staubsturm-Sync + Staubpartikel
        com.space_m.dim.duna.client.DunaClient.init();

        // R2-A Atmosphaere: Luftvorrat-Sync, Luftblasen-HUD, Depress-Renderer/-Partikel
        com.space_m.atmosphere.client.AtmosphereClient.init();

        // R2-B Lebenserhaltung: PLSS-Atemgeraeusch, Tooltip armor.canSeal
        com.space_m.lifesupport.LifeSupportClient.init();

        // Schwerkraft: schwebende Orbit-Items in der Client-Vorhersage
        com.space_m.gravity.client.CelestialGravityClient.init();

        ClientLifecycleEvent.CLIENT_SETUP.register(SpaceClient::setup);
    }

    private static void setup(Minecraft mc) {
        // R3-1 Ike: Alkylbromid-Fluessigkeit durchscheinend
        com.space_m.dim.ike.IkeClient.setup();
        // R1-C Himmel: Planetarium-Renderer
        com.space_m.client.sky.SkyRegistration.setup();
        com.space_m.dim.eve.client.EveClient.setup(); // R3-3 Entladungsgeysir-Renderer, Quecksilber
        // R2-B Lebenserhaltung: BER Luftwascher
        com.space_m.lifesupport.LifeSupportClient.setup();
        // R2-C Atmo-Maschinen: BER Kompressor/Emitter
        com.space_m.machine.atmo.client.AtmoClient.setup();
        SpaceRegistry.LOGGER.info("[space_m] Client setup finished");
    }

    /** Registry-Zugriff fuer C2S-Puffer auf 1.21.1 (wie HBM {@code ModPacketHandler.getClientRegistryAccess}). */
    public static RegistryAccess registryAccess() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) return mc.level.registryAccess();
        if (mc.getConnection() != null) return mc.getConnection().registryAccess();
        return RegistryAccess.EMPTY;
    }
}
