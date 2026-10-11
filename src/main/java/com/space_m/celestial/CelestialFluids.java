package com.space_m.celestial;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.space.SpaceEnvironment;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.trait.FluidTrait;
import com.space_m.SpaceMod;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * Fluid-Kennungen der Himmelskoerper-Traits. Das Original haelt {@code FluidType}-Objekte ({@code Fluids.EVEAIR} ...);
 * hier stehen Registry-IDs, weil die 62 Space-Fluide erst in R4 ueber {@code HbmFluidApi} angelegt werden
 * (Namensraum {@code space_m}, Pfad = Original-Feldname klein). Fluide, die HBM-Modernized schon hat, liegen unter
 * {@code hbm_m} ({@code EARTHAIR} des Space-Zweigs = {@code AIR} des Masters = {@code hbm_m:air}).
 *
 * <p>Alle Abfragen sind null-/registrierungssicher: ein (noch) nicht registriertes Fluid hat keine Traits und die
 * Farbe {@code 0xFFFFFF}.
 */
public final class CelestialFluids {
    private CelestialFluids() {}

    // ── HBM-Modernized ──
    public static final ResourceLocation NONE = hbm("none");
    public static final ResourceLocation WATER = SpaceEnvironment.HBM_WATER;
    /** Original {@code Fluids.EARTHAIR} (Master: {@code AIR}). */
    public static final ResourceLocation EARTHAIR = SpaceEnvironment.HBM_AIR;
    public static final ResourceLocation OXYGEN = hbm("oxygen");
    public static final ResourceLocation XENON = hbm("xenon");
    public static final ResourceLocation MERCURY = hbm("mercury");
    public static final ResourceLocation STEAM = hbm("steam");
    public static final ResourceLocation HOTSTEAM = hbm("hotsteam");
    public static final ResourceLocation SUPERHOTSTEAM = hbm("superhotsteam");
    public static final ResourceLocation ULTRAHOTSTEAM = hbm("ultrahotsteam");
    public static final ResourceLocation SPENTSTEAM = hbm("spentsteam");

    // ── Space (R4) ──
    public static final ResourceLocation EVEAIR = space("eveair");
    public static final ResourceLocation DUNAAIR = space("dunaair");
    public static final ResourceLocation TEKTOAIR = space("tektoair");
    public static final ResourceLocation NITROGEN = space("nitrogen");
    public static final ResourceLocation MILK = space("milk");
    public static final ResourceLocation BROMINE = space("bromine");
    public static final ResourceLocation CCL = space("ccl");
    public static final ResourceLocation JOOLGAS = space("joolgas");
    public static final ResourceLocation SARNUSGAS = space("sarnusgas");
    public static final ResourceLocation UGAS = space("ugas");
    public static final ResourceLocation NGAS = space("ngas");

    private static ResourceLocation hbm(String path) {
        return ResourceLocation.fromNamespaceAndPath("hbm_m", path);
    }

    private static ResourceLocation space(String path) {
        return SpaceMod.id(path);
    }

    /** Registriertes Fluid oder null. */
    @Nullable
    public static Fluid fluid(@Nullable ResourceLocation id) {
        if (id == null) return null;
        Fluid f = BuiltInRegistries.FLUID.get(id);
        return f == null || f == Fluids.EMPTY ? null : f;
    }

    /** Original {@code FluidType.hasTrait(Class)}. */
    public static boolean hasTrait(@Nullable ResourceLocation id, Class<? extends FluidTrait> trait) {
        Fluid f = fluid(id);
        return f != null && FluidType.hasTrait(f, trait);
    }

    /** Original {@code FluidType.getColor()} (Tint der HBM-Fluidtabelle). */
    public static int color(@Nullable ResourceLocation id) {
        if (id == null) return 0xFFFFFF;
        return ModFluids.getTintColor(id.getPath());
    }

    /** Lesen aus NBT/Netz: leere oder ungueltige Kennung -> {@link #NONE}. */
    public static ResourceLocation parse(@Nullable String s) {
        if (s == null || s.isEmpty()) return NONE;
        ResourceLocation id = ResourceLocation.tryParse(s);
        return id == null ? NONE : id;
    }
}
