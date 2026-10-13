package com.space_m.resources;

import java.util.Arrays;
import java.util.Map;

import com.hbm_m.api.space.SpaceApi;
import com.hbm_m.inventory.material.MaterialShapes;
import com.hbm_m.inventory.material.Mats;
import com.hbm_m.inventory.recipes.ArcFurnaceRecipes;
import com.hbm_m.inventory.recipes.ElectrolyserMetalRecipes;
import com.hbm_m.inventory.recipes.ElectrolyserMetalRecipes.ElectrolysisMetalRecipe;
import com.hbm_m.platform.recipe.RecipeHooks;
import com.hbm_m.recipe.CentrifugeRecipe;
import com.hbm_m.recipe.CombinationOvenRecipe;
import com.hbm_m.recipe.CrystallizerRecipe;
import com.hbm_m.recipe.PyroOvenRecipe;
import com.hbm_m.recipe.ShredderRecipe;
import com.space_m.SpaceMod;
import com.space_m.celestial.SolarSystem;
import com.space_m.resources.ItemCelestialBedrockOre.BedrockOreGrade;
import com.space_m.resources.ItemCelestialBedrockOre.ProcessingTrait;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
//? if forge {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?} elif neoforge {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?}

/**
 * Bedrock-Erz-Verarbeitungsstufen je Himmelskoerper (NTM Space {@code ItemBedrockOreNew} + Rezepte): Stichproben
 * gegen die Originalwerte (Batch {@code space_bedrock}).
 */
@GameTestHolder(SpaceMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BedrockProcessingGameTest {

    private BedrockProcessingGameTest() {}

    private static void check(boolean cond, String msg) {
        if (!cond) throw new GameTestAssertException(msg);
    }

    private static CelestialBedrockOre.Type type(SolarSystem.Body body, String suffix) {
        for (CelestialBedrockOre.Type t : CelestialBedrockOre.get(body).types) if (t.suffix.equals(suffix)) return t;
        throw new GameTestAssertException("type " + body + " " + suffix);
    }

    private static ItemStack ore(BedrockOreGrade g, SolarSystem.Body body, String suffix) {
        return ItemCelestialBedrockOre.make(g, type(body, suffix));
    }

    /** HBM-Zentrifuge fuellt die Ausgaben auf 4 auf (Rest null/leer). */
    private static boolean empty(ItemStack[] out, int from) {
        for (int i = from; i < out.length; i++) if (out[i] != null && !out[i].isEmpty()) return false;
        return true;
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(id));
    }

    /** Tabelle, Items, Grade-Toenung/Traits, Farben, Fragmente. */
    @GameTest(template = "empty3x3x3", batch = "space_bedrock", timeoutTicks = 20)
    public static void bedrockOreTableAndItems(GameTestHelper helper) {
        check(CelestialBedrockOre.getAllTypes().size() == 54, "54 Sorten");
        check(BedrockOreGrade.values().length == 26, "26 Grade");
        check(CelestialBedrockOreItems.all().size() == 54 * 26, "1404 Items, got " + CelestialBedrockOreItems.all().size());
        for (CelestialBedrockOre.Type t : CelestialBedrockOre.getAllTypes()) {
            for (BedrockOreGrade g : BedrockOreGrade.values()) {
                Item it = CelestialBedrockOreItems.ore(g, t);
                check(it instanceof ItemCelestialBedrockOre o && o.getGrade() == g && o.getOreType() == t, "item " + g + " " + t.id());
                check(BuiltInRegistries.ITEM.getKey(it).getPath().equals("bedrock_ore_" + g.key() + "_" + t.body.name + "_" + t.suffix), "id " + it);
            }
            // jedes Ausgangsmaterial hat eine Fragment-Form (HBM oder Addon)
            for (CelestialBedrockOre.Output o : t.outputs()) {
                check(Mats.getItemForShape(o.mat(), MaterialShapes.FRAGMENT) != null, "fragment for " + o.mat().names[0]);
            }
        }

        // Original-Werte (ItemBedrockOreNew.CelestialBedrockOre)
        CelestialBedrockOre.Type kl = type(SolarSystem.Body.KERBIN, "light");
        check(kl.primary.mat() == Mats.MAT_IRON && kl.primary.amount() == 18 && kl.byproductAcid.mat() == Mats.MAT_COPPER
                && kl.byproductSolvent.mat() == Mats.MAT_CRYOLITE && kl.byproductRad.mat() == Mats.MAT_SODIUM && kl.byproductRad.amount() == 3, "kerbin light");
        check(kl.light == 0xE6D2B2 && kl.dark == 0x564733, "kerbin light colours " + Integer.toHexString(kl.light) + "/" + Integer.toHexString(kl.dark));
        CelestialBedrockOre.Type ma = type(SolarSystem.Body.MOHO, "actinide");
        check(ma.primary.mat() == CelestialBedrockMats.MAT_AUSTRALIUM && ma.byproductAcid.mat() == CelestialBedrockMats.MAT_AUSTRALIUM
                && ma.byproductAcid.amount() == 9 && ma.byproductSolvent.mat() == CelestialBedrockMats.MAT_TASMANITE
                && ma.byproductRad.mat() == CelestialBedrockMats.MAT_AYERITE, "moho actinide");
        check(ma.light == 0xFFFF00 && ma.dark == 0x935B00, "moho actinide colours");
        CelestialBedrockOre.Type es = type(SolarSystem.Body.EVE, "schrabidic");
        check(es.primary.mat() == Mats.MAT_SCHRABIDIUM && es.byproductAcid.mat() == Mats.MAT_SOLINIUM
                && es.byproductSolvent.mat() == Mats.MAT_GHIORSIUM && es.byproductSolvent.amount() == 6, "eve schrabidic");
        CelestialBedrockOre.Type tp = type(SolarSystem.Body.TEKTO, "plastic");
        check(tp.primary.mat() == Mats.MAT_POLYMER && tp.byproductRad.mat() == Mats.MAT_PVC, "tekto plastic");
        check(type(SolarSystem.Body.IKE, "hazard").byproductAcid.mat() == Mats.MAT_U238, "ike hazard");

        check(BedrockOreGrade.PRIMARY_SULFURIC.tint == 0xFFFFD3 && BedrockOreGrade.SULFURIC_ARC.tint == 0xC3A2A2
                && BedrockOreGrade.BASE_WASHED.tint == 0xDBE2CB && BedrockOreGrade.RAD_ROASTED.tint == 0xCFCFCF, "grade tints");
        check(Arrays.equals(BedrockOreGrade.PRIMARY_NORAD.traits, new ProcessingTrait[] { ProcessingTrait.CENTRIFUGED, ProcessingTrait.RAD }), "norad traits");
        check(BedrockOreGrade.RAD_WASHED.prefix.equals("rad") && BedrockOreGrade.CRUMBS.prefix.equals("crumbs"), "prefixes");

        check(CelestialBedrockOreItems.fragments().size() == 30, "30 addon fragments");
        check(Mats.getItemForShape(Mats.MAT_PLUTONIUM, MaterialShapes.FRAGMENT) == item("space_m:bedrock_ore_fragment_plutonium"), "pu fragment");
        check(Mats.getItemForShape(Mats.MAT_IRON, MaterialShapes.FRAGMENT) == item("hbm_m:bedrock_ore_fragment_iron"), "hbm iron fragment stays");
        check(Mats.MAT_CO60.autogen.contains(MaterialShapes.FRAGMENT), "co60 autogen fragment");
        helper.succeed();
    }

    private static int count(Map<ResourceLocation, ?> byId, String folder) {
        int n = 0;
        for (ResourceLocation id : byId.keySet()) {
            if (id.getNamespace().equals(SpaceMod.MOD_ID) && id.getPath().startsWith("bedrock_ore/" + folder + "/")) n++;
        }
        return n;
    }

    /** Datapack-Rezepte (Zentrifuge, Kristallisator, Kombinations-/Pyro-Ofen, Schredder). */
    @GameTest(template = "empty3x3x3", batch = "space_bedrock", timeoutTicks = 20)
    public static void bedrockOreJsonRecipes(GameTestHelper helper) {
        Level level = helper.getLevel();
        Map<ResourceLocation, CentrifugeRecipe> cen = RecipeHooks.getAllRecipesById(level, CentrifugeRecipe.Type.INSTANCE);
        Map<ResourceLocation, CrystallizerRecipe> cry = RecipeHooks.getAllRecipesById(level, CrystallizerRecipe.Type.INSTANCE);
        Map<ResourceLocation, CombinationOvenRecipe> comb = RecipeHooks.getAllRecipesById(level, CombinationOvenRecipe.Type.INSTANCE);
        Map<ResourceLocation, PyroOvenRecipe> pyro = RecipeHooks.getAllRecipesById(level, PyroOvenRecipe.Type.INSTANCE);
        check(count(cen, "centrifuge") == 54 * 15, "centrifuge 810, got " + count(cen, "centrifuge"));
        check(count(cry, "crystallizer") == 54 * 29, "crystallizer 1566, got " + count(cry, "crystallizer"));
        check(count(comb, "combination_oven") == 54 * 5, "combination 270, got " + count(comb, "combination_oven"));
        check(count(pyro, "pyro_oven") == 54 * 5, "pyro 270, got " + count(pyro, "pyro_oven"));

        // Zentrifuge: PRIMARY -> extract(primary, 1) = 18 Fragmente (Mun light: Lithium)
        CentrifugeRecipe mp = cen.get(SpaceMod.id("bedrock_ore/centrifuge/mun_light_primary"));
        check(mp != null && mp.getInput().test(ore(BedrockOreGrade.PRIMARY, SolarSystem.Body.MUN, "light")), "mun light primary recipe");
        ItemStack[] out = mp.getOutputs();
        check(empty(out, 1) && out[0].getItem() == Mats.getItemForShape(Mats.MAT_LITHIUM, MaterialShapes.FRAGMENT) && out[0].getCount() == 18, "18 lithium fragments");
        // PRIMARY_FIRST Eve schrabidic: 2x 18 Schrabidium + Kruemel
        ItemStack[] pf = cen.get(SpaceMod.id("bedrock_ore/centrifuge/eve_schrabidic_primary_first")).getOutputs();
        check(empty(pf, 3) && pf[0].getItem() == item("space_m:bedrock_ore_fragment_schrabidium") && pf[1].getCount() == 18
                && ItemStack.isSameItem(pf[2], ore(BedrockOreGrade.CRUMBS, SolarSystem.Body.EVE, "schrabidic")), "eve schrabidic primary_first");
        // PRIMARY_RAD -> 2 NORAD + 2 Schwefel- + 2 Loesungsmittel- + 2 Hochdruck-Nebenprodukt
        ItemStack[] pr = cen.get(SpaceMod.id("bedrock_ore/centrifuge/dres_rare_primary_rad")).getOutputs();
        check(pr[3].getCount() == 2 && ItemStack.isSameItem(pr[3], ore(BedrockOreGrade.RAD_BYPRODUCT, SolarSystem.Body.DRES, "rare")), "primary_rad outputs");
        // SOLVENT_WASHED -> Loesungsmittel-Ausbeute (Tekto plastic: Semtex 6)
        ItemStack[] sw = cen.get(SpaceMod.id("bedrock_ore/centrifuge/tekto_plastic_solvent_washed")).getOutputs();
        check(sw[0].getItem() == item("space_m:bedrock_ore_fragment_semtex") && sw[0].getCount() == 6, "semtex 6");

        // Kristallisator
        CrystallizerRecipe crumbs = cry.get(SpaceMod.id("bedrock_ore/crystallizer/kerbin_light_crumbs_slop"));
        check(crumbs != null && crumbs.getInputCount() == 64 && crumbs.getAcidAmount() == 1000 && crumbs.getDuration() == 200
                && ItemStack.isSameItem(crumbs.getOutput(), ore(BedrockOreGrade.BASE, SolarSystem.Body.KERBIN, "light")), "crumbs -> base (64, slop 1000)");
        CrystallizerRecipe sb = cry.get(SpaceMod.id("bedrock_ore/crystallizer/ike_hazard_sulfuric_byproduct_water"));
        check(sb != null && sb.getInputCount() == 4 && sb.getDuration() == 100 && sb.getAcidAmount() == 250
                && ItemStack.isSameItem(sb.getOutput(), ore(BedrockOreGrade.SULFURIC_WASHED, SolarSystem.Body.IKE, "hazard")), "sulfuric washing req 4");
        CrystallizerRecipe hy = cry.get(SpaceMod.id("bedrock_ore/crystallizer/laythe_crystal_primary_norad_hydrogen"));
        check(hy != null && hy.getInputCount() == 1 && hy.getDuration() == 200
                && ItemStack.isSameItem(hy.getOutput(), ore(BedrockOreGrade.PRIMARY_FIRST, SolarSystem.Body.LAYTHE, "crystal")), "hydrogen -> primary_first");

        // Kombinations-/Pyro-Ofen: Roesten mit 50 mB Vitriol
        CombinationOvenRecipe co = comb.get(SpaceMod.id("bedrock_ore/combination_oven/duna_actinide_rad_byproduct"));
        check(co != null && co.getOutputFluidAmount() == 50 && ItemStack.isSameItem(co.getOutput(), ore(BedrockOreGrade.RAD_ROASTED, SolarSystem.Body.DUNA, "actinide")), "comb roast");
        PyroOvenRecipe po = pyro.get(SpaceMod.id("bedrock_ore/pyro_oven/minmus_heavy_base"));
        check(po != null && po.getOutputFluidMb() == 50 && ItemStack.isSameItem(po.getOutputItem(), ore(BedrockOreGrade.BASE_ROASTED, SolarSystem.Body.MINMUS, "heavy")), "pyro roast");

        // Schredder: jede Stufe -> Kies
        boolean shred = false;
        for (ShredderRecipe r : RecipeHooks.getAllRecipes(level, ShredderRecipe.Type.INSTANCE)) {
            if (r.getInput().test(ore(BedrockOreGrade.SOLVENT_ARC, SolarSystem.Body.MOHO, "rare")) && r.getOutput().is(Items.GRAVEL)) shred = true;
        }
        check(shred, "shredder bedrock ore -> gravel");
        helper.succeed();
    }

    /** Code-Rezepte (Lichtbogenofen, Elektrolyse) und Erzschlammer-Haken. */
    @GameTest(template = "empty3x3x3", batch = "space_bedrock", timeoutTicks = 20)
    public static void bedrockOreCodeRecipesAndSlopper(GameTestHelper helper) {
        // Lichtbogenofen: PRIMARY_FIRST fluessig = toFluid(primary, 5) = ceil(8 * 18 * 5) = 720 Eisen
        ArcFurnaceRecipes.ArcFurnaceRecipe a = ArcFurnaceRecipes.getOutput(ore(BedrockOreGrade.PRIMARY_FIRST, SolarSystem.Body.KERBIN, "light"), true);
        check(a != null && a.fluidOutput != null && a.fluidOutput.length == 1 && a.fluidOutput[0].material == Mats.MAT_IRON
                && a.fluidOutput[0].amount == 720, "arc primary_first 720 iron");
        // RAD_WASHED fluessig = toFluid(rad, 3): Eve light Co-60 3 -> 72
        ArcFurnaceRecipes.ArcFurnaceRecipe r = ArcFurnaceRecipes.getOutput(ore(BedrockOreGrade.RAD_WASHED, SolarSystem.Body.EVE, "light"), true);
        check(r != null && r.fluidOutput[0].material == Mats.MAT_CO60 && r.fluidOutput[0].amount == 72, "arc rad_washed 72 co60");
        // SULFURIC_ROASTED fest -> 4x SULFURIC_ARC
        ArcFurnaceRecipes.ArcFurnaceRecipe s = ArcFurnaceRecipes.getOutput(ore(BedrockOreGrade.SULFURIC_ROASTED, SolarSystem.Body.MUN, "rare"), false);
        check(s != null && s.solidOutput.getCount() == 4 && ItemStack.isSameItem(s.solidOutput, ore(BedrockOreGrade.SULFURIC_ARC, SolarSystem.Body.MUN, "rare")), "arc sulfuric_roasted x4");

        // Elektrolyse: PRIMARY_FIRST = Schmelze ceil(8*18*8) + Kruemel, 20 Ticks
        ElectrolysisMetalRecipe e = ElectrolyserMetalRecipes.getRecipe(ore(BedrockOreGrade.PRIMARY_FIRST, SolarSystem.Body.DUNA, "light"));
        check(e != null && e.output1.material == Mats.MAT_IRON && e.output1.amount == 1152 && e.output2 == null && e.duration == 20
                && e.byproduct.length == 1 && ItemStack.isSameItem(e.byproduct[0], ore(BedrockOreGrade.CRUMBS, SolarSystem.Body.DUNA, "light")), "electrolysis primary_first");
        ElectrolysisMetalRecipe c = ElectrolyserMetalRecipes.getRecipe(ore(BedrockOreGrade.CRUMBS, SolarSystem.Body.DRES, "rare"));
        check(c != null && c.output1.material == Mats.MAT_TANTALIUM && c.output1.amount == 288, "electrolysis crumbs tantalium 288");
        // nicht schmelzbar (Moho nonmetal Glowstone): Schlacke 2 Barren, 64 Fragmente (min(18*8, 64)) + Kruemel
        ElectrolysisMetalRecipe g = ElectrolyserMetalRecipes.getRecipe(ore(BedrockOreGrade.PRIMARY_FIRST, SolarSystem.Body.MOHO, "nonmetal"));
        check(g != null && g.output1.material == Mats.MAT_SLAG && g.output1.amount == MaterialShapes.INGOT.q(2)
                && g.byproduct.length == 2 && g.byproduct[0].getCount() == 64 && g.byproduct[0].getItem() == item("space_m:bedrock_ore_fragment_glowstone"), "electrolysis glowstone slag");

        // Erzschlammer: Ausgabe = Space-Grundstufe der Sorte; ohne NBT Kerbin (4 Sorten, Menge 1)
        ItemStack base = new ItemStack(com.hbm_m.item.ModItems.BEDROCK_ORE_BASE.get());
        Map<ResourceLocation, Double> amounts = SpaceApi.bedrockOres().oreAmounts(base);
        check(amounts != null && amounts.size() == 4, "kerbin 4 types");
        for (Map.Entry<ResourceLocation, Double> en : amounts.entrySet()) {
            ItemStack outStack = SpaceApi.bedrockOres().slopperOutput(en.getKey());
            check(outStack.getItem() instanceof ItemCelestialBedrockOre o && o.getGrade() == BedrockOreGrade.BASE && o.getOreType().id().equals(en.getKey()), "slopper " + en.getKey());
        }
        ItemStack schrab = SpaceApi.bedrockOres().slopperOutput(SpaceMod.id("eve_schrabidic"));
        check(ItemStack.isSameItem(schrab, ore(BedrockOreGrade.BASE, SolarSystem.Body.EVE, "schrabidic")), "eve schrabidic slopper");

        helper.succeed();
    }
}
