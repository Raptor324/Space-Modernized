package com.space_m.material;

import com.hbm_m.inventory.material.MaterialShapes;
import com.hbm_m.inventory.material.Mats;
import com.hbm_m.inventory.material.Mats.MaterialStack;
import com.hbm_m.inventory.recipes.CrucibleRecipes;
import com.hbm_m.inventory.recipes.CrucibleRecipes.CrucibleRecipe;
import com.space_m.item.SpaceMaterialItems;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * R4-B: Tiegel-Legierungen von NTM Space ({@code CrucibleRecipes}, Space-Zuwachs) in HBM-Modernizeds
 * {@link CrucibleRecipes#recipeOrderedList}/{@code recipeNameMap} (oeffentlich, Java-Rezepte ohne JSON):
 * {@code crucible.hsss} (Edelstahl-Schnellarbeitsstahl), {@code crucible.arse} (Galliumarsenid),
 * {@code crucible.stainless} (Edelstahl). Werte 1:1.
 */
public final class SpaceCrucible {

    private SpaceCrucible() {}

    private static boolean done;

    public static synchronized void register() {
        if (done) return;
        done = true;
        CrucibleRecipes.init();

        int n = MaterialShapes.NUGGET.q(1);
        int i = MaterialShapes.INGOT.q(1);

        add(new CrucibleRecipe("crucible.hsss").setup(12, new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("hbm_m", "dura_steel_ingot"))))
                .inputs(new MaterialStack(SpaceMats.MAT_STAINLESS, n * 5), new MaterialStack(Mats.MAT_TUNGSTEN, n * 3), new MaterialStack(Mats.MAT_COBALT, n * 1))
                .outputs(new MaterialStack(Mats.MAT_DURA, i * 2)));

        add(new CrucibleRecipe("crucible.arse").setup(9, new ItemStack(SpaceMaterialItems.INGOT_GAAS.get()))
                .inputs(new MaterialStack(SpaceMats.MAT_GALLIUM, n * 6), new MaterialStack(Mats.MAT_ARSENIC, n * 3))
                .outputs(new MaterialStack(SpaceMats.MAT_GAAS, i)));

        add(new CrucibleRecipe("crucible.stainless").setup(2, new ItemStack(SpaceMaterialItems.INGOT_STAINLESS.get()))
                .inputs(new MaterialStack(Mats.MAT_STEEL, n), new MaterialStack(SpaceMats.MAT_NICKEL, n))
                .outputs(new MaterialStack(SpaceMats.MAT_STAINLESS, n * 2)));
    }

    private static void add(CrucibleRecipe r) {
        if (CrucibleRecipes.recipeNameMap.containsKey(r.name)) return;
        CrucibleRecipes.recipeOrderedList.add(r);
        CrucibleRecipes.recipeNameMap.put(r.name, r);
    }
}
