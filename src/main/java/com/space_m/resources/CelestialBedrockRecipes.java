package com.space_m.resources;

import static com.space_m.resources.ItemCelestialBedrockOre.extract;
import static com.space_m.resources.ItemCelestialBedrockOre.make;
import static com.space_m.resources.ItemCelestialBedrockOre.toFluid;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.inventory.material.MaterialShapes;
import com.hbm_m.inventory.material.Mats;
import com.hbm_m.inventory.material.Mats.MaterialStack;
import com.hbm_m.inventory.recipes.ArcFurnaceRecipes;
import com.hbm_m.inventory.recipes.ArcFurnaceRecipes.ArcFurnaceRecipe;
import com.hbm_m.inventory.recipes.ElectrolyserMetalRecipes;
import com.hbm_m.inventory.recipes.ElectrolyserMetalRecipes.ElectrolysisMetalRecipe;
import com.space_m.resources.ItemCelestialBedrockOre.BedrockOreGrade;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Die Space-Bedrock-Erzrezepte, die HBM-Modernized als Code-Tabellen fuehrt (keine JSON-Rezepttypen):
 * 1:1 NTM Space {@code ArcFurnaceRecipes} (11 je Sorte) und {@code ElectrolyserMetalRecipes} (2 je Sorte,
 * {@code makeBedrockOreProduct}). Zentrifuge, Kristallisator, Kombinations-/Pyro-Ofen und Schredder liegen als
 * Datapack-JSON unter {@code data/space_m/recipes/bedrock_ore/} (erzeugt von {@code scratchpad/sBO_gen.py}).
 */
public final class CelestialBedrockRecipes {

    private CelestialBedrockRecipes() {}

    private static boolean done;

    /** Common-Setup (Items registriert, Fragment-Formen angemeldet). */
    public static synchronized void register() {
        if (done) return;
        done = true;

        for (CelestialBedrockOre.Type type : CelestialBedrockOre.getAllTypes()) {
            // ArcFurnaceRecipes
            arc(BedrockOreGrade.SULFURIC_BYPRODUCT, type, new ArcFurnaceRecipe().solid(make(BedrockOreGrade.SULFURIC_ARC, type, 2)));
            arc(BedrockOreGrade.SULFURIC_ROASTED, type, new ArcFurnaceRecipe().solid(make(BedrockOreGrade.SULFURIC_ARC, type, 4)));
            arc(BedrockOreGrade.SOLVENT_BYPRODUCT, type, new ArcFurnaceRecipe().solid(make(BedrockOreGrade.SOLVENT_ARC, type, 2)));
            arc(BedrockOreGrade.SOLVENT_ROASTED, type, new ArcFurnaceRecipe().solid(make(BedrockOreGrade.SOLVENT_ARC, type, 4)));
            arc(BedrockOreGrade.RAD_BYPRODUCT, type, new ArcFurnaceRecipe().solid(make(BedrockOreGrade.RAD_ARC, type, 2)));
            arc(BedrockOreGrade.RAD_ROASTED, type, new ArcFurnaceRecipe().solid(make(BedrockOreGrade.RAD_ARC, type, 4)));

            arc(BedrockOreGrade.PRIMARY_FIRST, type, new ArcFurnaceRecipe().fluidNull(toFluid(type.primary, 5)));
            arc(BedrockOreGrade.CRUMBS, type, new ArcFurnaceRecipe().fluidNull(toFluid(type.primary, 1)));

            arc(BedrockOreGrade.SULFURIC_WASHED, type, new ArcFurnaceRecipe().fluidNull(toFluid(type.byproductAcid, 3)));
            arc(BedrockOreGrade.SOLVENT_WASHED, type, new ArcFurnaceRecipe().fluidNull(toFluid(type.byproductSolvent, 3)));
            arc(BedrockOreGrade.RAD_WASHED, type, new ArcFurnaceRecipe().fluidNull(toFluid(type.byproductRad, 3)));

            // ElectrolyserMetalRecipes
            List<Object[]> productsF = new ArrayList<>();
            productsF.add(new Object[] { type.primary, 8 });
            productsF.add(new Object[] { make(BedrockOreGrade.CRUMBS, type), 1 });
            ElectrolyserMetalRecipes.recipes.put(CelestialBedrockOreItems.ore(BedrockOreGrade.PRIMARY_FIRST, type), makeBedrockOreProduct(productsF));

            List<Object[]> productsC = new ArrayList<>();
            productsC.add(new Object[] { type.primary, 2 });
            ElectrolyserMetalRecipes.recipes.put(CelestialBedrockOreItems.ore(BedrockOreGrade.CRUMBS, type), makeBedrockOreProduct(productsC));
        }
    }

    private static void arc(BedrockOreGrade grade, CelestialBedrockOre.Type type, ArcFurnaceRecipe recipe) {
        Item in = CelestialBedrockOreItems.ore(grade, type);
        ArcFurnaceRecipes.register(new ArcFurnaceRecipes.Entry(s -> s.is(in), List.of(new ItemStack(in)), null), recipe);
    }

    /** 1:1 Space {@code ElectrolyserMetalRecipes.makeBedrockOreProduct}. */
    public static ElectrolysisMetalRecipe makeBedrockOreProduct(List<Object[]> products) {
        List<MaterialStack> moltenProducts = new ArrayList<>();
        List<ItemStack> solidProducts = new ArrayList<>();

        for (Object[] product : products) {
            Object key = product[0];
            int value = (Integer) product[1];
            if (moltenProducts.size() < 2 && key instanceof CelestialBedrockOre.Output out) {
                MaterialStack melt = toFluid(out, value);
                if (melt != null) {
                    moltenProducts.add(melt);
                    continue;
                }
            }

            if (key instanceof CelestialBedrockOre.Output out) solidProducts.add(extract(out, value));
            if (key instanceof ItemStack stack) solidProducts.add(stack.copy());
        }
        if (moltenProducts.isEmpty()) moltenProducts.add(new MaterialStack(Mats.MAT_SLAG, MaterialShapes.INGOT.q(2)));

        return new ElectrolysisMetalRecipe(
                moltenProducts.get(0),
                moltenProducts.size() > 1 ? moltenProducts.get(1) : null,
                20,
                solidProducts.toArray(new ItemStack[0]));
    }
}
