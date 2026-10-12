package com.space_m.machine.chem.recipe;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonObject;
import com.hbm_m.platform.recipe.RecipeHooks;
import com.space_m.SpaceMod;
import com.space_m.machine.chem.ChemUtil;

import dev.architectury.fluid.FluidStack;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * Neue Rezepttypen von NTM Space (Original {@code SerializableRecipe}-Unterklassen mit Datei-Ueberschreibung
 * {@code config/hbmRecipes/hbm*.json}): {@code space_m:alkylation} ({@code AlkylationRecipes}), {@code space_m:cryo_distill}
 * ({@code CryoRecipes}), {@code space_m:vacuum_circuit} ({@code VacuumCircuitRecipes}). Wie in HBM-Modernized
 * (Hydrotreater, Reformer ...) datengetrieben: die Original-{@code registerDefaults()} liegen als JSON unter
 * {@code data/space_m/recipes/chem/...} und sind per Datenpaket aenderbar (ersetzt die Konfig-Dateien des Originals).
 */
public final class ChemRecipes {

    private ChemRecipes() {}

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(SpaceMod.MOD_ID, Registries.RECIPE_SERIALIZER);
    public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(SpaceMod.MOD_ID, Registries.RECIPE_TYPE);

    public static final RegistrySupplier<RecipeSerializer<AlkylationRecipe>> ALKYLATION_SERIALIZER =
            SERIALIZERS.register(AlkylationRecipe.Type.ID, () -> AlkylationRecipe.Serializer.INSTANCE);
    public static final RegistrySupplier<RecipeType<AlkylationRecipe>> ALKYLATION_TYPE =
            TYPES.register(AlkylationRecipe.Type.ID, () -> AlkylationRecipe.Type.INSTANCE);

    public static final RegistrySupplier<RecipeSerializer<CryoRecipe>> CRYO_SERIALIZER =
            SERIALIZERS.register(CryoRecipe.Type.ID, () -> CryoRecipe.Serializer.INSTANCE);
    public static final RegistrySupplier<RecipeType<CryoRecipe>> CRYO_TYPE =
            TYPES.register(CryoRecipe.Type.ID, () -> CryoRecipe.Type.INSTANCE);

    public static final RegistrySupplier<RecipeSerializer<VacuumCircuitRecipe>> VACUUM_CIRCUIT_SERIALIZER =
            SERIALIZERS.register(VacuumCircuitRecipe.Type.ID, () -> VacuumCircuitRecipe.Serializer.INSTANCE);
    public static final RegistrySupplier<RecipeType<VacuumCircuitRecipe>> VACUUM_CIRCUIT_TYPE =
            TYPES.register(VacuumCircuitRecipe.Type.ID, () -> VacuumCircuitRecipe.Type.INSTANCE);

    /** Aus {@code ChemMachines.init()} (Mod-Konstruktor). */
    public static void register() {
        SERIALIZERS.register();
        TYPES.register();
    }

    /** Alle Rezepte eines Typs (Server: Level; Client/JEI: {@code Minecraft.level}). */
    public static <R extends net.minecraft.world.item.crafting.Recipe<?>> List<R> all(@Nullable Level level, RecipeType<R> type) {
        if (level == null) return List.of();
        return RecipeHooks.getAllRecipes(level, type);
    }

    // ── Fluid-Stapel ({@code FluidStack} des Originals) ──────────────────────────────────────

    /** {@code {"fluid": "<id>", "amount": <mB>}}; fehlt der Schluessel -> leer (Original {@code Fluids.NONE, 0}). */
    static FluidStack readFluid(JsonObject json, String key) {
        if (!json.has(key)) return FluidStack.empty();
        JsonObject obj = GsonHelper.getAsJsonObject(json, key);
        ResourceLocation id = ResourceLocation.tryParse(GsonHelper.getAsString(obj, "fluid"));
        long amount = GsonHelper.getAsLong(obj, "amount", 0L);
        return RecipeHooks.fluidStackOf(id, amount);
    }

    /** Eingangsfluid {@code "input": "<id>"} (Schluessel der Original-HashMap). */
    static Fluid readFluidId(JsonObject json, String key) {
        ResourceLocation id = ResourceLocation.tryParse(GsonHelper.getAsString(json, key));
        if (id == null) return Fluids.EMPTY;
        Fluid f = net.minecraft.core.registries.BuiltInRegistries.FLUID.get(id);
        return f == null ? Fluids.EMPTY : f;
    }

    static void writeFluidId(FriendlyByteBuf buf, Fluid fluid) {
        ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(fluid);
        buf.writeResourceLocation(id != null ? id : ResourceLocation.fromNamespaceAndPath("minecraft", "empty"));
    }

    static Fluid readFluidId(FriendlyByteBuf buf) {
        Fluid f = net.minecraft.core.registries.BuiltInRegistries.FLUID.get(buf.readResourceLocation());
        return f == null ? Fluids.EMPTY : f;
    }

    /** Fluid des Stapels; leer -> HBM {@code none} (Original {@code Fluids.NONE}). */
    public static Fluid type(FluidStack stack) {
        return stack == null || stack.isEmpty() ? ChemUtil.none() : stack.getFluid();
    }

    /** mB des Stapels (leer = 0). */
    public static int fill(FluidStack stack) {
        return stack == null || stack.isEmpty() ? 0 : (int) Math.min(Integer.MAX_VALUE, stack.getAmount());
    }
}
