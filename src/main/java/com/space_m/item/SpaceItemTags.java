package com.space_m.item;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

import com.space_m.dim.PlanetOres;
import com.space_m.dim.minmus.MinmusBlocks;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;

/**
 * R4-B: Ore-Dictionary-Eintraege von NTM Space ({@code OreDictManager}, Space-Zuwachs) als Item-Tags im Schema von
 * HBM-Modernized: {@code hbm_m:oredict/<praefix>/<material>} (Praefix/Material in snake_case, z. B.
 * {@code plateTripleStainlessSteel} -> {@code hbm_m:oredict/plate_triple/stainless_steel}). Jeder DictFrame-Name
 * zaehlt (Curium-242 = {@code curium242} + {@code cm242}). Zusaetzlich die {@code forge:}-Formen-Tags
 * ({@code forge:ingots/nickel_pure} ...), ueber die HBMs {@code Mats.getShape} fremde Formen-Items erkennt
 * (NeoForge: {@code c:} per processResources). Erzeugt im Datagen ({@code SpaceTagProviders}).
 */
public final class SpaceItemTags {

    private SpaceItemTags() {}

    private static BiConsumer<TagKey<Item>, Item> out;

    public static TagKey<Item> oredict(String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("hbm_m", "oredict/" + path));
    }

    private static TagKey<Item> forge(String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("forge", path));
    }

    /** Eintraege {@code <praefix>/<name>} fuer alle Namen. */
    private static void od(String prefix, String[] names, Supplier<? extends ItemLike> item) {
        for (String n : names) out.accept(oredict(prefix + "/" + n), item.get().asItem());
    }

    private static String[] n(String... s) { return s; }

    public static void addItemTags(BiConsumer<TagKey<Item>, Item> consumer) {
        out = consumer;

        // ── Actinide (nugget registriert auch "tiny") ──
        actinide(n("curium242", "cm242"), SpaceMaterialItems.NUGGET_CM242, SpaceMaterialItems.BILLET_CM242, SpaceMaterialItems.INGOT_CM242);
        actinide(n("curium243", "cm243"), SpaceMaterialItems.NUGGET_CM243, SpaceMaterialItems.BILLET_CM243, SpaceMaterialItems.INGOT_CM243);
        actinide(n("curium244", "cm244"), SpaceMaterialItems.NUGGET_CM244, SpaceMaterialItems.BILLET_CM244, SpaceMaterialItems.INGOT_CM244);
        actinide(n("curium245", "cm245"), SpaceMaterialItems.NUGGET_CM245, SpaceMaterialItems.BILLET_CM245, SpaceMaterialItems.INGOT_CM245);
        actinide(n("curium246", "cm246"), SpaceMaterialItems.NUGGET_CM246, SpaceMaterialItems.BILLET_CM246, SpaceMaterialItems.INGOT_CM246);
        actinide(n("curium247", "cm247"), SpaceMaterialItems.NUGGET_CM247, SpaceMaterialItems.BILLET_CM247, SpaceMaterialItems.INGOT_CM247);
        actinide(n("curium_rg"), SpaceMaterialItems.NUGGET_CM_MIX, SpaceMaterialItems.BILLET_CM_MIX, SpaceMaterialItems.INGOT_CM_MIX);
        actinide(n("curium_fuel"), SpaceMaterialItems.NUGGET_CM_FUEL, SpaceMaterialItems.BILLET_CM_FUEL, SpaceMaterialItems.INGOT_CM_FUEL);
        actinide(n("berkelium247", "bk247"), SpaceMaterialItems.NUGGET_BK247, SpaceMaterialItems.BILLET_BK247, SpaceMaterialItems.INGOT_BK247);
        actinide(n("californium251", "cf251"), SpaceMaterialItems.NUGGET_CF251, SpaceMaterialItems.BILLET_CF251, SpaceMaterialItems.INGOT_CF251);
        actinide(n("californium252", "cf252"), SpaceMaterialItems.NUGGET_CF252, SpaceMaterialItems.BILLET_CF252, SpaceMaterialItems.INGOT_CF252);
        actinide(n("einsteinium253", "es253"), SpaceMaterialItems.NUGGET_ES253, SpaceMaterialItems.BILLET_ES253, SpaceMaterialItems.INGOT_ES253);
        od("ingot", n("einsteinium255", "es255"), SpaceMaterialItems.INGOT_ES255);
        String[] cn = n("chinesium989", "cn989");
        actinide(cn, SpaceMaterialItems.NUGGET_CN989, SpaceMaterialItems.BILLET_CN989, SpaceMaterialItems.INGOT_CN989);
        od("dust", cn, SpaceMaterialItems.POWDER_CN989);
        od("plate", cn, SpaceMaterialItems.PLATE_CN989);
        actinide(n("menthol"), SpaceMaterialItems.NUGGET_MENTHOL, SpaceMaterialItems.BILLET_MENTHOL, SpaceMaterialItems.INGOT_MENTHOL);

        // ── Metalle ──
        String[] ni = n("nickel_pure");
        od("ingot", ni, SpaceMaterialItems.INGOT_NICKEL);
        od("dust", ni, SpaceMaterialItems.POWDER_NICKEL);
        od("plate", ni, SpaceMaterialItems.PLATE_NICKEL);
        nugget(ni, SpaceMaterialItems.NUGGET_NICKEL);
        // NIM "Nickel": dust = chunk_ore PENTLANDITE ("dust selected for compat reasons"); PENTLANDITE crystal
        od("dust", n("nickel"), SpaceMaterialItems.CHUNK_ORE_PENTLANDITE);
        od("crystal", n("pentlandite"), SpaceMaterialItems.CHUNK_ORE_PENTLANDITE);
        String[] st = n("stainless_steel");
        od("ingot", st, SpaceMaterialItems.INGOT_STAINLESS);
        od("plate", st, SpaceMaterialItems.PLATE_STAINLESS);
        od("plate_triple", st, SpaceMaterialItems.PLATE_CAST_STAINLESS);   // Autogen CASTPLATE
        od("plate_sextuple", st, SpaceMaterialItems.PLATE_WELDED_STAINLESS); // Autogen WELDEDPLATE
        String[] zi = n("zinc");
        nugget(zi, SpaceMaterialItems.NUGGET_ZINC);
        od("ingot", zi, SpaceMaterialItems.INGOT_ZINC);
        od("dust", zi, SpaceMaterialItems.POWDER_ZINC);
        od("wire_fine", zi, SpaceMaterialItems.WIRE_ZINC);                 // Autogen WIRE (aot wire_zinc)
        String[] ga = n("gallium");
        nugget(ga, SpaceMaterialItems.NUGGET_GALLIUM);
        od("ingot", ga, SpaceMaterialItems.INGOT_GALLIUM);
        od("dust", ga, SpaceMaterialItems.POWDER_GALLIUM);
        od("dust_tiny", ga, SpaceMaterialItems.POWDER_GALLIUM_TINY);
        String[] gaas = n("gallium_arsenide");
        nugget(gaas, SpaceMaterialItems.NUGGET_GAAS);
        od("ingot", gaas, SpaceMaterialItems.INGOT_GAAS);
        od("billet", gaas, SpaceMaterialItems.BILLET_GAAS);
        nugget(n("hafnium"), SpaceMaterialItems.NUGGET_HAFNIUM);
        od("ingot", n("hafnium"), SpaceMaterialItems.INGOT_HAFNIUM);
        od("ingot", n("iridium"), SpaceMaterialItems.INGOT_IRIDIUM);
        od("ingot", n("rich_magma"), SpaceMaterialItems.INGOT_MAGMA);
        od("ingot", n("tasmanite"), SpaceMaterialItems.INGOT_AUSTRALIUM_LESSER);
        od("ingot", n("ayerite"), SpaceMaterialItems.INGOT_AUSTRALIUM_GREATER);
        od("billet", n("mingrade"), SpaceMaterialItems.BILLET_RED_COPPER);
        nugget(n("lanthanum"), SpaceMaterialItems.NUGGET_LANTHANIUM);      // LA.nugget(nugget_lanthanium, fragment_lanthanium)
        od("dust", n("rubber"), SpaceMaterialItems.POWDER_RUBBER);
        od("dust", n("pvc"), SpaceMaterialItems.POWDER_PVC);
        // Koks-Pulver: COALCOKE/PETCOKE/LIGCOKE .dust, ANY_COKE .dust(all), ANY_COAL_COKE (Gruppe + DUST-Praefix)
        od("dust", n("coal_coke", "any_coke", "any_coal_coke"), SpaceMaterialItems.POWDER_COKE_COAL);
        od("dust", n("pet_coke", "any_coke", "any_coal_coke"), SpaceMaterialItems.POWDER_COKE_PETROLEUM);
        od("dust", n("lignite_coke", "any_coke", "any_coal_coke"), SpaceMaterialItems.POWDER_COKE_LIGNITE);
        out.accept(oredict("dust/glowstone"), Items.GLOWSTONE_DUST);           // GLOWSTONE.dust(Items.glowstone_dust)

        // ── Erze (oreAll: alle Koerper-Varianten) ──
        for (PlanetOres.Variant v : PlanetOres.variants()) {
            switch (v.ore()) {
                case "ore_zinc" -> od("ore", zi, v.block());
                case "ore_nickel" -> od("ore", ni, v.block());
                default -> { }
            }
        }
        od("ore", n("conglomerate"), MinmusBlocks.STONE_RESOURCE_CONGLOMERATE);

        // ── Lebensmittel (KEY_FLOUR/BUTTER/STRAWBERRY_ICECREAM/STRAWBERRY_MILKSHAKE) ──
        out.accept(oredict("food_flour"), SpaceMaterialItems.FLOUR.get());
        out.accept(oredict("food_butter"), SpaceMaterialItems.BUTTER.get());
        out.accept(oredict("food_strawberryicecream"), SpaceMaterialItems.S_CREAM.get());
        out.accept(oredict("food_strawberrymilkshake"), SpaceMaterialItems.GLASS_SMILK.get());
        // KEY_STICK
        out.accept(oredict("stick_wood"), SpaceMaterialItems.STICK_PVC.get());
        out.accept(oredict("stick_wood"), SpaceMaterialItems.STICK_VINYL.get());

        // ── forge:-Formen fuer HBMs Mats.getShape (Material-Name snake_case) ──
        shapeTag("ingots", "nickel_pure", SpaceMaterialItems.INGOT_NICKEL);
        shapeTag("nuggets", "nickel_pure", SpaceMaterialItems.NUGGET_NICKEL);
        shapeTag("dusts", "nickel_pure", SpaceMaterialItems.POWDER_NICKEL);
        shapeTag("plates", "nickel_pure", SpaceMaterialItems.PLATE_NICKEL);
        shapeTag("ingots", "zinc", SpaceMaterialItems.INGOT_ZINC);
        shapeTag("nuggets", "zinc", SpaceMaterialItems.NUGGET_ZINC);
        shapeTag("dusts", "zinc", SpaceMaterialItems.POWDER_ZINC);
        shapeTag("wires_fine", "zinc", SpaceMaterialItems.WIRE_ZINC);
        shapeTag("ingots", "gallium", SpaceMaterialItems.INGOT_GALLIUM);
        shapeTag("nuggets", "gallium", SpaceMaterialItems.NUGGET_GALLIUM);
        shapeTag("dusts", "gallium", SpaceMaterialItems.POWDER_GALLIUM);
        shapeTag("tiny_dusts", "gallium", SpaceMaterialItems.POWDER_GALLIUM_TINY);
        shapeTag("ingots", "hafnium", SpaceMaterialItems.INGOT_HAFNIUM);
        shapeTag("nuggets", "hafnium", SpaceMaterialItems.NUGGET_HAFNIUM);
        shapeTag("ingots", "iridium", SpaceMaterialItems.INGOT_IRIDIUM);
        shapeTag("ingots", "gallium_arsenide", SpaceMaterialItems.INGOT_GAAS);
        shapeTag("nuggets", "gallium_arsenide", SpaceMaterialItems.NUGGET_GAAS);
        shapeTag("ingots", "stainless_steel", SpaceMaterialItems.INGOT_STAINLESS);
        shapeTag("plates", "stainless_steel", SpaceMaterialItems.PLATE_STAINLESS);
        shapeTag("ingots", "rich_magma", SpaceMaterialItems.INGOT_MAGMA);
    }

    private static void shapeTag(String folder, String mat, Supplier<? extends ItemLike> item) {
        out.accept(forge(folder + "/" + mat), item.get().asItem());
    }

    private static void nugget(String[] names, Supplier<? extends ItemLike> item) {
        od("nugget", names, item);
        od("tiny", names, item);
    }

    private static void actinide(String[] names, Supplier<? extends ItemLike> nug, Supplier<? extends ItemLike> bil, Supplier<? extends ItemLike> ing) {
        nugget(names, nug);
        od("billet", names, bil);
        od("ingot", names, ing);
    }

    /** Block-Tags: Gummi-Ackerboden (Material.sand -> Schaufel), Feldfruechte. */
    public static void addBlockTags(BiConsumer<TagKey<Block>, Block> consumer) {
        consumer.accept(BlockTags.MINEABLE_WITH_SHOVEL, SpaceCrops.RUBBER_FARMLAND.get());
        consumer.accept(BlockTags.CROPS, SpaceCrops.CROP_MINT.get());
        consumer.accept(BlockTags.CROPS, SpaceCrops.CROP_COFFEE.get());
        consumer.accept(BlockTags.CROPS, SpaceCrops.CROP_TEA.get());
        consumer.accept(BlockTags.CROPS, SpaceCrops.CROP_PARAFFIN.get());
        consumer.accept(BlockTags.MAINTAINS_FARMLAND, SpaceCrops.CROP_MINT.get());
        consumer.accept(BlockTags.MAINTAINS_FARMLAND, SpaceCrops.CROP_COFFEE.get());
        consumer.accept(BlockTags.MAINTAINS_FARMLAND, SpaceCrops.CROP_TEA.get());
        consumer.accept(BlockTags.MAINTAINS_FARMLAND, SpaceCrops.CROP_PARAFFIN.get());
    }
}
