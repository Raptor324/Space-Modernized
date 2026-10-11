package com.space_m.machine.atmo;

import java.util.List;

import com.space_m.SpaceMod;
import com.space_m.celestial.AtmosphereReaction;
import com.space_m.celestial.AtmosphereReaction.GasStack;
import com.space_m.celestial.CelestialFluids;

/**
 * 1:1 NTM Space {@code inventory/recipes/AtmosphereRecipes}: Atmosphaeren-Chemie, die {@code CelestialBody.updateChemistry}
 * (R1-A) bei jedem Chemie-Durchlauf mit Faktor 64 abarbeitet. Schluessel = Ausgabe, Wert = zwei Eingaben.
 *
 * <p>Das Original ist ein {@code SerializableRecipe} ({@code hbmAtmosphere.json}), liest aber keine Datei-Rezepte
 * ({@code readRecipe} leer) - daher hier nur die Standardrezepte. Sein NEI-Handler ({@code AtmosphereRecipeHandler},
 * "Atmospheric Chemistry") ist im Original nicht in {@code NEIRegistry} eingetragen und entfaellt deshalb auch hier.
 * Original-Klasse: AtmosphereRecipes
 */
public final class AtmosphereRecipes {
    private AtmosphereRecipes() {}

    public static final AtmosphereReaction LITHIUM_CARBONATE = new AtmosphereReaction(
            new GasStack(SpaceMod.id("lithcarbonate"), 32_000),
            List.of(
                    new GasStack(SpaceMod.id("lithydro"), 16_000),
                    new GasStack(CelestialFluids.DUNAAIR, 256_000)));

    /** Original {@code deleteRecipes()} + {@code registerDefaults()}. */
    public static void registerDefaults() {
        AtmosphereReaction.REACTIONS.remove(LITHIUM_CARBONATE);
        // processing actually does about 64x this, to speed up the reaction
        AtmosphereReaction.REACTIONS.add(LITHIUM_CARBONATE);
    }
}
