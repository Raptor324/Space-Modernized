package com.space_m.machine.chem.recipe;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hbm_m.platform.recipe.PlatformRecipe;
import com.hbm_m.platform.recipe.PlatformRecipeSerializer;
import com.hbm_m.platform.recipe.RecipeHooks;
import com.hbm_m.platform.recipe.RecipeInputWrapper;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * 1:1 NTM Space {@code VacuumCircuitRecipes} ({@code hbmVacuumCircuit.json}): Halbleiter ({@code wafer}, Slots 0-1) +
 * Platinen ({@code pcb}, Slots 2-3) -> {@code output}, {@code duration} Ticks bei {@code consumption} HE/t. Eine Zutat
 * ist {@code {"ingredient": <Vanilla-Ingredient>, "count": n}} (Original {@code AStack} mit {@code stacksize}).
 * Original-Klasse: VacuumCircuitRecipes
 */
public class VacuumCircuitRecipe extends PlatformRecipe {

    /** Original {@code AStack}: Zutat + Mindestanzahl. */
    public record Stack(Ingredient ingredient, int count) {
        /** Original {@code matchesRecipe(stack, true)} (Anzahl getrennt geprueft). */
        public boolean matches(ItemStack stack) {
            return !stack.isEmpty() && ingredient.test(stack);
        }

        public List<ItemStack> display() {
            List<ItemStack> list = new ArrayList<>();
            for (ItemStack s : ingredient.getItems()) {
                ItemStack c = s.copy();
                c.setCount(count);
                list.add(c);
            }
            return list;
        }
    }

    public final List<Stack> wafer;
    public final List<Stack> pcb;
    public final ItemStack output;
    public final int duration;
    public final long consumption;

    public VacuumCircuitRecipe(ResourceLocation id, ItemStack output, int duration, long consumption, List<Stack> wafer, List<Stack> pcb) {
        super(id);
        this.output = output;
        this.duration = duration;
        this.consumption = consumption;
        this.wafer = wafer;
        this.pcb = pcb;
    }

    /** Original {@code VacuumCircuitRecipes.getRecipe(new ItemStack[] {slots[0..3]})}. */
    @Nullable
    public static VacuumCircuitRecipe getRecipe(@Nullable Level level, ItemStack[] inputs) {
        for (VacuumCircuitRecipe recipe : ChemRecipes.all(level, Type.INSTANCE)) {
            if (matchesIngredients(new ItemStack[] { inputs[0], inputs[1] }, recipe.wafer)
                    && matchesIngredients(new ItemStack[] { inputs[2], inputs[3] }, recipe.pcb)) return recipe;
        }
        return null;
    }

    /** Original {@code VacuumCircuitRecipes.wafer} (Menge aller Halbleiter-Zutaten). */
    public static boolean isWafer(@Nullable Level level, ItemStack stack) {
        for (VacuumCircuitRecipe r : ChemRecipes.all(level, Type.INSTANCE)) for (Stack t : r.wafer) if (t.matches(stack)) return true;
        return false;
    }

    /** Original {@code VacuumCircuitRecipes.pcb}. */
    public static boolean isPcb(@Nullable Level level, ItemStack stack) {
        for (VacuumCircuitRecipe r : ChemRecipes.all(level, Type.INSTANCE)) for (Stack t : r.pcb) if (t.matches(stack)) return true;
        return false;
    }

    /** 1:1 {@code SerializableRecipe.matchesIngredients}: jede belegte Eingabe deckt genau eine Zutat, alle Zutaten gedeckt. */
    public static boolean matchesIngredients(ItemStack[] inputs, List<Stack> recipe) {
        List<Stack> recipeList = new ArrayList<>(recipe);
        for (ItemStack inputStack : inputs) {
            if (inputStack != null && !inputStack.isEmpty()) {
                boolean hasMatch = false;
                Iterator<Stack> iterator = recipeList.iterator();
                while (iterator.hasNext()) {
                    Stack recipeStack = iterator.next();
                    if (recipeStack.matches(inputStack) && inputStack.getCount() >= recipeStack.count()) {
                        hasMatch = true;
                        iterator.remove();
                        break;
                    }
                }
                if (!hasMatch) return false;
            }
        }
        return recipeList.isEmpty();
    }

    @Override public boolean matchesRecipe(@NotNull RecipeInputWrapper container, @NotNull Level level) { return false; }
    @Override public ItemStack assembleSafe() { return output.copy(); }
    @Override public ItemStack getResultItemSafe() { return output; }
    @Override public RecipeSerializer<?> getSerializer() { return Serializer.INSTANCE; }
    @Override public RecipeType<?> getType() { return Type.INSTANCE; }

    public static class Type implements RecipeType<VacuumCircuitRecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "vacuum_circuit";
    }

    public static class Serializer extends PlatformRecipeSerializer<VacuumCircuitRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        private static List<Stack> readStacks(JsonArray array) {
            List<Stack> list = new ArrayList<>();
            for (JsonElement e : array) {
                JsonObject o = e.getAsJsonObject();
                list.add(new Stack(RecipeHooks.ingredientFromJson(o.get("ingredient")), GsonHelper.getAsInt(o, "count", 1)));
            }
            return list;
        }

        @Override
        public VacuumCircuitRecipe readJson(ResourceLocation id, JsonObject json) {
            return new VacuumCircuitRecipe(id, RecipeHooks.itemStackFromJson(GsonHelper.getAsJsonObject(json, "output")),
                    GsonHelper.getAsInt(json, "duration"), GsonHelper.getAsLong(json, "consumption"),
                    readStacks(GsonHelper.getAsJsonArray(json, "wafer")), readStacks(GsonHelper.getAsJsonArray(json, "pcb")));
        }

        private static List<Stack> readStacks(FriendlyByteBuf buf) {
            int n = buf.readVarInt();
            List<Stack> list = new ArrayList<>(n);
            for (int i = 0; i < n; i++) list.add(new Stack(RecipeHooks.readIngredient(buf), buf.readVarInt()));
            return list;
        }

        private static void writeStacks(FriendlyByteBuf buf, List<Stack> list) {
            buf.writeVarInt(list.size());
            for (Stack s : list) {
                RecipeHooks.writeIngredient(buf, s.ingredient());
                buf.writeVarInt(s.count());
            }
        }

        @Override
        public VacuumCircuitRecipe readNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            ItemStack output = RecipeHooks.readItem(buf);
            int duration = buf.readVarInt();
            long consumption = buf.readVarLong();
            List<Stack> wafer = readStacks(buf);
            List<Stack> pcb = readStacks(buf);
            return new VacuumCircuitRecipe(id, output, duration, consumption, wafer, pcb);
        }

        @Override
        public void writeNetwork(FriendlyByteBuf buf, VacuumCircuitRecipe r) {
            RecipeHooks.writeItem(buf, r.output);
            buf.writeVarInt(r.duration);
            buf.writeVarLong(r.consumption);
            writeStacks(buf, r.wafer);
            writeStacks(buf, r.pcb);
        }
    }
}
