package com.space_m.machine.chem.jei;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.compat.jei.NeiUniversalJeiCategory;
import com.hbm_m.compat.jei.NeiUniversalJeiCategory.Entry;
import com.hbm_m.compat.jei.NeiUniversalJeiCategory.Slot;
import com.space_m.SpaceMod;
import com.space_m.machine.chem.ChemMachines;
import com.space_m.machine.chem.ChemUtil;
import com.space_m.machine.chem.recipe.AlkylationRecipe;
import com.space_m.machine.chem.recipe.ChemRecipes;
import com.space_m.machine.chem.recipe.CryoRecipe;
import com.space_m.machine.chem.recipe.VacuumCircuitRecipe;

import dev.architectury.fluid.FluidStack;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * JEI fuer die Chemie-Maschinen (Original {@code NEIRegistry}: {@code AlkylationHandler} "Alkylation"
 * ({@code ntmAlkylation}), {@code CryoHandler} "Cryogenic Distillation" ({@code ntmCryodistill}),
 * {@code VacuumCircuitHandler} "Vacuum Solderer" ({@code ntmVacuumCircuit}, mit Dauer/Verbrauch rechts) und
 * {@code DairyHandler} "Dairy" ({@code ntmDairy}), alle {@code NEIUniversalHandler}). Fluide wie {@code ItemFluidIcon}:
 * Eingang 1 000 mB, Alkylierungs-/Kryo-Ausgaben {@code fill * 10}.
 * Original-Klasse: AlkylationHandler
 * Original-Klasse: CryoHandler
 * Original-Klasse: VacuumCircuitHandler
 * Original-Klasse: DairyHandler
 */
@JeiPlugin
public class ChemJeiPlugin implements IModPlugin {

    private static final ResourceLocation UID = SpaceMod.id("chem_machines");

    public static final RecipeType<Entry> ALKYLATION = RecipeType.create(SpaceMod.MOD_ID, "ntm_alkylation", Entry.class);
    public static final RecipeType<Entry> CRYO = RecipeType.create(SpaceMod.MOD_ID, "ntm_cryodistill", Entry.class);
    public static final RecipeType<Entry> VACUUM_CIRCUIT = RecipeType.create(SpaceMod.MOD_ID, "ntm_vacuum_circuit", Entry.class);
    public static final RecipeType<Entry> DAIRY = RecipeType.create(SpaceMod.MOD_ID, "ntm_dairy", Entry.class);

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper gui = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new NeiUniversalJeiCategory(gui, ALKYLATION, Component.literal("Alkylation"), new ItemStack(ChemMachines.MACHINE_ALKYLATION.get())),
                new NeiUniversalJeiCategory(gui, CRYO, Component.literal("Cryogenic Distillation"), new ItemStack(ChemMachines.MACHINE_CRYO_DISTILL.get())),
                new VacuumCircuitCategory(gui),
                new NeiUniversalJeiCategory(gui, DAIRY, Component.literal("Dairy"), new ItemStack(ChemMachines.MACHINE_MILK_REFORMER.get())));
    }

    private static Slot fluid(FluidStack stack, int factor) {
        return Slot.fluid(ChemRecipes.type(stack), ChemRecipes.fill(stack) * factor);
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        Level level = Minecraft.getInstance().level;

        // AlkylationRecipes.getRecipes(): ohne Saeure nur der Eingang
        List<Entry> alky = new ArrayList<>();
        for (AlkylationRecipe r : ChemRecipes.all(level, AlkylationRecipe.Type.INSTANCE)) {
            List<Slot> in = new ArrayList<>();
            in.add(Slot.fluid(r.getInput(), 1000));
            if (!r.getAcid().isEmpty() && !ChemUtil.isNone(r.getAcid().getFluid())) in.add(fluid(r.getAcid(), 10));
            alky.add(new Entry(in, List.of(fluid(r.getOutput1(), 10), fluid(r.getOutput2(), 10))));
        }
        registration.addRecipes(ALKYLATION, alky);

        // CryoRecipes.getCryoRecipes()
        List<Entry> cryo = new ArrayList<>();
        for (CryoRecipe r : ChemRecipes.all(level, CryoRecipe.Type.INSTANCE)) {
            List<Slot> out = new ArrayList<>();
            for (int i = 0; i < 4; i++) out.add(fluid(r.getOutput(i), 10));
            cryo.add(new Entry(List.of(Slot.fluid(r.getInput(), 1000)), out));
        }
        registration.addRecipes(CRYO, cryo);

        // VacuumCircuitRecipes.getRecipes(): Halbleiter + Platinen -> Ausgabe
        List<Entry> vac = new ArrayList<>();
        for (VacuumCircuitRecipe r : ChemRecipes.all(level, VacuumCircuitRecipe.Type.INSTANCE)) {
            List<Slot> in = new ArrayList<>();
            for (VacuumCircuitRecipe.Stack s : r.wafer) in.add(Slot.of(s.display()));
            for (VacuumCircuitRecipe.Stack s : r.pcb) in.add(Slot.of(s.display()));
            Entry e = new Entry(in, List.of(Slot.of(r.output.copy())));
            VacuumCircuitCategory.INFO.put(e, r);
            vac.add(e);
        }
        registration.addRecipes(VACUUM_CIRCUIT, vac);

        // DairyHandler: 100 mB Milch -> 50 EMILK, 35 CMILK, 15 CREAM
        registration.addRecipes(DAIRY, List.of(new Entry(
                List.of(Slot.fluid(ChemUtil.fluid("MILK"), 100)),
                List.of(Slot.fluid(ChemUtil.fluid("EMILK"), 50), Slot.fluid(ChemUtil.fluid("CMILK"), 35), Slot.fluid(ChemUtil.fluid("CREAM"), 15)))));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ChemMachines.MACHINE_ALKYLATION.get()), ALKYLATION);
        registration.addRecipeCatalyst(new ItemStack(ChemMachines.MACHINE_CRYO_DISTILL.get()), CRYO);
        registration.addRecipeCatalyst(new ItemStack(ChemMachines.MACHINE_VACUUM_CIRCUIT.get()), VACUUM_CIRCUIT);
        registration.addRecipeCatalyst(new ItemStack(ChemMachines.MACHINE_MILK_REFORMER.get()), DAIRY);
    }

    /** {@code VacuumCircuitHandler.drawExtras}: Dauer und Verbrauch rechtsbuendig bei x 160, y 43/55. */
    static class VacuumCircuitCategory extends NeiUniversalJeiCategory {

        static final java.util.Map<Entry, VacuumCircuitRecipe> INFO = new java.util.IdentityHashMap<>();

        VacuumCircuitCategory(IGuiHelper gui) {
            super(gui, VACUUM_CIRCUIT, Component.literal("Vacuum Solderer"), new ItemStack(ChemMachines.MACHINE_VACUUM_CIRCUIT.get()));
        }

        @Override
        public void draw(Entry recipe, IRecipeSlotsView slots, GuiGraphics g, double mouseX, double mouseY) {
            super.draw(recipe, slots, g, mouseX, mouseY);
            VacuumCircuitRecipe sol = INFO.get(recipe);
            if (sol == null) return;
            Font font = Minecraft.getInstance().font;
            String duration = String.format(java.util.Locale.US, "%,d", sol.duration) + " ticks";
            String consumption = String.format(java.util.Locale.US, "%,d", sol.consumption) + " HE/t";
            int side = 160;
            g.drawString(font, duration, side - font.width(duration), 43, 0x404040, false);
            g.drawString(font, consumption, side - font.width(consumption), 55, 0x404040, false);
        }
    }
}
