package com.space_m.machine.chem.recipe;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonObject;
import com.hbm_m.api.fluids.VanillaFluidEquivalence;
import com.hbm_m.platform.recipe.PlatformRecipe;
import com.hbm_m.platform.recipe.PlatformRecipeSerializer;
import com.hbm_m.platform.recipe.RecipeHooks;
import com.hbm_m.platform.recipe.RecipeInputWrapper;

import dev.architectury.fluid.FluidStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * 1:1 NTM Space {@code CryoRecipes} ({@code hbmCryodistillator.json}): Luft/Gas (Tank 0, je Zyklus 100 mB) -> vier
 * Fraktionen {@code output1..4} (Original {@code Quartet W/X/Y/Z} -> Tanks 1..4).
 * Original-Klasse: CryoRecipes
 */
public class CryoRecipe extends PlatformRecipe {

    private final Fluid input;
    private final FluidStack[] outputs;

    public CryoRecipe(ResourceLocation id, Fluid input, FluidStack o1, FluidStack o2, FluidStack o3, FluidStack o4) {
        super(id);
        this.input = input;
        this.outputs = new FluidStack[] { o1, o2, o3, o4 };
    }

    public Fluid getInput() { return input; }

    /** Ausgabe 0..3 (Original {@code getW/X/Y/Z}). */
    public FluidStack getOutput(int i) { return outputs[i]; }

    public boolean matches(Fluid fluid) {
        return fluid != null && fluid != Fluids.EMPTY && VanillaFluidEquivalence.sameSubstance(fluid, input);
    }

    /** Original {@code CryoRecipes.getOutput(type)}. */
    @Nullable
    public static CryoRecipe getOutput(@Nullable Level level, Fluid type) {
        for (CryoRecipe r : ChemRecipes.all(level, Type.INSTANCE)) {
            if (r.matches(type)) return r;
        }
        return null;
    }

    @Override public boolean matchesRecipe(@NotNull RecipeInputWrapper container, @NotNull Level level) { return false; }
    @Override public ItemStack assembleSafe() { return ItemStack.EMPTY; }
    @Override public ItemStack getResultItemSafe() { return ItemStack.EMPTY; }
    @Override public RecipeSerializer<?> getSerializer() { return Serializer.INSTANCE; }
    @Override public RecipeType<?> getType() { return Type.INSTANCE; }

    public static class Type implements RecipeType<CryoRecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "cryo_distill";
    }

    public static class Serializer extends PlatformRecipeSerializer<CryoRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        public CryoRecipe readJson(ResourceLocation id, JsonObject json) {
            return new CryoRecipe(id, ChemRecipes.readFluidId(json, "input"),
                    ChemRecipes.readFluid(json, "output1"), ChemRecipes.readFluid(json, "output2"),
                    ChemRecipes.readFluid(json, "output3"), ChemRecipes.readFluid(json, "output4"));
        }

        @Override
        public CryoRecipe readNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            Fluid input = ChemRecipes.readFluidId(buf);
            return new CryoRecipe(id, input, RecipeHooks.readFluidStack(buf), RecipeHooks.readFluidStack(buf),
                    RecipeHooks.readFluidStack(buf), RecipeHooks.readFluidStack(buf));
        }

        @Override
        public void writeNetwork(FriendlyByteBuf buf, CryoRecipe r) {
            ChemRecipes.writeFluidId(buf, r.input);
            for (FluidStack out : r.outputs) RecipeHooks.writeFluidStack(buf, out);
        }
    }
}
