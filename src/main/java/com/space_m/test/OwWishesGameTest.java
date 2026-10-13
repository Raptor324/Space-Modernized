package com.space_m.test;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.decorations.TrinketTypes;
import com.hbm_m.hazard.HazardRegistry;
import com.hbm_m.hazard.HazardSystem;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.recipes.ElectrolyserFluidRecipes;
import com.hbm_m.item.TrinketBlockItem;
import com.hbm_m.platform.recipe.RecipeHooks;
import com.hbm_m.recipe.MixerRecipes;
import com.hbm_m.recipe.PseudoFluidType;
import com.hbm_m.recipe.SilexRecipe;
import com.space_m.SpaceMod;
import com.space_m.fluid.SpaceFluids;
import com.space_m.fork.trinket.ForkTrinkets;
import com.space_m.item.HazardTypeGlitch;
import com.space_m.material.SpaceGasCentrifuge;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
//? if forge {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?} elif neoforge {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?}

/**
 * Runde ow (offene HBM-Wuensche): Mischer-/Elektrolyse-Rezepte aus dem Datapack (W-R9B5-1/2), SILEX-Stufe XRAY (W-R9B5-3),
 * Mineralschlamm in der Gaszentrifuge (W-R9B5-5), Magma-Zentrifuge mit Gallium-/Arsen-Fragmenten (W-R9B5-4),
 * Wackelkopf-Gefahren GLITCH/ASBESTOS (W-R9B-1).
 */
@GameTestHolder(SpaceMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class OwWishesGameTest {

    private OwWishesGameTest() {}

    private static void check(boolean cond, String msg) {
        if (!cond) throw new GameTestAssertException(msg);
    }

    @GameTest(template = "empty3x3x3", batch = "space_ow")
    public static void mixerFromDatapack(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        var smilk = MixerRecipes.getOutput(sl, SpaceFluids.SMILK.fluid());
        check(smilk != null && smilk.length == 1 && smilk[0].output == 500 && smilk[0].processTime == 50
                && smilk[0].solidInput != null && smilk[0].solidInput.stacksize() == 4, "Erdbeermilch 500 mB / 50 t / 4 Erdbeeren");
        var nitric = MixerRecipes.getOutput(sl, ModFluids.NITRIC_ACID.getSource());
        check(nitric != null && nitric.length == 2 && nitric[0].input1 != null && nitric[0].input1.type() == SpaceFluids.AMMONIA.fluid()
                && nitric[1].solidInput != null, "Salpetersaeure: Ammoniak zuerst, dann Schwefelsaeure + Salpeter");
        var oxy = MixerRecipes.getOutput(sl, ModFluids.OXYHYDROGEN.getSource());
        check(oxy != null && oxy.length == 1 && oxy[0].input2 != null && oxy[0].input2.type() == ModFluids.OXYGEN.getSource(), "Knallgas nur aus Sauerstoff");
        var rad = MixerRecipes.getOutput(sl, ModFluids.RADIOSOLVENT.getSource());
        check(rad != null && rad.length == 2 && rad[1].output == 5000, "Radiosolvent + Geysir-Fassung");
        var vinyl = MixerRecipes.getOutput(sl, SpaceFluids.VINYL.fluid());
        check(vinyl != null && vinyl.length == 3 && vinyl[2].input2 != null, "Vinyl 3 Fassungen in Originalreihenfolge");
        check(MixerRecipes.getRecipes() != MixerRecipes.getRecipes(sl.getRecipeManager()), "Datapack-Ebene aktiv");
        check(MixerRecipes.getOutput(null, SpaceFluids.SMILK.fluid()) == null, "ohne RecipeManager nur Code-Tabelle");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "space_ow")
    public static void electrolysisFromDatapack(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        var brine = ElectrolyserFluidRecipes.getRecipe(sl, SpaceFluids.BRINE.fluid());
        check(brine != null && brine.amount == 400 && brine.duration == 40 && brine.output1.type() == ModFluids.HYDROGEN.getSource()
                && brine.byproduct.length == 1 && brine.byproduct[0].getCount() == 2, "Sole 400 mB -> H2/O2 + 2 Natrium");
        var ni = ElectrolyserFluidRecipes.getRecipe(sl, SpaceFluids.AQUEOUS_NICKEL.fluid());
        check(ni != null && ni.output1.type() == ModFluids.NONE.getSource() && ni.output2.fill() == 0 && ni.byproduct.length == 3, "Nickelloesung 3 Nebenprodukte");
        var lith = ElectrolyserFluidRecipes.getRecipe(sl, SpaceFluids.LITHCARBONATE.fluid());
        check(lith != null && lith.output1.fill() == 12_000 && lith.duration == 10, "Lithiumcarbonat 12 000 mB O2 / 10 t");
        check(ElectrolyserFluidRecipes.getRecipe(sl, ModFluids.WATER.getSource()) != null, "HBM-Wasser unveraendert");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "space_ow")
    public static void silexXrayAndCentrifuges(GameTestHelper helper) {
        var rm = helper.getLevel().getRecipeManager();
        var hea = RecipeHooks.getRecipeByKey(rm, SpaceMod.id("r9b5/silex/rbmk_pellet_hea242_0"));
        check(hea.isPresent() && hea.get() instanceof SilexRecipe s && s.isXray() && s.getLaser() == 4, "hea242 = Stufe XRAY");
        check(RecipeHooks.getRecipeByKey(rm, SpaceMod.id("r9b5/centrifuge/ingot_magma")).isPresent(), "Vulkanit-Zentrifuge");
        check(RecipeHooks.getRecipeByKey(rm, SpaceMod.id("r9b5/gas_centrifuge/minsol")).isPresent(), "Gaszentrifuge-Anzeige Mineralschlamm");
        check(PseudoFluidType.FLUID_CONVERSIONS.get(SpaceFluids.MINSOL.fluid()) == SpaceGasCentrifuge.MINSOL, "Mineralschlamm -> MINSOL");
        check(SpaceGasCentrifuge.MINSOL.getOutputType() == SpaceGasCentrifuge.MINSOLSEP && SpaceGasCentrifuge.MINSOL.getFluidConsumed() == 500
                && SpaceGasCentrifuge.MINSOL.getFluidProduced() == 500 && !SpaceGasCentrifuge.MINSOL.isHighSpeed(), "MINSOL 500/500 -> MINSOLSEP");
        check(SpaceGasCentrifuge.MINSOLSEP.getOutputType() == PseudoFluidType.NONE && SpaceGasCentrifuge.MINSOLSEP.getOutput().size() == 1, "MINSOLSEP -> Kristalle");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "space_ow")
    public static void bobbleHazards(GameTestHelper helper) {
        ItemStack microwave = TrinketBlockItem.make(ModBlocks.BOBBLEHEAD.get().asItem(), TrinketTypes.BobbleType.MICROWAVE.ordinal());
        ItemStack bob = TrinketBlockItem.make(ModBlocks.BOBBLEHEAD.get().asItem(), TrinketTypes.BobbleType.BOB.ordinal());
        check(HazardSystem.getHazardLevelFromStack(microwave, HazardTypeGlitch.GLITCH) == 1F, "MICROWAVE = GLITCH 1");
        check(HazardSystem.getHazardLevelFromStack(bob, HazardTypeGlitch.GLITCH) == 0F, "andere Wackelkoepfe ohne GLITCH");
        check(HazardSystem.getHazardLevelFromStack(ForkTrinkets.bobble(ForkTrinkets.ForkBobbleType.JUICE), HazardRegistry.ASBESTOS) == 1F, "JUICE = ASBESTOS 1");
        check(HazardSystem.getHazardLevelFromStack(ForkTrinkets.bobble(ForkTrinkets.ForkBobbleType.GWEN), HazardRegistry.ASBESTOS) == 0F, "GWEN ohne Asbest");
        helper.succeed();
    }
}
