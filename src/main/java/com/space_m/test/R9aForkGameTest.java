package com.space_m.test;

import java.math.BigInteger;

import com.hbm_m.annihilator.AnnihilatorRecipes;
import com.hbm_m.config.GeneralConfig;
import com.hbm_m.item.ModItems;
import com.space_m.SpaceMod;
import com.space_m.lib.SpaceTags;
import com.space_m.material.SpaceAnnihilator;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
//? if forge {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?} elif neoforge {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?}

/**
 * R9-A: Space-Anteile aus den 183 "X?"-Kernklassen, die das Addon ohne HBM-Aenderung nachzieht:
 * Korrosionsschutz-Einsatz {@code insert_yharonite} (ItemModInsert), Koker-Rezepte Brom/Scutterblut (CokerRecipes),
 * Annihilator-Eintraege "space fork" (AnnihilatorRecipes, 528-Modus).
 */
@GameTestHolder(SpaceMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class R9aForkGameTest {

    private R9aForkGameTest() {}

    private static void check(boolean cond, String msg) {
        if (!cond) throw new GameTestAssertException(msg);
    }

    @GameTest(template = "empty3x3x3", batch = "space_r9a")
    public static void corrosionInsertTag(GameTestHelper helper) {
        check(new ItemStack(ModItems.INSERT_YHARONITE.get()).is(SpaceTags.CORROSION_PROTECTION), "insert_yharonite ohne space_m:corrosion_protection");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "space_r9a")
    public static void cokerRecipes(GameTestHelper helper) {
        var rm = helper.getLevel().getRecipeManager();
        check(rm.byKey(SpaceMod.id("material/coker/bromine")).isPresent(), "Koker-Rezept Brom fehlt");
        check(rm.byKey(SpaceMod.id("material/coker/scutterblood")).isPresent(), "Koker-Rezept Scutterblut fehlt");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "space_r9a")
    public static void annihilatorSpaceFork(GameTestHelper helper) {
        boolean was528 = GeneralConfig.enable528;
        try {
            GeneralConfig.enable528 = true;
            SpaceAnnihilator.addSpaceRecipes();
        } finally {
            GeneralConfig.enable528 = was528;
        }
        check(SpaceAnnihilator.added(), "Space-Eintraege nicht eingetragen");
        var gas = AnnihilatorRecipes.recipes.get("comp:space_m:gaschip");
        check(gas != null && gas.stream().anyMatch(m -> m.amount().equals(new BigInteger("512"))), "comp:space_m:gaschip 512");
        check(AnnihilatorRecipes.recipes.keySet().stream().anyMatch(k -> k.startsWith("dict:") && k.endsWith(":ingots/stainless_steel")), "Edelstahl-Schluessel");
        check(AnnihilatorRecipes.recipes.keySet().stream().anyMatch(k -> k.startsWith("dict:") && k.endsWith(":ingots/aluminum")), "Aluminium-Schluessel");
        helper.succeed();
    }
}
