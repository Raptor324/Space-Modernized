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
    /**
     * Regression Nutzer-Meldung 2026-10-13 (1.21.1-NeoForge: Inventar oeffnen -> Absturz
     * "Itemstack 0 minecraft:air does not exist in tab's list", ausgeloest durch Architectury-{@code modify}).
     * Baut alle Tabs wie der Client beim Oeffnen des Inventars (HBM + Addon), mit und ohne OP-Rechte, und prueft
     * die Addon-Eintraege in HBM- und Vanilla-Tabs.
     */
    @GameTest(template = "empty3x3x3", batch = "space_tab", timeoutTicks = 40)
    public static void allCreativeTabsBuildWithAddon(GameTestHelper helper) {
        for (boolean ops : new boolean[] { true, false, true }) {
            try {
                net.minecraft.world.item.CreativeModeTabs.tryRebuildTabContents(helper.getLevel().enabledFeatures(), ops,
                        helper.getLevel().registryAccess());
            } catch (RuntimeException e) {
                StringBuilder msg = new StringBuilder("creative tab build failed (ops=" + ops + "): ");
                for (Throwable t = e; t != null && t.getCause() != t; t = t.getCause())
                    msg.append(t.getClass().getSimpleName()).append(": ").append(t.getMessage()).append(" | ");
                com.mojang.logging.LogUtils.getLogger().error("[SpaceTabGameTest] {}", msg, e);
                throw new GameTestAssertException(msg.toString());
            }
        }
        int tabs = 0, total = 0;
        for (CreativeModeTab tab : BuiltInRegistries.CREATIVE_MODE_TAB) {
            if (tab.getType() != CreativeModeTab.Type.CATEGORY) continue;
            tabs++;
            for (ItemStack st : tab.getDisplayItems()) {
                total++;
                check(!st.isEmpty(), "empty stack in tab " + BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab));
            }
        }
        check(com.space_m.main.SpaceTabFill.skippedEmpty() == 0,
                "empty addon tab entries skipped: " + com.space_m.main.SpaceTabFill.skippedEmpty() + " (see log warnings)");

        // Addon-Eintraege in den Vanilla-Tabs
        checkInTab(net.minecraft.world.item.CreativeModeTabs.TOOLS_AND_UTILITIES, com.space_m.item.SpaceNuclearItems.RECORD_GS.get());
        checkInTab(net.minecraft.world.item.CreativeModeTabs.TOOLS_AND_UTILITIES, com.space_m.item.SpaceNuclearItems.RECORD_EL.get());
        checkInTab(net.minecraft.world.item.CreativeModeTabs.COMBAT, com.space_m.item.SpaceMaterialItems.BUTTER_SWORD.get());
        for (var egg : com.space_m.entity.mob.SpaceMobs.eggs())
            checkInTab(net.minecraft.world.item.CreativeModeTabs.SPAWN_EGGS, egg.get());
        checkInTab(net.minecraft.world.item.CreativeModeTabs.SPAWN_EGGS, com.space_m.war.WarEntities.SIEGE_UFO_EGG.get());
        checkInTab(net.minecraft.world.item.CreativeModeTabs.SPAWN_EGGS, com.space_m.war.WarEntities.WAR_BEHEMOTH_EGG.get());

        // Addon-Eintraege in den neun HBM-Tabs: jedes eingetragene Item steht im Tab
        int hbmEntries = 0;
        for (SpaceCreativeTabs.Tab t : SpaceCreativeTabs.Tab.values()) {
            CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.get(t.id());
            check(tab != null, "HBM tab missing: " + t.id());
            Set<Item> items = new HashSet<>();
            for (ItemStack st : tab.getDisplayItems()) items.add(st.getItem());
            for (var sup : SpaceCreativeTabs.entries(t)) {
                ItemStack st = sup.get();
                check(!st.isEmpty(), "empty addon entry for " + t.id());
                check(items.contains(st.getItem()), "addon entry " + BuiltInRegistries.ITEM.getKey(st.getItem()) + " missing in " + t.id());
                hbmEntries++;
            }
        }
        check(hbmEntries > 100, "addon entries in HBM tabs: " + hbmEntries);
        com.mojang.logging.LogUtils.getLogger().info("[SpaceTabGameTest] {} tabs, {} stacks, {} addon entries in HBM tabs",
                tabs, total, hbmEntries);
        helper.succeed();
    }

    private static void checkInTab(net.minecraft.resources.ResourceKey<CreativeModeTab> key, Item item) {
        CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.getOrThrow(key);
        boolean found = tab.getDisplayItems().stream().anyMatch(s -> s.getItem() == item);
        check(found, BuiltInRegistries.ITEM.getKey(item) + " missing in " + key.location());
    }
}
