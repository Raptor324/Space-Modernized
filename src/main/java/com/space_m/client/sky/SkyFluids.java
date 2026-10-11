package com.space_m.client.sky;

import com.space_m.SpaceMod;

import net.minecraft.resources.ResourceLocation;

/**
 * Weitere Fluid-Kennungen, die nur der Himmel vergleicht (Ergaenzung zu {@code com.space_m.celestial.CelestialFluids}):
 * Original {@code Fluids.CARBONDIOXIDE/CHLORINE/RADIOSOLVENT} (in HBM-Modernized vorhanden) und {@code CHLOROMETHANE}
 * (Space-Fluid, R4).
 */
public final class SkyFluids {
    private SkyFluids() {}

    public static final ResourceLocation CARBONDIOXIDE = ResourceLocation.fromNamespaceAndPath("hbm_m", "carbondioxide");
    public static final ResourceLocation CHLORINE = ResourceLocation.fromNamespaceAndPath("hbm_m", "chlorine");
    public static final ResourceLocation RADIOSOLVENT = ResourceLocation.fromNamespaceAndPath("hbm_m", "radiosolvent");
    public static final ResourceLocation CHLOROMETHANE = SpaceMod.id("chloromethane");
}
