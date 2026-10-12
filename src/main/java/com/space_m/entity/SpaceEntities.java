package com.space_m.entity;

import java.util.function.Supplier;

import com.space_m.SpaceMod;

import dev.architectury.registry.level.entity.EntityAttributeRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;

/**
 * Entities des Addons (Original: 12 Mobs + Rakete/Drop-Pod/Depress von NTM Space, {@code EntityMappings}).
 * ID = Original-Mapping-Name (kleingeschrieben). Attribute ueber {@link #attributes} (Architectury, beide Loader).
 */
public final class SpaceEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(SpaceMod.MOD_ID, Registries.ENTITY_TYPE);

    // ── Eintraege (je Runde/Agent ein eigener, kommentierter Abschnitt) ─────────────────────────

    // R2-A Atmosphaere: Druckverlust am Leck (Original EntityMappings "entity_depress", Tracking 250)
    public static final RegistrySupplier<EntityType<com.space_m.atmosphere.EntityDepress>> DEPRESS = register("entity_depress",
            () -> EntityType.Builder.<com.space_m.atmosphere.EntityDepress>of(com.space_m.atmosphere.EntityDepress::new,
                    net.minecraft.world.entity.MobCategory.MISC).sized(0.6F, 1.8F).fireImmune().clientTrackingRange(16));

    // R5-C Drop-Pod (Original EntityMappings "entity_combat_pod", Tracking 1000 -> 63 Chunks; EntityThrowable 0,25 x 0,25)
    public static final RegistrySupplier<EntityType<com.space_m.rocket.transport.EntityCombatDropPod>> COMBAT_POD = register("entity_combat_pod",
            () -> EntityType.Builder.<com.space_m.rocket.transport.EntityCombatDropPod>of(com.space_m.rocket.transport.EntityCombatDropPod::new,
                    net.minecraft.world.entity.MobCategory.MISC).sized(0.25F, 0.25F).fireImmune().clientTrackingRange(63).updateInterval(1));

    // R5-B Rakete (Original EntityMappings "entity_rideable_rocket"/"_dummy", Tracking 1000 -> 63 Chunks; setSize(2, 8) bzw. (4, 2.5))
    public static final RegistrySupplier<EntityType<com.space_m.rocket.entity.EntityRideableRocket>> ENTITY_RIDEABLE_ROCKET = register("entity_rideable_rocket",
            () -> EntityType.Builder.<com.space_m.rocket.entity.EntityRideableRocket>of(com.space_m.rocket.entity.EntityRideableRocket::new,
                    net.minecraft.world.entity.MobCategory.MISC).sized(2F, 8F).fireImmune().clientTrackingRange(63).updateInterval(1));
    public static final RegistrySupplier<EntityType<com.space_m.rocket.entity.EntityRideableRocket.EntityRideableRocketDummy>> ENTITY_RIDEABLE_ROCKET_DUMMY = register("entity_rideable_rocket_dummy",
            () -> EntityType.Builder.<com.space_m.rocket.entity.EntityRideableRocket.EntityRideableRocketDummy>of(com.space_m.rocket.entity.EntityRideableRocket.EntityRideableRocketDummy::new,
                    net.minecraft.world.entity.MobCategory.MISC).sized(4F, 2.5F).fireImmune().noSave().clientTrackingRange(63).updateInterval(1));

    private SpaceEntities() {}

    /** {@code builder.build(id)} - Name wie in HBM-Modernized ({@code ModEntities}). */
    public static <T extends net.minecraft.world.entity.Entity> RegistrySupplier<EntityType<T>> register(
            String name, Supplier<EntityType.Builder<T>> builder) {
        return ENTITIES.register(name, () -> builder.get().build(SpaceMod.MOD_ID + ":" + name));
    }

    /** Attribut-Satz eines Mobs anmelden (vor dem Laden der Welt, d.h. im Mod-Konstruktor). */
    public static void attributes(Supplier<? extends EntityType<? extends LivingEntity>> type,
                                  Supplier<AttributeSupplier.Builder> attributes) {
        EntityAttributeRegistry.register(type, attributes);
    }

    public static void init() {
        // R8-B Krieg/Invasion: Siege-UFO, War-Behemoth, Carpet-Jet + Bomblet (eigene Klasse, vor ENTITIES.register())
        com.space_m.war.WarEntities.init();
        ENTITIES.register();
    }
}
