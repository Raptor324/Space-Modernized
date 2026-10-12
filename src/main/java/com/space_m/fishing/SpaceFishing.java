package com.space_m.fishing;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.decorations.TrinketTypes.PlushieType;
import com.hbm_m.item.TrinketBlockItem;
import com.space_m.SpaceMod;
import com.space_m.dim.SpaceDimensions;

import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/**
 * Angeln auf den Himmelskoerpern, 1:1 NTM Space {@code ModEventHandler.updateFish} +
 * {@code WorldProviderCelestial.getFish/getJunk/getTreasure} + {@code WorldProviderIke.getPlushie}:
 * <ul>
 *   <li>Kerbin (Overworld) und Orbit (kein {@code WorldProviderCelestial}): Vanilla-Tabellen.</li>
 *   <li>alle anderen Koerper ("tote Welten"): Fische werden durch die Vanilla-Muell-Tabelle von 1.7.10 ersetzt
 *       (Wasserflasche 10, Faden 5, Angel 2 mit bis zu 90 % Abnutzung, Schuessel 10, Stock 5, 10 Tintenbeutel 1,
 *       Haken 10 - Lederstiefel/Leder/Knochen/Gammelfleisch sind im Original auskommentiert); Muell und Schaetze
 *       bleiben die Overworld-Tabellen ({@code getJunk/getTreasure} = null).</li>
 *   <li>Ike: Fisch, Muell und Schatz = Pluesch "Yomi" ({@code new ItemStack(ModBlocks.plushie, 1, 1)}, Gewicht 100).</li>
 * </ul>
 * Umsetzung: Global Loot Modifier auf {@code minecraft:gameplay/fishing} (beide Loader), der in den Space-Dimensionen
 * die Kategorie selbst wuerfelt - wie die Vanilla-Tabelle von 1.20/1.21 (Fisch 85/Qualitaet -1, Muell 10/-2, Schatz 5/+2
 * nur im offenen Wasser, Glueck aus dem Loot-Kontext); die Kategorie-Formel von 1.7.10 ({@code FishingHooks}) ist damit
 * durch die heutige Vanilla-Formel ersetzt, die Inhalte je Kategorie sind 1:1.
 * Original-Klasse: WorldProviderCelestial (getFish/getJunk/getTreasure)
 */
public final class SpaceFishing {

    private SpaceFishing() {}

    public enum Category { FISH, JUNK, TREASURE }

    /** {@code minecraft:gameplay/fishing}. */
    public static final String FISHING_TABLE = "minecraft:gameplay/fishing";

    /** Gilt fuer diese Dimension eine Space-Angeltabelle? (Original: {@code WorldProviderCelestial && dimensionId != 0}) */
    public static boolean isCelestial(ResourceKey<Level> dim) {
        return SpaceDimensions.BODIES.contains(dim);
    }

    /** Vanilla-{@code fishing.json}: Gewicht {@code max(floor(w + q * luck), 0)}, Schatz nur im offenen Wasser. */
    public static Category rollCategory(RandomSource rand, float luck, boolean openWater) {
        int fish = Math.max(net.minecraft.util.Mth.floor(85 + -1 * luck), 0);
        int junk = Math.max(net.minecraft.util.Mth.floor(10 + -2 * luck), 0);
        int treasure = openWater ? Math.max(net.minecraft.util.Mth.floor(5 + 2 * luck), 0) : 0;
        int total = fish + junk + treasure;
        if (total <= 0) return Category.FISH;
        int r = rand.nextInt(total);
        if (r < fish) return Category.FISH;
        if (r < fish + junk) return Category.JUNK;
        return Category.TREASURE;
    }

    /**
     * Ersetzt die Beute eines Angelzugs in einer Space-Dimension.
     *
     * @param vanillaTable rollt eine Vanilla-Untertabelle ({@code JUNK}/{@code TREASURE}) mit demselben Kontext
     * @return null = unveraendert (keine Space-Dimension)
     */
    public static List<ItemStack> replace(LootContext context, Function<Category, List<ItemStack>> vanillaTable) {
        ResourceKey<Level> dim = context.getLevel().dimension();
        if (!isCelestial(dim)) return null;

        boolean openWater = context.getParamOrNull(LootContextParams.THIS_ENTITY) instanceof FishingHook hook && hook.isOpenWaterFishing();
        Category category = rollCategory(context.getRandom(), context.getLuck(), openWater);
        return loot(dim, category, context.getRandom(), vanillaTable);
    }

    /** Beute je Koerper und Kategorie (oeffentlich fuer den GameTest). */
    public static List<ItemStack> loot(ResourceKey<Level> dim, Category category, RandomSource rand, Function<Category, List<ItemStack>> vanillaTable) {
        List<ItemStack> out = new ArrayList<>();
        if (dim == SpaceDimensions.IKE) {
            out.add(getPlushie());
        } else if (category == Category.FISH) {
            out.add(deadWorldJunk(rand));
        } else {
            out.addAll(vanillaTable.apply(category));
        }
        return out;
    }

    /** {@code WorldProviderIke.getPlushie}: einziger Eintrag Pluesch Meta 1 (= YOMI), Gewicht 100. */
    public static ItemStack getPlushie() {
        return TrinketBlockItem.make(ModBlocks.PLUSHIE.get().asItem(), PlushieType.YOMI.ordinal());
    }

    /** {@code WorldProviderCelestial.getFish} (Vanilla-Muell von 1.7.10 ohne Stiefel/Leder/Knochen/Gammelfleisch). */
    public static ItemStack deadWorldJunk(RandomSource rand) {
        // potionitem 10, string 5, fishing_rod 2 (0.9), bowl 10, stick 5, dye x10 (Tintenbeutel) 1, tripwire_hook 10
        int r = rand.nextInt(10 + 5 + 2 + 10 + 5 + 1 + 10);
        if ((r -= 10) < 0) return waterBottle();
        if ((r -= 5) < 0) return new ItemStack(Items.STRING);
        if ((r -= 2) < 0) return damaged(new ItemStack(Items.FISHING_ROD), 0.9F, rand);
        if ((r -= 10) < 0) return new ItemStack(Items.BOWL);
        if ((r -= 5) < 0) return new ItemStack(Items.STICK);
        if ((r -= 1) < 0) return new ItemStack(Items.INK_SAC, 10);
        return new ItemStack(Blocks.TRIPWIRE_HOOK);
    }

    /** 1.7.10 {@code WeightedRandomFishable.func_150708_a} mit {@code func_150709_a(f)}. */
    static ItemStack damaged(ItemStack stack, float f, RandomSource rand) {
        int max = stack.getMaxDamage();
        int i = (int) (f * max);
        int j = max - rand.nextInt(rand.nextInt(i) + 1);
        if (j > i) j = i;
        if (j < 1) j = 1;
        stack.setDamageValue(j);
        return stack;
    }

    /** {@code new ItemStack(Items.potionitem)} (Meta 0 = Wasserflasche). */
    static ItemStack waterBottle() {
        //? if < 1.21.1 {
        return net.minecraft.world.item.alchemy.PotionUtils.setPotion(new ItemStack(Items.POTION), net.minecraft.world.item.alchemy.Potions.WATER);
        //?} else {
        /*return net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.POTION, net.minecraft.world.item.alchemy.Potions.WATER);
        *///?}
    }

    public static net.minecraft.resources.ResourceLocation id() {
        return SpaceMod.id("space_fishing");
    }
}
