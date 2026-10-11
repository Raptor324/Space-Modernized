package com.space_m.client.sky;

import com.space_m.SpaceMod;
import com.space_m.blockentity.SpaceBlockEntities;
import com.space_m.celestial.CelestialEnvironment;
import com.space_m.client.orrery.OrreryRenderer;
import com.space_m.dim.SpaceDimensions;
import com.space_m.main.SpaceRegistry;

import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.ReloadListenerRegistry;
import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.level.Level;
//? if forge {
import net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
//?} elif neoforge {
/*import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.bus.api.EventPriority;
*///?}

/**
 * Client-Verdrahtung des Himmels (nur physischer Client, aus {@code SpaceClient.init()} im Mod-Konstruktor):
 * <ul>
 *   <li>{@code RegisterDimensionSpecialEffectsEvent} (Mod-Bus, beide Loader): {@code minecraft:overworld} (Kerbin,
 *       Original {@code WorldProviderEarth} ersetzt den Oberwelt-Provider) und je Space-Dimension
 *       {@code space_m:<koerper>} (Effekt-Schluessel des {@code dimension_type}).</li>
 *   <li>{@code ViewportEvent.ComputeFogColor/RenderFog} (Spiel-Bus): Nebelfarbe/-dichte.</li>
 *   <li>HBM-Himmel stilllegen: {@code CelestialEnvironment.setSkyOverride} (HBM {@code RenderNTMSkybox}/Impact-Mixins
 *       fragen {@code SpaceEnvironment.overridesSky}).</li>
 *   <li>Client-Tick (Meteore, Eve-Donner, Thatmo-Schlacht, Leitstern), Ressourcen-Neuladen (Shader/OBJ),
 *       Planetarium-BER, Meteor-Hook von {@code CBT_Invasion}.</li>
 * </ul>
 */
public final class SkyRegistration {
    private SkyRegistration() {}

    private static boolean initialized;

    public static void init() {
        if (initialized) return;
        initialized = true;

        //? if forge {
        @SuppressWarnings("removal")
        net.minecraftforge.eventbus.api.IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(SkyRegistration::onRegisterEffects);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGH, false, ViewportEvent.ComputeFogColor.class, SkyRegistration::onFogColor);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGH, false, ViewportEvent.RenderFog.class, SkyRegistration::onRenderFog);
        //?} elif neoforge {
        /*net.neoforged.bus.api.IEventBus modBus = net.neoforged.fml.ModLoadingContext.get().getActiveContainer().getEventBus();
        modBus.addListener(SkyRegistration::onRegisterEffects);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, false, ViewportEvent.ComputeFogColor.class, SkyRegistration::onFogColor);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, false, ViewportEvent.RenderFog.class, SkyRegistration::onRenderFog);
        *///?}

        CelestialEnvironment.setSkyOverride(level -> level.isClientSide() && SkyDispatch.active(level));
        SkyDispatch.installInvasionHook();

        ClientTickEvent.CLIENT_POST.register(SkyDispatch::clientTick);

    }

    /** Im Client-Setup (Registries stehen): Planetarium-Renderer. */
    public static void setup() {
        // Erst hier: im Mod-Konstruktor gibt es im Datagen-Lauf (Dist CLIENT, aber kein Minecraft) keinen Ressourcenmanager
        if (net.minecraft.client.Minecraft.getInstance() != null) {
            ReloadListenerRegistry.register(PackType.CLIENT_RESOURCES, (ResourceManagerReloadListener) manager -> {
                SkyShaders.reload();
                SkyObjModel.PLANE.reset();
                SkyObjModel.SPHERE_V2.reset();
                SkyObjModel.SAT_RAIL.reset();
            }, SpaceMod.id("sky"));
        }
        BlockEntityRendererRegistry.register(SpaceBlockEntities.ORRERY.get(), OrreryRenderer::new);
    }

    // ── Effekte ──

    private static void onRegisterEffects(RegisterDimensionSpecialEffectsEvent event) {
        // Kerbin = Oberwelt (Vanilla-Wolkenhoehe 192 der 1.18+-Oberwelt)
        event.register(ResourceLocation.fromNamespaceAndPath("minecraft", "overworld"),
                new CelestialEffects(DimensionSpecialEffects.OverworldEffects.CLOUD_LEVEL, new CelestialSkyProvider(),
                        new CelestialSkyRenderer(), new DimensionSpecialEffects.OverworldEffects()));

        for (ResourceKey<Level> dim : SpaceDimensions.ALL) {
            String name = dim.location().getPath();
            // 1.7.10 WorldProvider.getCloudHeight() = 128
            event.register(dim.location(), new CelestialEffects(128.0F, providerFor(name), rendererFor(name), null));
        }
        SpaceRegistry.LOGGER.info("[space_m] Registered celestial sky effects for overworld + {} dimensions", SpaceDimensions.ALL.size());
    }

    /** Himmels-Anteil des Koerper-Providers (Original {@code WorldProvider<Koerper>}). */
    static CelestialSkyProvider providerFor(String name) {
        return switch (name) {
            case "eve" -> new CelestialSkyProvider.Eve();
            case "tekto" -> new CelestialSkyProvider.Tekto();
            case "ike" -> new CelestialSkyProvider.Ike();
            case "laythe" -> new CelestialSkyProvider.Laythe();
            case "duna" -> new CelestialSkyProvider.Duna();
            case "thatmo" -> new CelestialSkyProvider.Thatmo();
            case "orbit" -> new CelestialSkyProvider.Orbit();
            default -> new CelestialSkyProvider();
        };
    }

    /** Original {@code getSkyRenderer()} des Koerper-Providers. */
    static CelestialSkyRenderer rendererFor(String name) {
        return switch (name) {
            case "laythe" -> new LaytheSkyRenderer();
            case "thatmo" -> new ThatmoSkyRenderer();
            case "orbit" -> new OrbitSkyRenderer();
            default -> new CelestialSkyRenderer();
        };
    }

    // ── Nebel ──

    private static void onFogColor(ViewportEvent.ComputeFogColor event) {
        try {
            float[] c = CelestialFog.computeFogColor(event.getCamera(), (float) event.getPartialTick());
            if (c == null) return;
            event.setRed(c[0]);
            event.setGreen(c[1]);
            event.setBlue(c[2]);
        } catch (RuntimeException e) {
            SkyDispatch.logOnce("fogcolor", e);
        }
    }

    private static void onRenderFog(ViewportEvent.RenderFog event) {
        try {
            float[] d = CelestialFog.fogDistances(event.getCamera(), event.getFarPlaneDistance());
            if (d == null) return;
            event.setNearPlaneDistance(d[0]);
            event.setFarPlaneDistance(d[1]);
            event.setCanceled(true);
        } catch (RuntimeException e) {
            SkyDispatch.logOnce("fogdensity", e);
        }
    }
}
