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

        // R5-A Raketen: Screen, BER, Item-Eigenschaften, Tooltips der HBM-Teile
        com.space_m.rocket.client.RocketClient.init();

        // R5-C Transporter/Drop-Pods: Linker-Paket, Kapsel-Renderer, Linker-GUI
        com.space_m.rocket.transport.client.TransportClient.init();

        // R6-A Orbit/Station: Stations-Sync, Station am Himmel, Stationsbrummen
        com.space_m.orbit.client.OrbitClient.init();

        // R6-C Satelliten: Sync, Satelliten am Himmel (SkyHooks-Dekorator), Einstellungs-GUI, Tooltips
        com.space_m.satellite.client.SatelliteClient.init();

        // R5-B Rakete/Startrampe: Renderer, Geraeusche, Blick-Overlay
        com.space_m.rocket.entity.client.RocketEntityClient.init();
        // R7-B StarDar (S2C-Empfaenger)
        com.space_m.machine.mining.client.DishClient.init();

        // R7-A: Verbundtextur deco_stainless (Mod-Bus ModelEvent.ModifyBakingResult)
        com.space_m.machine.misc.client.DecoStainlessCt.init();

        // R8-B Krieg/Invasion: Renderer, Nuke-Shocks (SkyHooks-Dekorator), Meteore, Kriegsprojektile; Erz-Tooltips
        com.space_m.war.client.WarClient.init();
        com.space_m.resources.client.ResourcesClient.init();

        // R8-A Mobs: Renderer
        com.space_m.entity.mob.client.MobClient.init();
        com.space_m.bomb.client.BombClient.init(); // R9-B1 Antimaterie-Bombe, Explosions-Renderer, Pilz Typ 2

        // R9-B4: LPW-2-Overlay, Drop-Pod-Teilrenderer, DuoDec-Notenpartikel
        SpaceMiscClient.init();

        // E11 Fork-Wackelkoepfe/-Plueschtiere (Renderer)
        com.space_m.fork.trinket.client.ForkTrinketClient.init();

        ClientLifecycleEvent.CLIENT_SETUP.register(SpaceClient::setup);
    }

    private static void setup(Minecraft mc) {
        // R9-B: Laythe-Schlick-Toenung (Schlickhoehlen); erst im Client-Setup (im Datagen gibt es kein Minecraft)
        com.space_m.dim.laythe.client.LaytheClient.init();
        // E11: Fork-Kassetten - Overlay in Trackfarbe (wie HBMs ItemCassette.getColor)
        dev.architectury.registry.client.rendering.ColorHandlerRegistry.registerItemColors(
                (stack, tintIndex) -> 0xFF000000 | com.hbm_m.item.machine.ItemCassette.getColor(stack, tintIndex),
                com.space_m.fork.ForkItems.CASSETTE_DUCC, com.space_m.fork.ForkItems.CASSETTE_XBOX_LIVE);

        // R3-1 Ike: Alkylbromid-Fluessigkeit durchscheinend
        com.space_m.dim.ike.IkeClient.setup();
        // R1-C Himmel: Planetarium-Renderer
        com.space_m.client.sky.SkyRegistration.setup();
        com.space_m.dim.eve.client.EveClient.setup(); // R3-3 Entladungsgeysir-Renderer, Quecksilber
        // R2-B Lebenserhaltung: BER Luftwascher
        com.space_m.lifesupport.LifeSupportClient.setup();
        // R2-C Atmo-Maschinen: BER Kompressor/Emitter
        com.space_m.machine.atmo.client.AtmoClient.setup();
        // R5-A Raketen: BER VAB, Laufwerk-Itemeigenschaft
        com.space_m.rocket.client.RocketClient.setup();
        // R5-C Transporter/Drop-Pods: GUI Rampe, BER Rampe/Kapsel
        com.space_m.rocket.transport.client.TransportClient.setup();
        // R5-B Startrampe: GUI + BER
        com.space_m.rocket.entity.client.RocketEntityClient.setup();
        // R6-A Stationsports: BER
        com.space_m.orbit.client.OrbitClient.setup();
        // R6-B Stationsantrieb/-computer/-launcher: BER + GUIs
        com.space_m.orbit.propulsion.client.PropulsionClient.setup();
        // R7-B Chemie/Bergbau/StarDar (Renderer, Screens, Item-Darstellung)
        com.space_m.machine.chem.client.ChemClient.setup();
        com.space_m.machine.mining.client.MiningClient.setup();
        com.space_m.machine.mining.client.DishClient.setup();
        // R7-A Dyson-System + Sonstiges: BER, GUIs, Item-Renderer, Partikel spinlaunch/flare
        com.space_m.machine.dyson.client.DysonClient.setup();
        com.space_m.machine.misc.client.MiscClient.setup();
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
