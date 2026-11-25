package com.space_m.machine.chem.jei;

import com.hbm_m.compat.jei.NeiUniversalJeiCategory;
import com.space_m.machine.chem.ChemMachines;
import com.space_m.machine.chem.recipe.VacuumCircuitRecipe;

import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** {@code VacuumCircuitHandler.drawExtras}: Dauer und Verbrauch rechtsbuendig bei x 160, y 43/55. */
final class VacuumCircuitCategory extends NeiUniversalJeiCategory {

    static final java.util.Map<Entry, VacuumCircuitRecipe> INFO = new java.util.IdentityHashMap<>();

    VacuumCircuitCategory(IGuiHelper gui) {
        super(gui, ChemJeiPlugin.VACUUM_CIRCUIT, Component.literal("Vacuum Solderer"), new ItemStack(ChemMachines.MACHINE_VACUUM_CIRCUIT.get()));
    }

    static Level clientLevel() {
        return Minecraft.getInstance().level;
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
