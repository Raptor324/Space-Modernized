package com.space_m.main;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
//? if forge {
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
//?} elif neoforge {
/*import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
*///?}

/**
 * Befuellt fremde Creative-Tabs (HBM-Tabs, Vanilla-Tabs) direkt ueber das Loader-Ereignis
 * {@code BuildCreativeModeTabContentsEvent} statt ueber Architecturys {@code CreativeTabRegistry.modify}.
 *
 * <p>Grund (Absturz beim Oeffnen des Inventars auf 1.21.1-NeoForge, 2026-10-13): Architectury 13.0.11 leitet auf
 * NeoForge {@code CreativeTabOutput.accept(stack)} auf {@code event.insertAfter(ItemStack.EMPTY, stack)} um. NeoForge
 * verlangt, dass der Anker im Tab steht, und wirft
 * {@code IllegalArgumentException: Itemstack 0 minecraft:air does not exist in tab's list} - fuer <b>jeden</b>
 * {@code output.accept(..)} in einem {@code modify}-Listener. Auf Forge haengt dieselbe Stelle einfach hinten an.
 *
 * <p>Zusaetzlich: leere Stapel werden nie eingefuegt (Warnung mit Quelle im Log), Stapelgroesse wird auf 1 gesetzt
 * (NeoForge verlangt das) und bereits vorhandene Stapel werden uebersprungen (Forge fuehrt Duplikate still zusammen,
 * NeoForge wirft "already exists").
 */
public final class SpaceTabFill {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Senke fuer einen Tab-Befueller; {@code source} erscheint in der Warnung, falls der Stapel leer ist. */
    @FunctionalInterface
    public interface Sink {
        void accept(ItemStack stack, String source);

        default void accept(ItemStack stack) {
            accept(stack, "?");
        }
    }

    private record Filler(ResourceLocation tab, String owner, Consumer<Sink> fill) {}

    private static final List<Filler> FILLERS = new ArrayList<>();
    /** Bereits gemeldete leere Eintraege (Tab|Quelle), damit jede Ursache nur einmal im Log steht. */
    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();
    private static int skippedEmpty;

    private SpaceTabFill() {}

    /** Haengt {@code fill} an den Tab {@code tab} an (Inhalt wird bei jedem Tab-Aufbau neu berechnet). */
    public static synchronized void modify(ResourceLocation tab, String owner, Consumer<Sink> fill) {
        FILLERS.add(new Filler(tab, owner, fill));
    }

    /** Vanilla-Tab per {@code CreativeModeTabs.X}. */
    public static void modify(ResourceKey<CreativeModeTab> tab, String owner, Consumer<Sink> fill) {
        modify(tab.location(), owner, fill);
    }

    /** Anzahl seit Start verworfener leerer Stapel (GameTest). */
    public static int skippedEmpty() {
        return skippedEmpty;
    }

    /** Mod-Bus-Listener (beide Loader, angemeldet in {@code SpaceMod}). */
    public static void onBuildContents(BuildCreativeModeTabContentsEvent event) {
        ResourceLocation tab = event.getTabKey().location();
        for (Filler f : FILLERS) {
            if (!f.tab().equals(tab)) continue;
            f.fill().accept((stack, source) -> add(event, tab, f.owner(), stack, source));
        }
    }

    private static void add(BuildCreativeModeTabContentsEvent event, ResourceLocation tab, String owner,
            ItemStack stack, String source) {
        if (stack == null || stack.isEmpty()) {
            skippedEmpty++;
            if (WARNED.add(tab + "|" + owner + "|" + source)) {
                LOGGER.warn("[space_m] Leerer Stapel fuer Creative-Tab {} verworfen (Quelle {}: {})", tab, owner, source);
            }
            return;
        }
        if (stack.getCount() != 1) stack = stack.copyWithCount(1);
        //? if forge {
        boolean present = event.getEntries().contains(stack);
        //?} elif neoforge {
        /*boolean present = event.getParentEntries().contains(stack) || event.getSearchEntries().contains(stack);
        *///?}
        if (present) {
            LOGGER.debug("[space_m] Creative-Tab {}: {} schon vorhanden, uebersprungen ({})",
                    tab, BuiltInRegistries.ITEM.getKey(stack.getItem()), owner);
            return;
        }
        event.accept(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
    }
}
