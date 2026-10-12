package com.space_m.item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.hbm_m.item.ItemCustomLore;
import com.hbm_m.item.LoreTooltipItem;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.food.HbmFoodItem;
import com.hbm_m.item.machine.ItemRTGPellet;
import com.hbm_m.item.rbmk.RBMKPelletItem;
import com.hbm_m.item.rbmk.RBMKRodItem;
import com.space_m.main.SpaceCreativeTabs.Tab;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

/**
 * R4-B: neue Gegenstaende aus NTM Space ({@code com.hbm.items.ModItems}, 187 neue Felder, plus neue Metadaten-Varianten
 * von {@code item_expensive}, {@code circuit}, {@code chunk_ore}, {@code ore_byproduct}, {@code pellet_rtg_depleted},
 * {@code powder_coke}, {@code mineral_fragment} und Autogen-Formen der Space-Materialien). AUTOMATISCH ERZEUGT durch
 * {@code scratchpad/sR4b_gen.py} - Registry-ID = Original-{@code setUnlocalizedName}; Metadaten-Varianten nach
 * HBM-Modernized-Muster {@code <basis>_<variante>} bzw. (Schaltkreise) Enum-Name klein wie {@code atomic_clock}.
 *
 * <p>Nicht hier: Raketenteile/Transporter-Linker (R5), Satelliten (R6), Festplatte voll (R7), Engelsspawner (R8),
 * Schallplatten (R4-D); {@code strawberry}/{@code pellet_rtg_berkelium} gibt es schon in HBM-Modernized.
 */
public final class SpaceMaterialItems {

    private SpaceMaterialItems() {}

    private static final List<RegistrySupplier<Item>> ALL = new ArrayList<>();

    @SuppressWarnings("unchecked")
    private static <T extends Item> RegistrySupplier<T> basic(String id, java.util.function.Supplier<T> s, Tab tab) {
        RegistrySupplier<T> r = SpaceItems.basic(id, s, tab);
        ALL.add((RegistrySupplier<Item>) (RegistrySupplier<?>) r);
        return r;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Item> RegistrySupplier<T> plain(String id, java.util.function.Supplier<T> s, Tab tab) {
        RegistrySupplier<T> r = SpaceItems.register(id, s, tab);
        ALL.add((RegistrySupplier<Item>) (RegistrySupplier<?>) r);
        return r;
    }

    /** Alle hier registrierten Gegenstaende (GameTest, Tags). */
    public static List<RegistrySupplier<Item>> all() { return Collections.unmodifiableList(ALL); }

    public static final RegistrySupplier<Item> AMMONIUM_NITRATE = basic("ammonium_nitrate",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> BALL_FERRIC_CLAY = basic("ball_ferric_clay",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> BEAN_ROAST = basic("bean_roast",
            () -> new Item(new Item.Properties()), Tab.CONSUMABLE);
    public static final RegistrySupplier<Item> BERYLLIUM_MIRROR = basic("beryllium_mirror",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> BILLET_BK247 = basic("billet_bk247",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> BILLET_CF251 = basic("billet_cf251",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> BILLET_CF252 = basic("billet_cf252",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> BILLET_CM242 = basic("billet_cm242",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> BILLET_CM243 = basic("billet_cm243",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> BILLET_CM244 = basic("billet_cm244",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> BILLET_CM245 = basic("billet_cm245",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> BILLET_CM246 = basic("billet_cm246",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> BILLET_CM247 = basic("billet_cm247",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> BILLET_CM_FUEL = basic("billet_cm_fuel",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> BILLET_CM_MIX = basic("billet_cm_mix",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> BILLET_CN989 = basic("billet_cn989",
            () -> new ItemCustomLore(new Item.Properties()).setRarity(Rarity.EPIC), Tab.PARTS);
    public static final RegistrySupplier<Item> BILLET_ES253 = basic("billet_es253",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> BILLET_GAAS = basic("billet_gaas",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> BILLET_MENTHOL = basic("billet_menthol",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> BILLET_RED_COPPER = basic("billet_red_copper",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> BLADE_SYNGAS = basic("blade_syngas",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> BUTTER = basic("butter",
            () -> new Item(new Item.Properties()), Tab.CONSUMABLE);
    public static final RegistrySupplier<Item> CMUG_EMPTY = basic("cmug_empty",
            () -> new Item(new Item.Properties()), Tab.CONSUMABLE);
    public static final RegistrySupplier<Item> COIN_AIRLINER = basic("coin_airliner",
            () -> new ItemCustomLore(new Item.Properties()).setRarity(Rarity.UNCOMMON), Tab.CONSUMABLE);
    public static final RegistrySupplier<Item> CRYSTAL_CLEANED = basic("crystal_cleaned",
            () -> new ItemCustomLore(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> CRYSTAL_MINERAL = basic("crystal_mineral",
            () -> new ItemCustomLore(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> CRYSTAL_NICKEL = basic("crystal_nickel",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> CRYSTAL_NIOBIUM = basic("crystal_niobium",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> CRYSTAL_ZINC = basic("crystal_zinc",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> FLESH = basic("flesh",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> FLOUR = basic("flour",
            () -> new Item(new Item.Properties()), Tab.CONSUMABLE);
    public static final RegistrySupplier<Item> GLASS_EMPTY = basic("glass_empty",
            () -> new Item(new Item.Properties()), Tab.CONSUMABLE);
    public static final RegistrySupplier<Item> HARD_DRIVE = basic("hard_drive",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> INGOT_AUSTRALIUM_GREATER = basic("ingot_australium_greater",
            () -> new ItemCustomLore(new Item.Properties()).setRarity(Rarity.UNCOMMON), Tab.PARTS);
    public static final RegistrySupplier<Item> INGOT_AUSTRALIUM_LESSER = basic("ingot_australium_lesser",
            () -> new ItemCustomLore(new Item.Properties()).setRarity(Rarity.UNCOMMON), Tab.PARTS);
    public static final RegistrySupplier<Item> INGOT_BK247 = basic("ingot_bk247",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> INGOT_CF251 = basic("ingot_cf251",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> INGOT_CF252 = basic("ingot_cf252",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> INGOT_CM242 = basic("ingot_cm242",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> INGOT_CM243 = basic("ingot_cm243",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> INGOT_CM244 = basic("ingot_cm244",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> INGOT_CM245 = basic("ingot_cm245",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> INGOT_CM246 = basic("ingot_cm246",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> INGOT_CM247 = basic("ingot_cm247",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> INGOT_CM_FUEL = basic("ingot_cm_fuel",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> INGOT_CM_MIX = basic("ingot_cm_mix",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> INGOT_CN989 = basic("ingot_cn989",
            () -> new ItemCustomLore(new Item.Properties()).setRarity(Rarity.EPIC), Tab.PARTS);
    public static final RegistrySupplier<Item> INGOT_ES253 = basic("ingot_es253",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> INGOT_ES255 = basic("ingot_es255",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> INGOT_GAAS = basic("ingot_gaas",
            () -> new ItemCustomLore(new Item.Properties()).setRarity(Rarity.RARE), Tab.PARTS);
    public static final RegistrySupplier<Item> INGOT_GALLIUM = basic("ingot_gallium",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> INGOT_GWENIUM = basic("ingot_gwenium",
            () -> new ItemCustomLore(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> INGOT_HAFNIUM = basic("ingot_hafnium",
            () -> new ItemCustomLore(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> INGOT_IRIDIUM = basic("ingot_iridium",
            () -> new ItemCustomLore(new Item.Properties()).setRarity(Rarity.RARE), Tab.PARTS);
    public static final RegistrySupplier<Item> INGOT_MAGMA = basic("ingot_magma",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> INGOT_MENTHOL = basic("ingot_menthol",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> INGOT_NICKEL = basic("ingot_nickel",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> INGOT_PALLADIUM = basic("ingot_palladium",
            () -> new ItemCustomLore(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> INGOT_STAINLESS = basic("ingot_stainless",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> INGOT_TT = basic("ingot_tt",
            () -> new ItemCustomLore(new Item.Properties()).setRarity(Rarity.UNCOMMON), Tab.PARTS);
    public static final RegistrySupplier<Item> INGOT_TTAS = basic("ingot_ttas",
            () -> new ItemCustomLore(new Item.Properties()).setRarity(Rarity.RARE), Tab.PARTS);
    public static final RegistrySupplier<Item> INGOT_ZINC = basic("ingot_zinc",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> MINERAL_DUST = basic("mineral_dust",
            () -> new ItemCustomLore(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> NICKEL_SALTS = basic("nickel_salts",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> NUGGET_BK247 = basic("nugget_bk247",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> NUGGET_CF251 = basic("nugget_cf251",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> NUGGET_CF252 = basic("nugget_cf252",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> NUGGET_CM242 = basic("nugget_cm242",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> NUGGET_CM243 = basic("nugget_cm243",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> NUGGET_CM244 = basic("nugget_cm244",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> NUGGET_CM245 = basic("nugget_cm245",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> NUGGET_CM246 = basic("nugget_cm246",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> NUGGET_CM247 = basic("nugget_cm247",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> NUGGET_CM_FUEL = basic("nugget_cm_fuel",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> NUGGET_CM_MIX = basic("nugget_cm_mix",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> NUGGET_CN989 = basic("nugget_cn989",
            () -> new ItemCustomLore(new Item.Properties()).setRarity(Rarity.EPIC), Tab.PARTS);
    public static final RegistrySupplier<Item> NUGGET_ES253 = basic("nugget_es253",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> NUGGET_GAAS = basic("nugget_gaas",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> NUGGET_GALLIUM = basic("nugget_gallium",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> NUGGET_HAFNIUM = basic("nugget_hafnium",
            () -> new ItemCustomLore(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> NUGGET_LANTHANIUM = basic("nugget_lanthanium",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> NUGGET_MENTHOL = basic("nugget_menthol",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> NUGGET_NICKEL = basic("nugget_nickel",
            () -> new ItemCustomLore(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> NUGGET_ZINC = basic("nugget_zinc",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> PLATE_CN989 = basic("plate_cn989",
            () -> new ItemCustomLore(new Item.Properties()).setRarity(Rarity.EPIC), Tab.PARTS);
    public static final RegistrySupplier<Item> PLATE_NICKEL = basic("plate_nickel",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> PLATE_STAINLESS = basic("plate_stainless",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> POWDER_CN989 = basic("powder_cn989",
            () -> new ItemCustomLore(new Item.Properties()).setRarity(Rarity.EPIC), Tab.PARTS);
    public static final RegistrySupplier<Item> POWDER_COFFEE = basic("powder_coffee",
            () -> new ItemCustomLore(new Item.Properties()), Tab.CONSUMABLE);
    public static final RegistrySupplier<Item> POWDER_GALLIUM = basic("powder_gallium",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> POWDER_GALLIUM_TINY = basic("powder_gallium_tiny",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> POWDER_NICKEL = basic("powder_nickel",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> POWDER_PVC = basic("powder_pvc",
            () -> new ItemCustomLore(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> POWDER_RUBBER = basic("powder_rubber",
            () -> new ItemCustomLore(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> POWDER_WD2004 = basic("powder_wd2004",
            () -> new ItemCustomLore(new Item.Properties()).setRarity(Rarity.EPIC), Tab.PARTS);
    public static final RegistrySupplier<Item> POWDER_WD2004_TINY = basic("powder_wd2004_tiny",
            () -> new ItemCustomLore(new Item.Properties()).setRarity(Rarity.EPIC), Tab.PARTS);
    public static final RegistrySupplier<Item> POWDER_ZINC = basic("powder_zinc",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> RAG_BLOOD = basic("rag_blood",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> SCUTTERTAIL = basic("scuttertail",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> STICK_PVC = basic("stick_pvc",
            () -> new ItemCustomLore(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> STICK_VINYL = basic("stick_vinyl",
            () -> new ItemCustomLore(new Item.Properties()), Tab.PARTS);
    // R9-B4: 3D-Darstellung (builtin/entity, Original ItemRenderLibrary) -> plain + Hand-JSON
    public static final RegistrySupplier<Item> SWARM_MEMBER = plain("swarm_member",
            () -> new ItemSwarmMember(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> TEA_LEAF = basic("tea_leaf",
            () -> new Item(new Item.Properties()), Tab.CONSUMABLE);
    public static final RegistrySupplier<Item> TEACUP_EMPTY = basic("teacup_empty",
            () -> new Item(new Item.Properties()), Tab.CONSUMABLE);
    public static final RegistrySupplier<Item> TURBINE_SYNGAS = basic("turbine_syngas",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> WOODEMIUM_BRIQUETTE = basic("woodemium_briquette",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> CHOCOLATE_MINT_BILLET = basic("chocolate_mint_billet",
            () -> HbmFoodItem.of(5, 5F, true).build(), Tab.CONSUMABLE);
    public static final RegistrySupplier<Item> FLESH_BURGER = basic("flesh_burger",
            () -> HbmFoodItem.of(7, 1.0F, false).build(), Tab.PARTS);
    public static final RegistrySupplier<Item> FLESH_WAFER = basic("flesh_wafer",
            () -> HbmFoodItem.of(5, 0.6F, false).build(), Tab.PARTS);
    public static final RegistrySupplier<Item> GRILLED_FLESH = basic("grilled_flesh",
            () -> HbmFoodItem.of(6, 0.8F, false).build(), Tab.PARTS);
    public static final RegistrySupplier<Item> MIN_CREAM = basic("min_cream",
            () -> HbmFoodItem.of(10, 1.0F, false).build(), Tab.CONSUMABLE);
    public static final RegistrySupplier<Item> S_CREAM = basic("s_cream",
            () -> HbmFoodItem.of(8, 1.0F, false).potion(MobEffects.REGENERATION, 20, 2, 1F).build(), Tab.CONSUMABLE);
    public static final RegistrySupplier<Item> BEAN_RAW = basic("bean_raw",
            () -> SpaceCrops.seed(SpaceCrops.CROP_COFFEE.get(), 1, 0.2F), Tab.CONSUMABLE);
    public static final RegistrySupplier<Item> MINT_LEAVES = basic("mint_leaves",
            () -> SpaceCrops.seed(SpaceCrops.CROP_MINT.get(), 3, 0.4F), Tab.CONSUMABLE);
    public static final RegistrySupplier<Item> TEASEEDS = basic("teaseeds",
            () -> SpaceCrops.seed(SpaceCrops.CROP_TEA.get(), 0, 0F), Tab.CONSUMABLE);
    public static final RegistrySupplier<Item> PARAFFIN_SEEDS = basic("paraffin_seeds",
            () -> SpaceCrops.seed(SpaceCrops.CROP_PARAFFIN.get(), 1, 0.2F), Tab.CONSUMABLE);
    public static final RegistrySupplier<Item> BOTTLE_HONEY = basic("bottle_honey",
            () -> new ItemEnergyGlass(new Item.Properties(), ItemEnergyGlass.Kind.HONEY), Tab.CONSUMABLE);
    public static final RegistrySupplier<Item> GLASS_SMILK = basic("glass_smilk",
            () -> new ItemEnergyGlass(new Item.Properties(), ItemEnergyGlass.Kind.STRAWBERRY_MILK), null);
    public static final RegistrySupplier<Item> TEACUP = basic("teacup",
            () -> new ItemEnergyGlass(new Item.Properties(), ItemEnergyGlass.Kind.TEA), Tab.CONSUMABLE);
    public static final RegistrySupplier<Item> ANIMAN = basic("animan",
            () -> new ItemPillAniman(new Item.Properties()), Tab.CONSUMABLE);
    // Original CreativeTabs.tabCombat (Vanilla-Tab, eingetragen in SpaceMaterialItems.init)
    public static final RegistrySupplier<Item> BUTTER_SWORD = basic("butter_sword",
            () -> new ItemButterSword(new Item.Properties()), null);
    // R9-B4: 3D-Darstellung (builtin/entity, Original ItemRenderAngelCore) -> plain + Hand-JSON
    public static final RegistrySupplier<Item> CORE_ANGEL = plain("core_angel",
            () -> new ItemAngelCore(new Item.Properties().stacksTo(1), 16_000_000, 16_000_000, 800_000), Tab.CONTROL);
    public static final RegistrySupplier<Item> INSERT_CMB = basic("insert_cmb",
            () -> new ItemModInsertCorrosion(9999, 0.7F, 0.9F, 0.4F, 1F), Tab.CONSUMABLE);
    public static final RegistrySupplier<Item> FLIPPERS = basic("flippers",
            () -> new ItemModFlippers(new Item.Properties().stacksTo(1)), Tab.CONSUMABLE);
    public static final RegistrySupplier<Item> V1 = basic("v1",
            () -> new ItemModV1(new Item.Properties().stacksTo(1)), Tab.CONSUMABLE);
    public static final RegistrySupplier<Item> PELLET_RTG_CF251 = basic("pellet_rtg_cf251",
            () -> new ItemRTGPellet(com.hbm_m.config.VersatileConfig.rtgDecay() ? 600 : 200, (long) (ItemRTGPellet.lifespan(1F, ItemRTGPellet.HalfLifeType.SHORT) * 2.5), () -> ModItems.PELLET_RTG_DEPLETED_LEAD.get(), new Item.Properties()), Tab.CONTROL);
    public static final RegistrySupplier<Item> PELLET_RTG_CF252 = basic("pellet_rtg_cf252",
            () -> new ItemRTGPellet(com.hbm_m.config.VersatileConfig.rtgDecay() ? 600 : 200, (long) (ItemRTGPellet.lifespan(1F, ItemRTGPellet.HalfLifeType.SHORT) * 2.5), () -> ModItems.PELLET_RTG_DEPLETED_LEAD.get(), new Item.Properties()), Tab.CONTROL);
    public static final RegistrySupplier<Item> RBMK_PELLET_BK247 = plain("rbmk_pellet_bk247",
            () -> new RBMKPelletItem(new Item.Properties()).setFullName("Highly Enriched Berkelium-247").setYield(100000000D).setReactivity(50).setHeat(2D).setMeltingPoint(2993), Tab.CONTROL);
    public static final RegistrySupplier<Item> RBMK_PELLET_LECM = plain("rbmk_pellet_lecm",
            () -> new RBMKPelletItem(new Item.Properties()).setFullName("Low Enriched Curium-245").setYield(30000000D).setReactivity(20).setHeat(1.25D).setMeltingPoint(1340).setDiffusion(0.4D), Tab.CONTROL);
    public static final RegistrySupplier<Item> RBMK_PELLET_MECM = plain("rbmk_pellet_mecm",
            () -> new RBMKPelletItem(new Item.Properties()).setFullName("Medium Enriched Curium-245").setYield(27000000D).setReactivity(30).setHeat(1.25D).setMeltingPoint(1720).setDiffusion(0.3D), Tab.CONTROL);
    public static final RegistrySupplier<Item> RBMK_PELLET_HECM = plain("rbmk_pellet_hecm",
            () -> new RBMKPelletItem(new Item.Properties()).setFullName("Highly Enriched Curium-245").setYield(24000000D).setReactivity(60).setHeat(1.3D).setMeltingPoint(1880).setDiffusion(0.3D), Tab.CONTROL);
    public static final RegistrySupplier<Item> RBMK_FUEL_BK247 = basic("rbmk_fuel_bk247",
            () -> new RBMKRodItem((RBMKPelletItem) RBMK_PELLET_BK247.get(), new Item.Properties()).setYield(100000000D).setStats(50).setFunction(RBMKRodItem.EnumBurnFunc.LINEAR).setHeat(2D).setMeltingPoint(2993).setPellet(() -> RBMK_PELLET_BK247.get()), Tab.CONTROL);
    public static final RegistrySupplier<Item> POWDER_COKE_COAL = basic("powder_coke_coal",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> POWDER_COKE_LIGNITE = basic("powder_coke_lignite",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> POWDER_COKE_PETROLEUM = basic("powder_coke_petroleum",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> CLUMP_PEROXIDE = basic("clump_peroxide",
            () -> new ItemMineralClump(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> CLUMP_NITRIC = basic("clump_nitric",
            () -> new ItemMineralClump(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> CLUMP_SULFURIC = basic("clump_sulfuric",
            () -> new ItemMineralClump(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> CLUMP_SOLVENT = basic("clump_solvent",
            () -> new ItemMineralClump(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> CLUMP_HYDROCHLORIC = basic("clump_hydrochloric",
            () -> new ItemMineralClump(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> CLUMP_SCHRABIDIC = basic("clump_schrabidic",
            () -> new ItemMineralClump(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> ITEM_EXPENSIVE_STAINLESS_BEAM = basic("item_expensive_stainless_beam",
            () -> new LoreTooltipItem(List.of(Component.translatable("tooltip.hbm_m.item_expensive.desc").withStyle(ChatFormatting.RED)), new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> ITEM_EXPENSIVE_AVIONICS = basic("item_expensive_avionics",
            () -> new LoreTooltipItem(List.of(Component.translatable("tooltip.hbm_m.item_expensive.desc").withStyle(ChatFormatting.RED)), new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> ITEM_EXPENSIVE_ASTRO = basic("item_expensive_astro",
            () -> new LoreTooltipItem(List.of(Component.translatable("tooltip.hbm_m.item_expensive.desc").withStyle(ChatFormatting.RED)), new Item.Properties()), Tab.PARTS);
    // ItemCircuit.EnumCircuitType.GAAS
    public static final RegistrySupplier<Item> GAAS = basic("gaas",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    // ItemCircuit.EnumCircuitType.AVIONICS
    public static final RegistrySupplier<Item> AVIONICS = basic("avionics",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    // ItemCircuit.EnumCircuitType.AERO
    public static final RegistrySupplier<Item> AERO = basic("aero",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    // ItemCircuit.EnumCircuitType.CAPACITOR_LANTHANIUM
    public static final RegistrySupplier<Item> CAPACITOR_LANTHANIUM = basic("capacitor_lanthanium",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    // ItemCircuit.EnumCircuitType.PROCESST1
    public static final RegistrySupplier<Item> PROCESST1 = basic("processt1",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    // ItemCircuit.EnumCircuitType.PROCESST2
    public static final RegistrySupplier<Item> PROCESST2 = basic("processt2",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    // ItemCircuit.EnumCircuitType.PROCESST3
    public static final RegistrySupplier<Item> PROCESST3 = basic("processt3",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    // ItemCircuit.EnumCircuitType.GASCHIP
    public static final RegistrySupplier<Item> GASCHIP = basic("gaschip",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    // ItemCircuit.EnumCircuitType.HFCHIP
    public static final RegistrySupplier<Item> HFCHIP = basic("hfchip",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    // ItemCircuit.EnumCircuitType.MOLYCHIP
    public static final RegistrySupplier<Item> MOLYCHIP = basic("molychip",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> CHUNK_ORE_PENTLANDITE = basic("chunk_ore_pentlandite",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    public static final RegistrySupplier<Item> PELLET_RTG_DEPLETED_AMERICIUM = basic("pellet_rtg_depleted_americium",
            () -> new com.hbm_m.item.machine.ItemRTGPelletDepleted(new Item.Properties()), Tab.CONTROL);
    public static final RegistrySupplier<Item> ORE_BYPRODUCT_B_ARSENIC = basic("ore_byproduct_b_arsenic",
            () -> new Item(new Item.Properties()), null);
    // Original ohne Lang-Schluessel (zeigt den Rohschluessel) - Name analog b_arsenic
    public static final RegistrySupplier<Item> ORE_BYPRODUCT_B_STRONTIUM = basic("ore_byproduct_b_strontium",
            () -> new Item(new Item.Properties()), null);
    // ItemAutogen(plate_cast) hbmmat.stainlesssteel
    public static final RegistrySupplier<Item> PLATE_CAST_STAINLESS = basic("plate_cast_stainless",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    // ItemAutogen(plate_welded) hbmmat.stainlesssteel
    public static final RegistrySupplier<Item> PLATE_WELDED_STAINLESS = basic("plate_welded_stainless",
            () -> new Item(new Item.Properties()), Tab.PARTS);
    // ItemAutogen(WIRE) MAT_ZINC
    public static final RegistrySupplier<Item> WIRE_ZINC = basic("wire_zinc",
            () -> new Item(new Item.Properties()), Tab.PARTS);

    /** Erzwingt das Laden der Klasse vor {@code SpaceItems.ITEMS.register()} (Aufruf in {@link SpaceItems#init()}). */
    public static void init() {
        ItemButterSword.addToCombatTab(BUTTER_SWORD);
    }
}
