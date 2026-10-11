package com.space_m.celestial;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.resources.ResourceLocation;

/**
 * Eine Atmosphaeren-Chemiereaktion (Original {@code AtmosphereRecipes.getRecipesMap()}: Ausgabe-{@code FluidStack}
 * -> Eingaben). {@link CelestialBody#updateChemistry} arbeitet sie wie das Original ab; die Rezepte selbst
 * traegt die Atmosphaeren-Runde (R2, {@code AtmosphereRecipes}) in {@link #REACTIONS} ein.
 */
public record AtmosphereReaction(GasStack output, List<GasStack> inputs) {

    /** Original {@code FluidStack} (Typ + Menge in mB). */
    public record GasStack(ResourceLocation fluid, int fill) {}

    /** Alle Reaktionen in Rezept-Reihenfolge. */
    public static final List<AtmosphereReaction> REACTIONS = new CopyOnWriteArrayList<>();
}
