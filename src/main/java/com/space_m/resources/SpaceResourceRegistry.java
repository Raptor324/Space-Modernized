package com.space_m.resources;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

import com.hbm_m.api.space.SpaceResources;
import com.hbm_m.inventory.FluidContainerRegistry;
import com.hbm_m.inventory.FluidContainerRegistry.FluidContainer;
import com.mojang.logging.LogUtils;
import com.space_m.SpaceMod;
import com.space_m.celestial.CelestialBody;
import com.space_m.celestial.CelestialEnvironment;
import com.space_m.dim.PlanetOres;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.registry.level.biome.BiomeModifications;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * R8-B Planeten-Ressourcen: Einstieg (aus {@code SpaceRegistry.init()}).
 * <ul>
 *   <li>Bedrock-Erze je Koerper: Feature {@code space_m:celestial_bedrock_ore} in allen Space-Biomen (Architectury
 *       {@link BiomeModifications}, Schritt {@code UNDERGROUND_ORES} = Original nach {@code generateNTMOres}),
 *       Sortentabelle {@link CelestialBedrockOre} bei HBM angemeldet ({@link SpaceResources#registerBedrockOre}).</li>
 *   <li>Lagerstaetten ({@link BlockOreFluid}): {@link SpaceResources#registerFluidDeposit} +
 *       {@link SpaceResources#registerDepositFluids} je Koerper; Fass-Inhalt der Erzbloecke 1:1
 *       {@code FluidContainerRegistry} (ore_oil 250 mB Rohoel, ore_gas 100 mB Erdgas, ore_shale 250/50 mB Erdgas).</li>
 *   <li>Erz-Tooltip "Can be found on": {@link OreLocations} (Daten) + {@code ResourcesClient}.</li>
 * </ul>
 */
public final class SpaceResourceRegistry {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(SpaceMod.MOD_ID, Registries.FEATURE);

    public static final RegistrySupplier<CelestialBedrockOreFeature> CELESTIAL_BEDROCK_ORE = FEATURES.register("celestial_bedrock_ore", CelestialBedrockOreFeature::new);

    public static final ResourceKey<PlacedFeature> CELESTIAL_BEDROCK_ORE_PLACED =
            ResourceKey.create(Registries.PLACED_FEATURE, SpaceMod.id("celestial_bedrock_ore"));

    private static boolean initialized;
    private static boolean setupDone;

    private SpaceResourceRegistry() {}

    public static void init() {
        if (initialized) return;
        initialized = true;

        CelestialBedrockMats.init();
        FEATURES.register();
        SpaceBedrockOreHooks.install(); // W-R8B-1

        // WorldGeneratorCelestial.generate: alle Space-Dimensionen ausser Orbit (keine Celestial-Provider) und Kerbin
        BiomeModifications.addProperties(
                ctx -> ctx.getKey().map(id -> SpaceMod.MOD_ID.equals(id.getNamespace()) && !"orbit".equals(id.getPath())).orElse(false),
                (ctx, props) -> props.getGenerationProperties().addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, CELESTIAL_BEDROCK_ORE_PLACED));

        LifecycleEvent.SETUP.register(SpaceResourceRegistry::setup);
    }

    /** Common-Setup (Bloecke/Fluide registriert). */
    public static synchronized void setup() {
        if (setupDone) return;
        setupDone = true;

        CelestialBedrockOre.registerWithHbm();
        // Verarbeitungsstufen je Koerpersorte: Fragment-Formen, Lichtbogenofen/Elektrolyse (Rest als Rezept-JSON)
        CelestialBedrockOreItems.registerShapes();
        CelestialBedrockRecipes.register();

        // Lagerstaetten (alle BlockOreFluid-Varianten; HBM-Maschinen fragen BlockOreFluid ueber
        // SpaceResources.FluidDepositBlock ab, W-R8B-2)
        List<BlockOreFluid> deposits = new ArrayList<>();
        for (PlanetOres.Variant v : PlanetOres.variants()) {
            if (v.block().get() instanceof BlockOreFluid fluidOre) {
                deposits.add(fluidOre);
                fluidOre.link();
                Block empty = fluidOre.getEmpty();
                // W-R8B-2 b: ohne leeren Block (Bedrock-Oel, nur Fracking) mit empty = null anmelden
                SpaceResources.registerFluidDeposit(fluidOre.getReserveType(), () -> fluidOre, empty != null ? () -> empty : null);
            }
        }
        for (BlockOreFluid ore : deposits) {
            CelestialBody body = CelestialBody.getBodyOrNull(ore.body.name);
            if (body == null) continue;
            ResourceLocation bodyId = CelestialEnvironment.bodyId(body);
            if (SpaceResources.depositPrimaryFluid(bodyId, ore.getReserveType()) == null && ore.getPrimaryFluid() != null) {
                SpaceResources.registerDepositFluids(bodyId, ore.getReserveType(), ore.getPrimaryFluid(), ore.getSecondaryFluid());
            }
        }

        // FluidContainerRegistry (Original-Schleife ueber alle Metadaten von ore_oil/ore_gas/ore_shale)
        Fluid crude = fluid("hbm_m", "crude_oil");
        Fluid gas = fluid("hbm_m", "gas");
        int shaleGas = com.hbm_m.config.GeneralConfig.enable528 ? 50 : 250;
        for (PlanetOres.Variant v : PlanetOres.variants()) {
            switch (v.ore()) {
                case "ore_oil" -> container(v, crude, 250);
                case "ore_gas" -> container(v, gas, 100);
                case "ore_shale" -> container(v, gas, shaleGas);
                default -> { }
            }
        }

        LOGGER.info("[space_m] R8 resources: {} bedrock ore types, {} fluid deposits", CelestialBedrockOre.getAllTypes().size(), deposits.size());
    }

    private static void container(PlanetOres.Variant v, Fluid fluid, int amount) {
        if (fluid == Fluids.EMPTY) return;
        FluidContainerRegistry.registerContainer(new FluidContainer(new ItemStack(v.block().get()), null, fluid, amount));
    }

    private static Fluid fluid(String ns, String path) {
        return BuiltInRegistries.FLUID.get(ResourceLocation.fromNamespaceAndPath(ns, path));
    }
}
