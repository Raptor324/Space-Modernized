package com.space_m.entity.mob;

import java.util.List;
import java.util.function.Supplier;

import com.space_m.block.SpaceBlocks;
import com.space_m.entity.SpaceEntities;
import com.space_m.entity.mob.block.BlockStalagmiteFlesh;
import com.space_m.entity.mob.block.BlockTumor;
import com.space_m.item.SpaceItems;
import com.space_m.main.SpaceCreativeTabs;

import dev.architectury.core.item.ArchitecturySpawnEggItem;
import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * R8-A: die Mobs von NTM Space ({@code EntityMappings.addMob}) mit Spawn-Eiern - ausser Siege-UFO
 * ({@code entity_meme_ufo}) und War Behemoth, die R8-B mit dem Kriegssystem baut ({@code com.space_m.war}) -, dazu {@code spawn_angel}
 * ({@code ItemChopper}) und die Fleischbloecke ({@code tumor}, {@code stalactite/stalagmite} Metadatum 2), auf denen der
 * Fleisch-Creeper spawnt.
 *
 * <p>Registry-ID = Original-Mappingname ({@code entity_moon_cow} ...). Original {@code ModEntityList.registerEntity}
 * meldet Mobs mit Reichweite 80 Bloecke (5 Chunks), Intervall 3, Geschwindigkeitsupdates an. Spawn-Ei-Farben
 * 1:1 aus {@code addMob}. Groessen 1:1 {@code setSize} (fehlt sie, die Vorgabe der Elternklasse von 1.7.10).
 *
 * <p>{@link #init()} wird in {@code SpaceRegistry} vor {@code SpaceBlocks.init()} aufgerufen (laedt die Klasse,
 * damit alle Eintraege vor dem {@code register()} der gemeinsamen Register stehen).
 */
public final class SpaceMobs {

    private SpaceMobs() {}

    // ── Entities ────────────────────────────────────────────────────────────────────────────────

    public static final RegistrySupplier<EntityType<EntityMoonCow>> MOON_COW = mob("entity_moon_cow",
            () -> EntityType.Builder.<EntityMoonCow>of(EntityMoonCow::new, MobCategory.CREATURE).sized(0.9F, 1.3F));
    public static final RegistrySupplier<EntityType<EntityScutterfish>> SCUTTERFISH = mob("entity_scutterfish",
            () -> EntityType.Builder.<EntityScutterfish>of(EntityScutterfish::new, MobCategory.WATER_CREATURE).sized(2.0F, 2.0F));
    public static final RegistrySupplier<EntityType<EntityScuttlecrab>> SCUTTLECRAB = mob("entity_scuttlecrab",
            () -> EntityType.Builder.<EntityScuttlecrab>of(EntityScuttlecrab::new, MobCategory.WATER_CREATURE).sized(0.6F, 1.8F));
    public static final RegistrySupplier<EntityType<EntityDepthSquid>> DEPTH_SQUID = mob("entity_depthsquid",
            () -> EntityType.Builder.<EntityDepthSquid>of(EntityDepthSquid::new, MobCategory.WATER_CREATURE).sized(0.95F, 0.95F));
    public static final RegistrySupplier<EntityType<EntityScrapFish>> SCRAP_FISH = mob("entity_scrapfish",
            () -> EntityType.Builder.<EntityScrapFish>of(EntityScrapFish::new, MobCategory.WATER_CREATURE).sized(2.0F, 2.0F));
    public static final RegistrySupplier<EntityType<EntitySifterEel>> SIFTER_EEL = mob("entity_siftereel",
            () -> EntityType.Builder.<EntitySifterEel>of(EntitySifterEel::new, MobCategory.WATER_CREATURE).sized(2.0F, 2.0F));
    public static final RegistrySupplier<EntityType<EntityCreeperFlesh>> CREEPER_FLESH = mob("entity_mob_flesh_creeper",
            () -> EntityType.Builder.<EntityCreeperFlesh>of(EntityCreeperFlesh::new, MobCategory.MONSTER).sized(0.6F, 1.8F));
    public static final RegistrySupplier<EntityType<EntityTankbot>> TANKBOT = mob("entity_tankbot",
            () -> EntityType.Builder.<EntityTankbot>of(EntityTankbot::new, MobCategory.MONSTER).sized(2.45F, 3.25F).fireImmune());
    public static final RegistrySupplier<EntityType<EntityBFAngel>> BF_ANGEL = mob("entity_ntm_bfa",
            () -> EntityType.Builder.<EntityBFAngel>of(EntityBFAngel::new, MobCategory.MONSTER).sized(5.0F, 4.0F).fireImmune());
    /** Original {@code EntitySiegeCraft} der Space-Fassung ("Macro UFO"), HBM-Klasse + Lebenszeit/Rueckzug. */
    public static final RegistrySupplier<EntityType<EntitySiegeCraftSpace>> MICRO_UFO = mob("entity_micro_ufo",
            () -> EntityType.Builder.<EntitySiegeCraftSpace>of(EntitySiegeCraftSpace::new, MobCategory.MONSTER).sized(7.0F, 1.0F).fireImmune());

    /**
     * {@code ISuffocationImmune} im Original (MoonCow, Tankbot, WarBehemoth, BFAngel, UFOBase -> SiegeUFO/SiegeCraft).
     * Siege-UFO und War Behemoth registriert R8-B ({@code com.space_m.war.WarEntities}); ihre IDs stehen trotzdem hier.
     */
    public static final List<String> SUFFOCATION_IMMUNE = List.of("space_m:entity_moon_cow", "space_m:entity_tankbot",
            "space_m:entity_war_behemoth", "space_m:entity_ntm_bfa", "space_m:entity_meme_ufo", "space_m:entity_micro_ufo");

    // ── Bloecke (Original ModBlocks.tumor, BlockStalagmite Metadatum 2 "flesh") ─────────────────────

    /** {@code new BlockTint(Material.clay, "tumor_base2", "tumor_base3", "fleshv2")}, Haerte 1, Schnee-Schrittgeraeusch, blockTab. */
    public static final RegistrySupplier<BlockTumor> TUMOR = SpaceBlocks.dropsSelf(SpaceBlocks.registerWithItem("tumor",
            () -> new BlockTumor(BlockBehaviour.Properties.of().mapColor(MapColor.CLAY).strength(1.0F, 1.0F).sound(SoundType.SNOW)),
            SpaceCreativeTabs.Tab.BLOCKS));
    /** {@code stalactite}/{@code stalagmite} Metadatum 2: Haerte 0.5, Widerstand 2 -> 2*3/5 = 1.2, blockTab. */
    public static final RegistrySupplier<BlockStalagmiteFlesh> STALACTITE_FLESH = SpaceBlocks.registerWithItem("stalactite_flesh",
            () -> new BlockStalagmiteFlesh(BlockStalagmiteFlesh.props(), true), SpaceCreativeTabs.Tab.BLOCKS);
    public static final RegistrySupplier<BlockStalagmiteFlesh> STALAGMITE_FLESH = SpaceBlocks.registerWithItem("stalagmite_flesh",
            () -> new BlockStalagmiteFlesh(BlockStalagmiteFlesh.props(), false), SpaceCreativeTabs.Tab.BLOCKS);

    // ── Items ────────────────────────────────────────────────────────────────────────────────────

    /** Original {@code spawn_angel = new ItemChopper()}: consumableTab, Stapel 1. */
    public static final RegistrySupplier<ItemSpawnAngel> SPAWN_ANGEL = SpaceItems.basic("spawn_angel",
            () -> new ItemSpawnAngel(new Item.Properties().stacksTo(1)), SpaceCreativeTabs.Tab.CONSUMABLE);

    // Spawn-Eier (Original addMob: Vanilla-Spawn-Ei mit den beiden Farben)
    public static final RegistrySupplier<ArchitecturySpawnEggItem> MOON_COW_EGG = egg("moon_cow", MOON_COW, 0xECD376, 0x262A44);
    public static final RegistrySupplier<ArchitecturySpawnEggItem> SCUTTERFISH_EGG = egg("scutterfish", SCUTTERFISH, 0xC8C9CD, 0x858894);
    public static final RegistrySupplier<ArchitecturySpawnEggItem> SCUTTLECRAB_EGG = egg("scuttlecrab", SCUTTLECRAB, 0xF17951, 0xEDDABB);
    public static final RegistrySupplier<ArchitecturySpawnEggItem> DEPTH_SQUID_EGG = egg("depthsquid", DEPTH_SQUID, 0x00B4DF, 0x016085);
    public static final RegistrySupplier<ArchitecturySpawnEggItem> SCRAP_FISH_EGG = egg("scrapfish", SCRAP_FISH, 0xDF9835, 0x510E13);
    public static final RegistrySupplier<ArchitecturySpawnEggItem> SIFTER_EEL_EGG = egg("siftereel", SIFTER_EEL, 0x5B963E, 0xC0B286);
    public static final RegistrySupplier<ArchitecturySpawnEggItem> MICRO_UFO_EGG = egg("micro_ufo", MICRO_UFO, 0x5B963E, 0xC0B286);
    public static final RegistrySupplier<ArchitecturySpawnEggItem> CREEPER_FLESH_EGG = egg("flesh_creeper", CREEPER_FLESH, 0xFF0000, 0xD65337);
    public static final RegistrySupplier<ArchitecturySpawnEggItem> TANKBOT_EGG = egg("tankbot", TANKBOT, 0x204131, 0x75CE00);
    public static final RegistrySupplier<ArchitecturySpawnEggItem> BF_ANGEL_EGG = egg("ntm_bfa", BF_ANGEL, 0x204131, 0x75CE00);

    /** Reihenfolge der Spawn-Eier im Vanilla-Reiter "Spawn-Eier" (Original: Reihenfolge der addMob-Aufrufe). */
    private static final List<RegistrySupplier<ArchitecturySpawnEggItem>> EGGS = List.of(TANKBOT_EGG, BF_ANGEL_EGG,
            CREEPER_FLESH_EGG, MOON_COW_EGG, SCUTTERFISH_EGG, SCUTTLECRAB_EGG, DEPTH_SQUID_EGG, SCRAP_FISH_EGG, SIFTER_EEL_EGG, MICRO_UFO_EGG);

    public static List<RegistrySupplier<ArchitecturySpawnEggItem>> eggs() { return EGGS; }

    // ── Helfer ─────────────────────────────────────────────────────────────────────────────────

    private static <T extends Mob> RegistrySupplier<EntityType<T>> mob(String name, Supplier<EntityType.Builder<T>> builder) {
        return SpaceEntities.register(name, () -> builder.get().clientTrackingRange(5).updateInterval(3));
    }

    private static <T extends Mob> RegistrySupplier<ArchitecturySpawnEggItem> egg(String name, RegistrySupplier<EntityType<T>> type, int c1, int c2) {
        return SpaceItems.register(name + "_spawn_egg", () -> new ArchitecturySpawnEggItem(type, c1, c2, new Item.Properties()), null);
    }

    private static boolean initialized;

    /** Aus {@code SpaceRegistry.init()} vor den gemeinsamen Registern. */
    public static void init() {
        if (initialized) return;
        initialized = true;

        // Attribute (Architectury, beide Loader)
        SpaceEntities.attributes(MOON_COW, EntityMoonCow::createAttributes);
        SpaceEntities.attributes(SCUTTERFISH, EntityFish::createFishAttributes);
        SpaceEntities.attributes(SCRAP_FISH, EntityFish::createFishAttributes);
        SpaceEntities.attributes(SIFTER_EEL, EntityFish::createFishAttributes);
        SpaceEntities.attributes(SCUTTLECRAB, EntityScuttlecrab::createAttributes);
        SpaceEntities.attributes(DEPTH_SQUID, EntityDepthSquid::createAttributes);
        SpaceEntities.attributes(CREEPER_FLESH, EntityCreeperFlesh::createAttributes);
        SpaceEntities.attributes(TANKBOT, EntityTankbot::createAttributes);
        SpaceEntities.attributes(BF_ANGEL, EntityBFAngel::createAttributes);
        SpaceEntities.attributes(MICRO_UFO, EntitySiegeCraftSpace::createAttributes);

        // Spawn-Eier: Original im Vanilla-Reiter (Spawn-Eier statt "Verschiedenes")
        CreativeTabRegistry.modifyBuiltin(BuiltInRegistries.CREATIVE_MODE_TAB.getOrThrow(CreativeModeTabs.SPAWN_EGGS),
                (flags, output, canUseOps) -> {
                    for (RegistrySupplier<ArchitecturySpawnEggItem> egg : EGGS) output.accept(new ItemStack(egg.get()));
                });

        // Spawn-Regeln (Platzierung, Fleisch auf tumor) - Loader-Ereignisse
        MobSpawns.init();
    }
}
