package com.space_m.machine.atmo.jei;

import com.space_m.SpaceMod;
import com.space_m.block.SpaceBlocks;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * JEI-Plugin fuer die Atmo-Maschinen (Original {@code NEIRegistry}: {@code handlers.add(new AtmosphericCompressorHandler())}).
 * JEI ist nur Compile-Abhaengigkeit; ohne JEI wird die Klasse nie geladen.
 */
@JeiPlugin
public class AtmoJeiPlugin implements IModPlugin {

    private static final ResourceLocation UID = SpaceMod.id("atmo_machines");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new AtmosphereExtractionCategory(registration.getJeiHelpers().getGuiHelper(),
                new ItemStack(SpaceBlocks.MACHINE_ATMO_VENT.get())));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(AtmosphereExtractionCategory.TYPE, AtmosphereExtractionCategory.recipes());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(SpaceBlocks.MACHINE_ATMO_VENT.get()), AtmosphereExtractionCategory.TYPE);
    }
}
