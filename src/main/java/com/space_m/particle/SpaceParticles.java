package com.space_m.particle;

import com.space_m.SpaceMod;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;

/**
 * Partikeltypen des Addons (Original: 10 neue Partikel von NTM Space, v.a. Raketenabgas).
 * Provider (Client) meldet {@link com.space_m.client.SpaceClient} ueber Architectury {@code ParticleProviderRegistry} an.
 */
public final class SpaceParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(SpaceMod.MOD_ID, Registries.PARTICLE_TYPE);

    // ── Eintraege (je Runde/Agent ein eigener, kommentierter Abschnitt) ─────────────────────────

    // R3-2 Duna: Staubsturm (Original ParticleDust, effectNT "duststorm")
    public static final RegistrySupplier<SimpleParticleType> DUST_STORM = simple("dust_storm", false);

    private SpaceParticles() {}

    /** Einfacher Partikeltyp ohne Parameter ({@code alwaysShow} = auch bei "Partikel: minimal"). */
    public static RegistrySupplier<SimpleParticleType> simple(String name, boolean alwaysShow) {
        return PARTICLES.register(name, () -> new SimpleParticleType(alwaysShow) {});
    }

    public static void init() {
        PARTICLES.register();
    }
}
