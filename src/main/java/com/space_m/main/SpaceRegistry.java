package com.space_m.main;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.space_m.block.SpaceBlocks;
import com.space_m.blockentity.SpaceBlockEntities;
import com.space_m.config.SpaceConfig;
import com.space_m.entity.SpaceEntities;
import com.space_m.fluid.SpaceFluids;
import com.space_m.inventory.SpaceMenus;
import com.space_m.item.SpaceItems;
import com.space_m.network.SpacePacketHandler;
import com.space_m.particle.SpaceParticles;
import com.space_m.sound.SpaceSounds;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;

/**
 * Gemeinsame (loaderneutrale) Initialisierung des Addons - Gegenstueck zu {@code com.hbm_m.main.MainRegistry}.
 *
 * <p>Reihenfolge: Config -> Netzwerk (NeoForge verlangt Payload-Anmeldung im Mod-Konstruktor) ->
 * Registries -> Creative-Tabs -> Client. Neue Registries hier eintragen (je eine Zeile).
 */
public final class SpaceRegistry {
    public static final Logger LOGGER = LogUtils.getLogger();

    private SpaceRegistry() {}

    public static void init() {
        LOGGER.info("[space_m] Initializing Space Modernized");

        // Config vor allem anderen (Original: SpaceConfig/WorldConfig/... werden in preInit geladen).
        SpaceConfig.load();

        // Netzwerk: C2S + (Server) S2C-Typen. Client-Empfaenger meldet SpaceClient an.
        SpacePacketHandler.register();

        // Registries (Architectury DeferredRegister, beide Loader)
        SpaceSounds.init();
        SpaceParticles.init();
        SpaceFluids.init();
        // R1-B: Dimensions-Framework (Generator/Biomquelle/Features) + Koerper-Bloecke (vor SpaceBlocks.init)
        com.space_m.dim.SpaceWorldgen.init();
        // R4-B: Feldfruechte + Gummi-Ackerboden (Bloecke vor SpaceBlocks.init), Trankeffekt run
        com.space_m.item.SpaceCrops.init();
        com.space_m.item.SpaceEffects.init();
        com.space_m.material.SpaceMats.init();
        SpaceBlocks.init();
        SpaceItems.init();
        SpaceBlockEntities.init();
        SpaceMenus.init();
        SpaceEntities.init();

        // Space-Eintraege in die 9 Original-Tabs (in HBM-Modernized registriert)
        SpaceCreativeTabs.init();

        // R1-A: Sonnensystem/Himmelskoerper, SpaceEnvironment bei HBM anmelden, Trait-Ticks + Sync
        com.space_m.celestial.CelestialRegistry.init();

        // R1-D: Reise (CelestialTeleporter-Warteschlange), Befehle, Zeit/Wetter je Himmelskoerper
        com.space_m.travel.SpaceTravel.init();

        // R2-A: Atmosphaere (abgedichtete Raeume, Ersticken, Welt-Effekte, Anbindung an CelestialEnvironment)
        com.space_m.atmosphere.ChunkAtmosphereManager.register();

        // R2-C: Atmo-Maschinen (Atmosphaeren-Chemie, Fluid-Trait terraformer)
        com.space_m.machine.atmo.AtmoMachines.init();

        // R2-B: Lebenserhaltung (schwere Stiefel beidseitig, oxy_pinwheel noDualUnload)
        com.space_m.lifesupport.LifeSupportEvents.init();

        // Schwerkraft je Himmelskoerper (ModEventHandler.onLivingUpdate/onEntityFall/Orbit-Items)
        com.space_m.gravity.CelestialGravity.register();

        LifecycleEvent.SETUP.register(SpaceRegistry::commonSetup);

        // Client-Einstieg (Renderer, Screens, S2C-Empfaenger); auf dem Server nie geladen.
        EnvExecutor.runInEnv(Env.CLIENT, () -> com.space_m.client.SpaceClient::init);
    }

    private static void commonSetup() {
        // R4-B: Schmelzwerte (Mats.materialEntries) + Gefahren der Space-Gegenstaende
        com.space_m.material.SpaceMats.registerEntries();
        com.space_m.item.SpaceHazards.register();
        com.space_m.material.SpaceCrucible.register();
        LOGGER.info("[space_m] Common setup finished");
    }
}
