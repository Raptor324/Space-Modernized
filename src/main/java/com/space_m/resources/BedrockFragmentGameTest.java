package com.space_m.resources;

import java.util.Map;
import java.util.Optional;

import com.hbm_m.platform.recipe.RecipeHooks;
import com.hbm_m.util.SimpleCraftingContainer;
import com.space_m.SpaceMod;

import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
//? if forge {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?} elif neoforge {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?}

/**
 * Addon-eigene Bedrock-Fragmente -> Material (NTM Space {@code OreDictManager}-Schleife, {@code MineralRecipes.add9To1}):
 * Stichproben ueber die echte Werkbank-Suche, 30 Addon- + 44 HBM-Rezepte, keine Rueckrichtung.
 */
@GameTestHolder(SpaceMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BedrockFragmentGameTest {

    private BedrockFragmentGameTest() {}

    private static Item item(String id) {
        Item i = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
        if (i == null || i == Items.AIR) throw new GameTestAssertException("Gegenstand fehlt: " + id);
        return i;
    }

    private static SimpleCraftingContainer grid(Item in, int filled) {
        NonNullList<ItemStack> items = NonNullList.withSize(9, ItemStack.EMPTY);
        for (int i = 0; i < filled; i++) items.set(i, new ItemStack(in));
        return new SimpleCraftingContainer(items, 3, 3);
    }

    private static void expect(GameTestHelper helper, String frag, String out) {
        SimpleCraftingContainer g = grid(item(frag), 9);
        Optional<CraftingRecipe> r = RecipeHooks.getCraftingRecipeFor(helper.getLevel(), g);
        if (r.isEmpty()) throw new GameTestAssertException("Kein Rezept fuer 9x " + frag);
        ItemStack res = RecipeHooks.assembleCrafting(r.get(), g, helper.getLevel());
        if (res.getItem() != item(out) || res.getCount() != 1)
            throw new GameTestAssertException(frag + ": erwartet 1x " + out + ", bekommen " + res.getCount() + "x " + BuiltInRegistries.ITEM.getKey(res.getItem()));
    }

    @GameTest(template = "empty3x3x3", batch = "space_bedrock_fragment", timeoutTicks = 20)
    public static void addonFragmentRules(GameTestHelper helper) {
        expect(helper, "space_m:bedrock_ore_fragment_glowstone", "minecraft:glowstone_dust");     // Regel 1, Vanilla
        expect(helper, "space_m:bedrock_ore_fragment_nickel", "space_m:powder_nickel");           // Regel 1, Addon-Pulver
        expect(helper, "space_m:bedrock_ore_fragment_schrabidium", "hbm_m:schrabidium_powder");   // Regel 1, HBM-Pulver
        expect(helper, "space_m:bedrock_ore_fragment_u235", "hbm_m:u235_ingot");                  // Regel 4
        expect(helper, "space_m:bedrock_ore_fragment_tasmanite", "space_m:ingot_australium_lesser"); // Regel 4, Addon-Barren
        expect(helper, "hbm_m:bedrock_ore_fragment_iron", "hbm_m:iron_powder");                   // HBM-Seite mit Addon
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "space_bedrock_fragment", timeoutTicks = 20)
    public static void addonFragmentCountAndNoReverse(GameTestHelper helper) {
        Map<ResourceLocation, CraftingRecipe> all = RecipeHooks.getAllRecipesById(helper.getLevel(), RecipeType.CRAFTING);
        long space = all.keySet().stream().filter(id -> id.getNamespace().equals(SpaceMod.MOD_ID) && id.getPath().startsWith("bedrock_fragment/")).count();
        long hbm = all.keySet().stream().filter(id -> id.getNamespace().equals("hbm_m") && id.getPath().startsWith("bedrock_fragment/")).count();
        if (space != 30 || hbm != 44) throw new GameTestAssertException("Erwartet 30 Addon + 44 HBM, gefunden " + space + " + " + hbm);
        for (Map.Entry<ResourceLocation, CraftingRecipe> e : all.entrySet()) {
            ItemStack res = e.getValue().getResultItem(helper.getLevel().registryAccess());
            if (BuiltInRegistries.ITEM.getKey(res.getItem()).getPath().startsWith("bedrock_ore_fragment_"))
                throw new GameTestAssertException("Rueckrichtung gefunden: " + e.getKey());
        }
        helper.succeed();
    }
}
