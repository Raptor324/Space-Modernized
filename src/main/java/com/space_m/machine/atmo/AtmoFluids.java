package com.space_m.machine.atmo;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.trait.FluidTraitSimple;
import com.space_m.celestial.CelestialFluids;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * Bruecke zwischen den Fluid-Kennungen der Himmelskoerper-Traits ({@link CelestialFluids}, {@code ResourceLocation})
 * und den HBM-Tanks ({@link Fluid}). Das Original hat beides als {@code FluidType}; hier sind Space-Fluide bis R4 evtl.
 * noch nicht registriert - ein unbekanntes Fluid wird wie {@code Fluids.NONE} behandelt (Maschine laeuft dann leer).
 */
public final class AtmoFluids {
    private AtmoFluids() {}

    /** HBM {@code none} (Original {@code Fluids.NONE}). */
    public static Fluid none() {
        return ModFluids.NONE.getSource();
    }

    /** Registriertes Fluid oder {@link #none()}. */
    public static Fluid fluid(@Nullable ResourceLocation id) {
        Fluid f = CelestialFluids.fluid(id);
        return f == null ? none() : f;
    }

    /** Registry-Kennung; leer/unbekannt -> {@link CelestialFluids#NONE}. */
    public static ResourceLocation id(@Nullable Fluid fluid) {
        if (fluid == null || fluid == Fluids.EMPTY) return CelestialFluids.NONE;
        ResourceLocation key = BuiltInRegistries.FLUID.getKey(fluid);
        return key == null ? CelestialFluids.NONE : key;
    }

    /** Original {@code type == Fluids.NONE}. */
    public static boolean isNone(@Nullable Fluid fluid) {
        return fluid == null || fluid == Fluids.EMPTY || fluid == none();
    }

    /** Original {@code type.hasTrait(FT_Gaseous.class) || type.hasTrait(FT_Gaseous_ART.class)}. */
    public static boolean isGas(@Nullable Fluid fluid) {
        if (isNone(fluid)) return false;
        return FluidType.hasTrait(fluid, FluidTraitSimple.FT_Gaseous.class) || FluidType.hasTrait(fluid, FluidTraitSimple.FT_Gaseous_ART.class);
    }

    /** Original {@code getTankType().getColor()}. */
    public static int color(@Nullable Fluid fluid) {
        return CelestialFluids.color(id(fluid));
    }
}
