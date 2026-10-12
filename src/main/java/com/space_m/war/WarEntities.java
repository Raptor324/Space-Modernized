package com.space_m.war;

import java.util.function.Consumer;

import com.space_m.entity.SpaceEntities;
import com.space_m.item.SpaceItems;

import dev.architectury.core.item.ArchitecturySpawnEggItem;
import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

/**
 * Entities des Kriegs-/Invasionssystems (R8-B). IDs = Original {@code EntityMappings}-Namen:
 * {@code entity_meme_ufo} (EntitySiegeUFO, Ei 0x303030/0x800000), {@code entity_war_behemoth} (Ei 0x204131/0x75CE00);
 * Carpet-Jet = Original {@code EntityBomber} Typ 9 ({@code entity_bomber}, Tracking 1000) als eigene ID
 * {@code carpet_jet}, sein Bomblet {@code carpet_bomblet}. Spawn-Eier (Original addMob -> Vanilla-Ei) im
 * Spawn-Ei-Tab.
 */
public final class WarEntities {

    private WarEntities() {}

    public static final RegistrySupplier<EntityType<EntitySiegeUFO>> SIEGE_UFO = SpaceEntities.register("entity_meme_ufo",
            () -> EntityType.Builder.<EntitySiegeUFO>of(EntitySiegeUFO::new, MobCategory.MONSTER)
                    .sized(1.5F, 1F).clientTrackingRange(16).updateInterval(1));

    public static final RegistrySupplier<EntityType<EntityWarBehemoth>> WAR_BEHEMOTH = SpaceEntities.register("entity_war_behemoth",
            () -> EntityType.Builder.<EntityWarBehemoth>of(EntityWarBehemoth::new, MobCategory.MONSTER)
                    .sized(1.75F, 6.35F).clientTrackingRange(16).updateInterval(1));

    public static final RegistrySupplier<EntityType<EntityCarpetJet>> CARPET_JET = SpaceEntities.register("carpet_jet",
            () -> EntityType.Builder.<EntityCarpetJet>of(EntityCarpetJet::new, MobCategory.MISC)
                    .sized(8.0F, 4.0F).clientTrackingRange(63).updateInterval(1));

    public static final RegistrySupplier<EntityType<EntityCarpetBomblet>> CARPET_BOMBLET = SpaceEntities.register("carpet_bomblet",
            () -> EntityType.Builder.<EntityCarpetBomblet>of(EntityCarpetBomblet::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(8).updateInterval(1));

    public static RegistrySupplier<Item> SIEGE_UFO_EGG;
    public static RegistrySupplier<Item> WAR_BEHEMOTH_EGG;

    /** Client-Tick des Carpet-Jets (Schleifenton), vom Client-Teil gesetzt. */
    public static Consumer<EntityCarpetJet> clientTick = jet -> { };

    /** Aus {@code SpaceEntities.init()} (vor {@code ENTITIES.register()}). */
    public static void init() {
        SpaceEntities.attributes(SIEGE_UFO, EntitySiegeUFO::createAttributes);
        SpaceEntities.attributes(WAR_BEHEMOTH, EntityWarBehemoth::createAttributes);
    }

    /** Aus {@code SpaceItems.init()} (vor {@code ITEMS.register()}). */
    public static void initItems() {
        SIEGE_UFO_EGG = SpaceItems.register("entity_meme_ufo_spawn_egg",
                () -> new ArchitecturySpawnEggItem(SIEGE_UFO, 0x303030, 0x800000, new Item.Properties()), null);
        WAR_BEHEMOTH_EGG = SpaceItems.register("entity_war_behemoth_spawn_egg",
                () -> new ArchitecturySpawnEggItem(WAR_BEHEMOTH, 0x204131, 0x75CE00, new Item.Properties()), null);
        CreativeTabRegistry.append(CreativeModeTabs.SPAWN_EGGS, SIEGE_UFO_EGG);
        CreativeTabRegistry.append(CreativeModeTabs.SPAWN_EGGS, WAR_BEHEMOTH_EGG);
    }
}
