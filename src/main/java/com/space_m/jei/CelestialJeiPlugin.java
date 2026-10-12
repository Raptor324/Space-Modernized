package com.space_m.jei;

import com.space_m.SpaceMod;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * R9-B: JEI-Plugin fuer die Koerper-Uebersichten (Original {@code NEIRegistry}: {@code BedrockDrillHandler},
 * {@code WaterTableHandler}, {@code OilExtractionHandler}). JEI ist nur Compile-Abhaengigkeit; ohne JEI nie geladen.
 */
@JeiPlugin
public class CelestialJeiPlugin implements IModPlugin {

    private static final ResourceLocation UID = SpaceMod.id("celestial_info");

    private CelestialInfoCategory water, oil, bedrock;

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    private static ItemStack[] stacks(String... ids) {
        return java.util.Arrays.stream(ids).map(CelestialInfoCategory::stack).filter(s -> !s.isEmpty()).toArray(ItemStack[]::new);
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper gui = registration.getJeiHelpers().getGuiHelper();
        water = new CelestialInfoCategory(gui, CelestialInfoCategory.WATER_TABLE, "Fluid Table", false,
                stacks("hbm_m:pump_electric", "hbm_m:pump_steam"));
        oil = new CelestialInfoCategory(gui, CelestialInfoCategory.OIL_EXTRACTION, "Oil Extraction", true,
                stacks("hbm_m:derrick", "hbm_m:pumpjack", "hbm_m:hydraulic_frackining_tower"));
        bedrock = new CelestialInfoCategory(gui, CelestialInfoCategory.BEDROCK, "Bedrock Drilling", false,
                stacks("hbm_m:mining_drill"));
        registration.addRecipeCategories(bedrock, water, oil);
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(CelestialInfoCategory.BEDROCK, bedrock.bedrock());
        registration.addRecipes(CelestialInfoCategory.WATER_TABLE, water.waterTable());
        registration.addRecipes(CelestialInfoCategory.OIL_EXTRACTION, oil.oilExtraction());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (ItemStack s : bedrock.getMachines()) registration.addRecipeCatalyst(s, CelestialInfoCategory.BEDROCK);
        for (ItemStack s : water.getMachines()) registration.addRecipeCatalyst(s, CelestialInfoCategory.WATER_TABLE);
        for (ItemStack s : oil.getMachines()) registration.addRecipeCatalyst(s, CelestialInfoCategory.OIL_EXTRACTION);
    }
}
