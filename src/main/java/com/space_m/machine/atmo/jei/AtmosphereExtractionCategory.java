package com.space_m.machine.atmo.jei;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com.hbm_m.compat.jei.NeiUniversalJeiCategory;
import com.space_m.SpaceMod;
import com.space_m.celestial.CelestialBody;
import com.space_m.celestial.SolarSystem;
import com.space_m.celestial.trait.CBT_Atmosphere;
import com.space_m.machine.atmo.AtmoFluids;

import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

/**
 * 1:1 NTM Space {@code AtmosphericCompressorHandler} ("Atmosphere Extraction", Schluessel {@code ntmAtmoCompressor},
 * Maschine {@code machine_atmo_vent}) auf Basis {@code NEICelestialHandler}: je Koerper mit Standard-Atmosphaere
 * ({@code getDefaultTrait(CBT_Atmosphere)}) ein Rezept ohne Eingaben, Ausgaben = alle Gase mit ihrem Partialdruck
 * ({@code ItemFluidIcon.make(fluid, pressure)} -> Tooltipzeile "x atm"). Links dreht sich die Planetentextur
 * (32x32 bei 32|12, UV-Verschiebung 4 s), darunter der Koerpername ({@code body.<name>}).
 *
 * <p>Abweichung (technisch noetig): Gase, die noch nicht als Fluid registriert sind (Space-Fluide, R4), fehlen in der
 * Ausgabe; Koerper ohne ein einziges registriertes Gas erscheinen erst mit R4.
 * Original-Klasse: AtmosphericCompressorHandler
 * Original-Klasse: NEICelestialHandler (Planetenbild + Name, Rezepte ohne Eingaben)
 */
public class AtmosphereExtractionCategory extends NeiUniversalJeiCategory {

    public static final RecipeType<Entry> TYPE = RecipeType.create(SpaceMod.MOD_ID, "ntm_atmo_compressor", Entry.class);

    private static final Map<Entry, CelestialBody> BODIES = new IdentityHashMap<>();

    public AtmosphereExtractionCategory(IGuiHelper gui, ItemStack machine) {
        super(gui, TYPE, Component.literal("Atmosphere Extraction"), machine);
    }

    /** Original {@code AtmosphericCompressorHandler.getRecipes()} in Reihenfolge {@code SolarSystem.Body.values()}. */
    public static List<Entry> recipes() {
        BODIES.clear();
        List<Entry> list = new ArrayList<>();
        for (SolarSystem.Body bodyEnum : SolarSystem.Body.values()) {
            CelestialBody body = bodyEnum.getBody();
            if (body == null) continue;

            CBT_Atmosphere atmosphere = body.getDefaultTrait(CBT_Atmosphere.class);
            if (atmosphere == null) continue;

            List<Slot> outputs = new ArrayList<>();
            for (CBT_Atmosphere.FluidEntry entry : atmosphere.fluids) {
                Fluid fluid = AtmoFluids.fluid(entry.fluid);
                if (AtmoFluids.isNone(fluid)) continue;
                double pressure = Math.round(entry.pressure * 1000.0D) / 1000.0D; // BobMathUtil.roundDecimal(atm, 3)
                outputs.add(Slot.fluid(fluid, 1).tip(Component.literal(pressure + "atm")));
            }
            if (outputs.isEmpty()) continue;

            Entry recipe = new Entry(List.of(), outputs);
            BODIES.put(recipe, body);
            list.add(recipe);
        }
        return list;
    }

    @Override
    public void draw(Entry recipe, IRecipeSlotsView slots, GuiGraphics g, double mouseX, double mouseY) {
        super.draw(recipe, slots, g, mouseX, mouseY);

        CelestialBody body = BODIES.get(recipe);
        if (body == null) return;

        if (body.texture != null) {
            // Original: UV-Verschiebung (Clock.get_ms() % 4000) / 4000 ueber 32x32 bei (32, 12)
            float uvOffset = (float) (System.currentTimeMillis() % 4000L) / 4000F;
            int split = Math.round((1F - uvOffset) * 32F);
            if (split > 0) g.blit(body.texture, 32, 12, split, 32, uvOffset * 32F, 0F, split, 32, 32, 32);
            if (split < 32) g.blit(body.texture, 32 + split, 12, 32 - split, 32, 0F, 0F, 32 - split, 32, 32, 32);
        }

        // drawStringC("body." + name, 16, 48, 64, 12, 0x000000, false)
        Font font = Minecraft.getInstance().font;
        Component name = Component.translatable("body." + body.name);
        g.drawString(font, name, 16 + 32 - font.width(name) / 2, 48 + 6 - font.lineHeight / 2, 0x000000, false);
    }
}
