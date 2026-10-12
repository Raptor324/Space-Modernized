package com.space_m.test;

import java.util.List;

import com.hbm_m.hazard.HazardRegistry;
import com.hbm_m.hazard.HazardSystem;
import com.hbm_m.inventory.material.MaterialShapes;
import com.hbm_m.inventory.material.Mats;
import com.hbm_m.inventory.recipes.CrucibleRecipes;
import com.space_m.SpaceMod;
import com.space_m.item.HazardTypeNeutron;
import com.space_m.item.SpaceCrops;
import com.space_m.item.SpaceItemTags;
import com.space_m.item.SpaceMaterialItems;
import com.space_m.material.SpaceMats;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
//? if forge {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?} elif neoforge {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?}

/**
 * R4-B Materialien/Items: Registrierung, Giesserei-Materialien + Schmelzwerte, Tiegel-Legierungen, Gefahren
 * (Strahlung/Neutronen/Hitze), Rezepte, oredict-Tags, Feldfruechte.
 */
@GameTestHolder(SpaceMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MaterialsGameTest {

    private MaterialsGameTest() {}

    private static void check(boolean cond, String msg) {
        if (!cond) throw new GameTestAssertException(msg);
    }

    @GameTest(template = "empty3x3x3", batch = "space_materials")
    public static void itemsRegistered(GameTestHelper helper) {
        check(SpaceMaterialItems.all().size() >= 150, "too few R4-B items: " + SpaceMaterialItems.all().size());
        for (var r : SpaceMaterialItems.all()) check(r.get() != Items.AIR, "air item " + r.getId());
        check(SpaceMaterialItems.ITEM_EXPENSIVE_STAINLESS_BEAM.getId().getPath().equals("item_expensive_stainless_beam"), "stainless beam id");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "space_materials")
    public static void materialsAndCrucible(GameTestHelper helper) {
        check(Mats.matById.get(24_001) == SpaceMats.MAT_STAINLESS, "MAT_STAINLESS id 24001");
        check(Mats.matByName.get("NickelPure") == SpaceMats.MAT_NICKEL, "MAT_NICKEL name");
        List<Mats.MaterialStack> m = Mats.getMaterialsFromItem(new ItemStack(SpaceMaterialItems.INGOT_STAINLESS.get()));
        check(m.size() == 1 && m.get(0).material == SpaceMats.MAT_STAINLESS && m.get(0).amount == MaterialShapes.INGOT.q(1), "stainless ingot smelts to 72 quanta");
        // W-R4b-1: Formen-Tags (forge:, NeoForge c:) zaehlen nicht doppelt, Giessform gibt das Addon-Item aus
        check(new ItemStack(SpaceMaterialItems.INGOT_NICKEL.get()).getTags().anyMatch(t -> t.location().getPath().equals("ingots/nickel_pure")
                && (t.location().getNamespace().equals("forge") || t.location().getNamespace().equals("c"))), "forge/c-Formen-Tag ingots/nickel_pure");
        List<Mats.MaterialStack> ni = Mats.getMaterialsFromItem(new ItemStack(SpaceMaterialItems.INGOT_NICKEL.get()));
        check(ni.size() == 1 && ni.get(0).amount == MaterialShapes.INGOT.q(1), "nickel ingot smelts to 72 quanta (einfach)");
        check(Mats.getItemForShape(SpaceMats.MAT_STAINLESS, MaterialShapes.PLATE) == SpaceMaterialItems.PLATE_STAINLESS.get(), "Giessform Edelstahlplatte");
        List<Mats.MaterialStack> b = Mats.getMaterialsFromItem(new ItemStack(SpaceMaterialItems.BLADE_SYNGAS.get()));
        check(!b.isEmpty() && b.get(0).amount == MaterialShapes.INGOT.q(3), "blade_syngas = 3 stainless ingots");
        check(CrucibleRecipes.get("crucible.stainless") != null, "crucible.stainless");
        check(CrucibleRecipes.get("crucible.arse") != null, "crucible.arse");
        check(CrucibleRecipes.get("crucible.hsss") != null, "crucible.hsss");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "space_materials")
    public static void hazards(GameTestHelper helper) {
        ItemStack cf = new ItemStack(SpaceMaterialItems.INGOT_CF252.get());
        check(Math.abs(HazardSystem.getHazardLevelFromStack(cf, HazardRegistry.RADIATION) - 15.3F) < 1e-3, "cf252 ingot 15.3 RAD");
        check(Math.abs(HazardSystem.getHazardLevelFromStack(cf, HazardTypeNeutron.NEUTRON) - 15.3F / 5F) < 1e-3, "cf252 neutron entry");
        check(Math.abs(HazardSystem.getHazardLevelFromStack(cf, HazardRegistry.HOT) - 3F) < 1e-3, "cf252 hot 3");
        ItemStack nug = new ItemStack(SpaceMaterialItems.NUGGET_CM242.get());
        check(Math.abs(HazardSystem.getHazardLevelFromStack(nug, HazardRegistry.RADIATION) - 0.93F) < 1e-3, "cm242 nugget 0.93 RAD");
        check(HazardSystem.getHazardLevelFromStack(new ItemStack(SpaceMaterialItems.POWDER_WD2004.get()), HazardRegistry.DIGAMMA) == 1F, "wd2004 digamma");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "space_materials")
    public static void recipesAndTags(GameTestHelper helper) {
        var rm = helper.getLevel().getRecipeManager();
        for (String id : new String[] {"material/press/plate_stainless", "material/crafting/ingot_nickel", "material/smelting/ore_zinc_mun",
                "material/shredder/flour", "material/blast_furnace/stainless", "material/assembler/exstainlessbeam",
                "material/crystallizer/nickel_salts", "material/arc_welder/plate_welded_stainless"}) {
            check(rm.byKey(SpaceMod.id(id)).isPresent(), "recipe space_m:" + id + " missing");
        }
        check(new ItemStack(SpaceMaterialItems.PLATE_STAINLESS.get()).is(SpaceItemTags.oredict("plate/stainless_steel")), "plate/stainless_steel tag");
        check(new ItemStack(SpaceMaterialItems.PLATE_CAST_STAINLESS.get()).is(SpaceItemTags.oredict("plate_triple/stainless_steel")), "plate_triple tag");
        check(new ItemStack(SpaceMaterialItems.NUGGET_CM242.get()).is(SpaceItemTags.oredict("nugget/cm242")), "cm242 alias tag");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "space_materials")
    public static void cropsGrow(GameTestHelper helper) {
        BlockPos soil = new BlockPos(1, 1, 1);
        helper.setBlock(soil, Blocks.FARMLAND.defaultBlockState());
        helper.setBlock(soil.above(), SpaceCrops.CROP_MINT.get().defaultBlockState());
        BlockPos abs = helper.absolutePos(soil.above());
        BlockState s = helper.getLevel().getBlockState(abs);
        s.randomTick(helper.getLevel(), abs, helper.getLevel().random);
        check(SpaceCrops.CROP_MINT.get().getAge(helper.getLevel().getBlockState(abs)) == 1, "crop grows one stage per random tick");
        check(!SpaceCrops.CROP_PARAFFIN.get().defaultBlockState().canSurvive(helper.getLevel(), abs), "paraffin needs rubber farmland");
        helper.succeed();
    }
}
