package com.space_m.jei;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.compat.jei.NeiUniversalJeiCategory;
import com.space_m.SpaceMod;
import com.space_m.celestial.CelestialBody;
import com.space_m.celestial.SolarSystem;
import com.space_m.celestial.trait.CBT_Water;
import com.space_m.machine.atmo.AtmoFluids;

import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;

/**
 * R9-B: die drei Koerper-Uebersichten aus NTM Space {@code NEIRegistry} auf Basis {@code NEICelestialHandler} (je Koerper
 * ein Rezept ohne Eingaben, links das drehende Planetenbild + Name, wie {@code AtmosphereExtractionCategory}):
 * <ul>
 *   <li>{@code WaterTableHandler} "Fluid Table" ({@code ntmFluidTable}): {@code CBT_Water} je Koerper, Wasser 10 000 mB,
 *       sonst 1 000 mB; Maschinen {@code pump_electric}, {@code pump_steam}.</li>
 *   <li>{@code OilExtractionHandler} "Oil Extraction" ({@code ntmOilExtraction}): fest verdrahtete Lagerstaetten je Koerper,
 *       Kerbin/Tekto zusaetzlich "Frackable"; Maschinen Bohrturm, Pumpenbock, Fracking-Turm.</li>
 *   <li>{@code BedrockDrillHandler} "Bedrock Drilling" ({@code ntmBedrockCelestial}): Bedrock-Erz des Koerpers
 *       ({@code bedrock_ore_base}, Koerper im NBT) und bei Bedrock-Eis 32 Packeis; Maschine Bagger.</li>
 * </ul>
 */
// Original-Klasse: WaterTableHandler
// Original-Klasse: OilExtractionHandler
// Original-Klasse: BedrockDrillHandler
public class CelestialInfoCategory extends NeiUniversalJeiCategory {

    public static final RecipeType<Entry> WATER_TABLE = RecipeType.create(SpaceMod.MOD_ID, "ntm_fluid_table", Entry.class);
    public static final RecipeType<Entry> OIL_EXTRACTION = RecipeType.create(SpaceMod.MOD_ID, "ntm_oil_extraction", Entry.class);
    public static final RecipeType<Entry> BEDROCK = RecipeType.create(SpaceMod.MOD_ID, "ntm_bedrock_celestial", Entry.class);

    private final Map<Entry, CelestialBody> bodies = new IdentityHashMap<>();
    private final boolean frackableNote;

    public CelestialInfoCategory(IGuiHelper gui, RecipeType<Entry> type, String title, boolean frackableNote, ItemStack... machines) {
        super(gui, type, Component.literal(title), machines);
        this.frackableNote = frackableNote;
    }

    static ItemStack stack(String id) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(id));
        return item == null || item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    private static Fluid fluid(String id) {
        return AtmoFluids.fluid(ResourceLocation.tryParse(id));
    }

    /** Original {@code WaterTableHandler.getRecipes()}. */
    public List<Entry> waterTable() {
        Map<CelestialBody, List<Slot>> map = new LinkedHashMap<>();
        for (SolarSystem.Body bodyEnum : SolarSystem.Body.values()) {
            CelestialBody body = bodyEnum.getBody();
            if (body == null) continue;
            CBT_Water table = body.getDefaultTrait(CBT_Water.class);
            if (table == null) continue;
            Fluid f = AtmoFluids.fluid(table.fluid);
            if (AtmoFluids.isNone(f)) continue;
            boolean water = com.hbm_m.api.space.SpaceEnvironment.HBM_WATER.equals(table.fluid);
            map.put(body, List.of(Slot.fluid(f, water ? 10_000 : 1_000)));
        }
        return build(map);
    }

    /** Original {@code OilExtractionHandler.getRecipes()} ("TODO: not be hardcoded"). */
    public List<Entry> oilExtraction() {
        Map<CelestialBody, List<Slot>> map = new LinkedHashMap<>();
        for (SolarSystem.Body bodyEnum : SolarSystem.Body.values()) {
            CelestialBody body = bodyEnum.getBody();
            if (body == null) continue;
            List<Slot> out = new ArrayList<>();
            switch (bodyEnum) {
                case KERBIN, DUNA -> { out.add(Slot.fluid(fluid("hbm_m:crude_oil"), 1000)); out.add(Slot.fluid(fluid("hbm_m:gas"), 200)); }
                case EVE -> { out.add(Slot.fluid(fluid("hbm_m:gas"), 1000)); out.add(Slot.fluid(fluid("hbm_m:petroleum"), 200)); }
                case LAYTHE -> { out.add(Slot.fluid(fluid("hbm_m:oil_ds"), 1000)); out.add(Slot.fluid(fluid("hbm_m:gas"), 200)); }
                case TEKTO -> { out.add(Slot.fluid(fluid("space_m:tcrude"), 1000)); out.add(Slot.fluid(fluid("space_m:hgas"), 200)); }
                case MUN, MINMUS, IKE -> out.add(Slot.fluid(fluid("space_m:brine"), 1000));
                default -> { }
            }
            if (out.isEmpty()) continue;
            map.put(body, out);
        }
        return build(map);
    }

    /** Original {@code BedrockDrillHandler.getRecipes()}. */
    public List<Entry> bedrock() {
        Map<CelestialBody, List<Slot>> map = new LinkedHashMap<>();
        for (SolarSystem.Body bodyEnum : SolarSystem.Body.values()) {
            CelestialBody body = bodyEnum.getBody();
            if (body == null) continue;
            List<Slot> out = new ArrayList<>();
            ItemStack ore = baseOreFor(bodyEnum);
            if (!ore.isEmpty()) out.add(Slot.of(ore));
            if (body.hasIce) out.add(Slot.of(new ItemStack(Items.PACKED_ICE, 32)));
            map.put(body, out);
        }
        return build(map);
    }

    /** {@code new ItemStack(bedrock_ore_base, 1, body.ordinal())} = HBM-Item mit NBT {@code body} (CelestialBedrockOre). */
    static ItemStack baseOreFor(SolarSystem.Body body) {
        if (com.space_m.resources.CelestialBedrockOre.get(body) == null) return ItemStack.EMPTY;
        ItemStack stack = stack("hbm_m:bedrock_ore_base");
        if (stack.isEmpty()) return stack;
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        tag.putInt("body", body.ordinal());
        //? if < 1.21.1 {
        stack.setTag(tag);
        //?} else {
        /*stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
        *///?}
        return stack;
    }

    private List<Entry> build(Map<CelestialBody, List<Slot>> map) {
        bodies.clear();
        List<Entry> list = new ArrayList<>();
        map.forEach((body, out) -> {
            Entry e = new Entry(List.of(), out);
            bodies.put(e, body);
            list.add(e);
        });
        return list;
    }

    @Override
    public void draw(Entry recipe, IRecipeSlotsView slots, GuiGraphics g, double mouseX, double mouseY) {
        super.draw(recipe, slots, g, mouseX, mouseY);
        CelestialBody body = bodies.get(recipe);
        if (body == null) return;
        drawBody(g, body);
        // OilExtractionHandler.drawBackground: Kerbin/Tekto "Frackable"
        if (frackableNote && ("kerbin".equals(body.name) || "tekto".equals(body.name))) {
            g.drawString(Minecraft.getInstance().font, "Frackable", 102, 48, 0x000000, false);
        }
    }

    /** {@code NEICelestialHandler.drawBackground}: Planetenbild 32x32 bei (32, 12), UV-Lauf 4 s, Name darunter. */
    static void drawBody(GuiGraphics g, @Nullable CelestialBody body) {
        if (body == null) return;
        if (body.texture != null) {
            float uvOffset = (float) (System.currentTimeMillis() % 4000L) / 4000F;
            int split = Math.round((1F - uvOffset) * 32F);
            if (split > 0) g.blit(body.texture, 32, 12, split, 32, uvOffset * 32F, 0F, split, 32, 32, 32);
            if (split < 32) g.blit(body.texture, 32 + split, 12, 32 - split, 32, 0F, 0F, 32 - split, 32, 32, 32);
        }
        Font font = Minecraft.getInstance().font;
        Component name = Component.translatable("body." + body.name);
        g.drawString(font, name, 16 + 32 - font.width(name) / 2, 48 + 6 - font.lineHeight / 2, 0x000000, false);
    }
}
