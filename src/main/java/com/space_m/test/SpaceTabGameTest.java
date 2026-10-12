package com.space_m.test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.space_m.SpaceMod;
import com.space_m.main.SpaceCreativeTabs;
import com.space_m.rocket.SpaceRocketItems;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
//? if forge {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?} elif neoforge {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?}

/** Sammel-Tab "NTM Space": registriert, enthaelt jedes space_m-Item, Varianten, keine Duplikate. */
@GameTestHolder(SpaceMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SpaceTabGameTest {

    private SpaceTabGameTest() {}

    private static void check(boolean cond, String msg) {
        if (!cond) throw new GameTestAssertException(msg);
    }

    @GameTest(template = "empty3x3x3", batch = "space_tab", timeoutTicks = 20)
    public static void ntmSpaceTabContainsAllItems(GameTestHelper helper) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(SpaceMod.MOD_ID, "ntm_space_tab");
        CreativeModeTab tab = helper.getLevel().registryAccess().registryOrThrow(Registries.CREATIVE_MODE_TAB).get(id);
        check(tab != null && tab == SpaceCreativeTabs.NTM_SPACE_TAB.get(), "tab space_m:ntm_space_tab registered");
        check(tab.getDisplayName().getString().equals("itemGroup.space_m.ntm_space_tab")
                || tab.getDisplayName().getString().equals("NTM Space"), "tab title key");
        check(tab.getIconItem().getItem() == SpaceRocketItems.ROCKET_CUSTOM.get(), "tab icon rocket_custom");

        List<ItemStack> stacks = new ArrayList<>();
        SpaceCreativeTabs.fillSpaceTab(stacks::add);
        Set<Item> inTab = new HashSet<>();
        for (ItemStack st : stacks) inTab.add(st.getItem());
        int n = 0;
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
            if (!SpaceMod.MOD_ID.equals(key.getNamespace())) continue;
            n++;
            check(inTab.contains(item), "missing in NTM Space tab: " + key);
        }
        check(n > 300, "space_m items: " + n);
        long drives = stacks.stream().filter(s -> s.getItem() == SpaceRocketItems.FULL_DRIVE.get()).count();
        check(drives == com.space_m.celestial.SolarSystem.Body.values().length, "one drive per body, found " + drives);
        long fluidContainers = stacks.stream().filter(s -> !SpaceMod.MOD_ID.equals(BuiltInRegistries.ITEM.getKey(s.getItem()).getNamespace())).count();
        check(fluidContainers > 0, "space fluid containers present");
        Set<ItemStack> unique = new it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet<>(
                net.minecraft.world.item.ItemStackLinkedSet.TYPE_AND_TAG);
        for (ItemStack st : stacks) check(unique.add(st), "duplicate stack " + st);
        com.mojang.logging.LogUtils.getLogger().info("NTM Space tab: {} stacks, {} space_m items, {} drives, {} HBM fluid containers",
                stacks.size(), n, drives, fluidContainers);
        helper.succeed();
    }
}
