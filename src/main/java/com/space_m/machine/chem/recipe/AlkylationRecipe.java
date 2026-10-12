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
 * 1:1 NTM Space {@code AlkylationRecipes} ({@code hbmAlkylation.json}): Eingangsfluid (Tank 0, je Zyklus 100 mB) ->
 * Saeure/Katalysator {@code acid} (Tank 1, optional = {@code Fluids.NONE}), Produkte {@code output1} (Tank 2) und
 * {@code output2} (Tank 3). Gleiche JSON-Schluessel wie das Original ({@code input}, {@code acid}, {@code output1},
 * {@code output2}), Fluidstapel als {@code {"fluid": id, "amount": mB}}.
 * Original-Klasse: AlkylationRecipes
 */
public class AlkylationRecipe extends PlatformRecipe {

    private final Fluid input;
    private final FluidStack acid;
    private final FluidStack output1;
    private final FluidStack output2;

    public AlkylationRecipe(ResourceLocation id, Fluid input, FluidStack acid, FluidStack output1, FluidStack output2) {
        super(id);
        this.input = input;
        this.acid = acid;
        this.output1 = output1;
        this.output2 = output2;
    }

    public Fluid getInput() { return input; }
    /** Original {@code getX()}. */
    public FluidStack getAcid() { return acid; }
    /** Original {@code getY()}. */
    public FluidStack getOutput1() { return output1; }
    /** Original {@code getZ()}. */
    public FluidStack getOutput2() { return output2; }

    public boolean matches(Fluid fluid) {
        return fluid != null && fluid != Fluids.EMPTY && VanillaFluidEquivalence.sameSubstance(fluid, input);
    }

    /** Original {@code AlkylationRecipes.getOutput(type)}. */
    @Nullable
    public static AlkylationRecipe getOutput(@Nullable Level level, Fluid type) {
        for (AlkylationRecipe r : ChemRecipes.all(level, Type.INSTANCE)) {
            if (r.matches(type)) return r;
        }
        return null;
    }

    @Override public boolean matchesRecipe(@NotNull RecipeInputWrapper container, @NotNull Level level) { return false; }
    @Override public ItemStack assembleSafe() { return ItemStack.EMPTY; }
    @Override public ItemStack getResultItemSafe() { return ItemStack.EMPTY; }
    @Override public RecipeSerializer<?> getSerializer() { return Serializer.INSTANCE; }
    @Override public RecipeType<?> getType() { return Type.INSTANCE; }

    public static class Type implements RecipeType<AlkylationRecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "alkylation";
    }

    public static class Serializer extends PlatformRecipeSerializer<AlkylationRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        public AlkylationRecipe readJson(ResourceLocation id, JsonObject json) {
            return new AlkylationRecipe(id, ChemRecipes.readFluidId(json, "input"), ChemRecipes.readFluid(json, "acid"),
                    ChemRecipes.readFluid(json, "output1"), ChemRecipes.readFluid(json, "output2"));
        }

        @Override
        public AlkylationRecipe readNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            Fluid input = ChemRecipes.readFluidId(buf);
            return new AlkylationRecipe(id, input, RecipeHooks.readFluidStack(buf), RecipeHooks.readFluidStack(buf), RecipeHooks.readFluidStack(buf));
        }

        @Override
        public void writeNetwork(FriendlyByteBuf buf, AlkylationRecipe r) {
            ChemRecipes.writeFluidId(buf, r.input);
            RecipeHooks.writeFluidStack(buf, r.acid);
            RecipeHooks.writeFluidStack(buf, r.output1);
            RecipeHooks.writeFluidStack(buf, r.output2);
        }
    }
}
