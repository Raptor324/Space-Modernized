package com.space_m.item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.hbm_m.item.machine.ItemFELCrystal;
import com.hbm_m.item.nuclear.WatzPelletItem;
import com.hbm_m.item.nuclear.WatzPelletType;
import com.hbm_m.item.rbmk.RBMKPelletItem;
import com.hbm_m.item.rbmk.RBMKRodItem;
import com.hbm_m.item.special.ItemModRecord;
import com.hbm_m.util.function.Function.FunctionLinear;
import com.hbm_m.util.function.Function.FunctionSqrt;
import com.space_m.main.SpaceCreativeTabs.Tab;
import com.space_m.sound.SpaceSounds;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * R9-B (R4-B-Rest): Kernbrennstoffe und Sonstiges aus NTM Space, das auf HBM-Erweiterungspunkten (sAPI) aufsetzt.
 * <ul>
 *   <li>{@code watz_pellet}/{@code watz_pellet_depleted} der Space-Typen PU241, AMRG, AMF, CMRG, CMF, BK247, CF251, CF252,
 *       ES253 (Original {@code ItemWatzPellet.EnumWatzType}, Werte 1:1) ueber {@link WatzPelletType#register}; Registry
 *       {@code watz_pellet_<typ>} / {@code watz_pellet_depleted_<typ>} wie HBM. Ertrag 1 000 000 000 (Space-Wert des
 *       Enums). Texturen 1:1 aus {@code RGBMutatorInterpolatedComponentRemap} erzeugt ({@code scratchpad/sR9b_watztex.py}).</li>
 *   <li>Curium-RBMK-Staebe {@code rbmk_fuel_lecm/mecm/hecm} ({@code EnumBurnFunc.SLOW_LINEAR}, {@code EnumDepleteFunc.CF_SLOPE}).</li>
 *   <li>{@code laser_crystal_iron} ({@code ItemFELCrystal(EnumWavelengths.XRAY)}).</li>
 *   <li>Schallplatten {@code record_gs/gp/el} ({@code ItemModRecord}, Vanilla-Tab "Werkzeuge" = 1.7.10 {@code tabMisc}).</li>
 * </ul>
 */
// Original-Klasse: ItemWatzPellet (Space-Typen)
// Original-Klasse: ItemModRecord (Space-Platten gs/gp/el)
public final class SpaceNuclearItems {

    private SpaceNuclearItems() {}

    private static final List<RegistrySupplier<Item>> GENERATED = new ArrayList<>();

    // ── Watz (ItemWatzPellet.EnumWatzType, Space-Zuwachs) ────────────────────────────────────────────────────────
    public static final WatzPelletType PU241 = space(WatzPelletType.register("PU241", 0x78817E, 394240, 1_950, 25, 0.0025D, new FunctionLinear(1.30D), new FunctionSqrt(2.66D / 18D).withOff(24D * 24D), null));
    public static final WatzPelletType AMRG = space(WatzPelletType.register("AMRG", 0x93767B, 0x66474D, 2_888, 48, 0.0035D, new FunctionLinear(1.33D), new FunctionSqrt(4.33D / 25.5D).withOff(28D * 28D), null));
    public static final WatzPelletType AMF = space(WatzPelletType.register("AMF", 0x93767B, 0x66474D, 2_333, 44, 0.003D, new FunctionLinear(1.33D), new FunctionSqrt(4.11D / 22.2D).withOff(27D * 27D), null));
    public static final WatzPelletType CMRG = space(WatzPelletType.register("CMRG", 0xD8C2C4, 0xAD9799, 2_999, 50, 0.005D, new FunctionLinear(1.5D), new FunctionSqrt(5.5D / 25.5D).withOff(30D * 28D), null));
    public static final WatzPelletType CMF = space(WatzPelletType.register("CMF", 0xD8C2C4, 0xAD9799, 2_444, 48, 0.0045D, new FunctionLinear(1.8D), new FunctionSqrt(5.0D / 20D).withOff(26D * 24D), null));
    public static final WatzPelletType BK247 = space(WatzPelletType.register("BK247", 0xC2C9C7, 0x8D9592, 3_000, 55, 0.012D, new FunctionLinear(1.5D), new FunctionSqrt(6.0D / 23.5D).withOff(10D * 10D), null));
    public static final WatzPelletType CF251 = space(WatzPelletType.register("CF251", 0x7879B4, 0x4D4E89, 1_250, 60, 0.001D, new FunctionLinear(1.7D), new FunctionSqrt(6.65D / 23.5D).withOff(10D * 10D), null));
    public static final WatzPelletType CF252 = space(WatzPelletType.register("CF252", 0x7879B4, 0x4D4E89, 1_050, 120, 0.0015D, new FunctionLinear(1.8D), new FunctionSqrt(8.85D / 28.8D).withOff(10D * 10D), null));
    public static final WatzPelletType ES253 = space(WatzPelletType.register("ES253", 0xB9BFB2, 0x594E44, 3_750, 70, 0.0001D, new FunctionLinear(1.3D), new FunctionSqrt(7.0D / 27.7D).withOff(10D * 10D), null));

    private static final WatzPelletType[] WATZ_TYPES = {PU241, AMRG, AMF, CMRG, CMF, BK247, CF251, CF252, ES253};

    public static final Map<WatzPelletType, RegistrySupplier<Item>> WATZ_PELLET = new LinkedHashMap<>();
    public static final Map<WatzPelletType, RegistrySupplier<Item>> WATZ_PELLET_DEPLETED = new LinkedHashMap<>();

    static {
        for (WatzPelletType t : WATZ_TYPES) {
            WATZ_PELLET.put(t, gen("watz_pellet_" + t.id(), () -> new WatzPelletItem(new Item.Properties(), t, false), Tab.CONTROL));
        }
        for (WatzPelletType t : WATZ_TYPES) {
            WATZ_PELLET_DEPLETED.put(t, gen("watz_pellet_depleted_" + t.id(), () -> new WatzPelletItem(new Item.Properties(), t, true), Tab.CONTROL));
        }
        for (WatzPelletType t : WATZ_TYPES) t.withItems(WATZ_PELLET.get(t), WATZ_PELLET_DEPLETED.get(t));
    }

    // ── RBMK (ModItems rbmk_fuel_lecm/mecm/hecm) ─────────────────────────────────────────────────────────────────
    public static final RegistrySupplier<Item> RBMK_FUEL_LECM = gen("rbmk_fuel_lecm",
            () -> new RBMKRodItem((RBMKPelletItem) SpaceMaterialItems.RBMK_PELLET_LECM.get(), new Item.Properties()).setYield(30000000D).setStats(20, 5)
                    .setFunction(RBMKRodItem.EnumBurnFunc.SLOW_LINEAR).setDepletionFunction(RBMKRodItem.EnumDepleteFunc.CF_SLOPE)
                    .setHeat(1.25D).setMeltingPoint(1340).setDiffusion(0.4D).setPellet(() -> SpaceMaterialItems.RBMK_PELLET_LECM.get()), Tab.CONTROL);
    public static final RegistrySupplier<Item> RBMK_FUEL_MECM = gen("rbmk_fuel_mecm",
            () -> new RBMKRodItem((RBMKPelletItem) SpaceMaterialItems.RBMK_PELLET_MECM.get(), new Item.Properties()).setYield(27000000D).setStats(30, 10)
                    .setFunction(RBMKRodItem.EnumBurnFunc.SLOW_LINEAR).setDepletionFunction(RBMKRodItem.EnumDepleteFunc.CF_SLOPE)
                    .setHeat(1.25D).setMeltingPoint(1720).setDiffusion(0.3D).setPellet(() -> SpaceMaterialItems.RBMK_PELLET_MECM.get()), Tab.CONTROL);
    public static final RegistrySupplier<Item> RBMK_FUEL_HECM = gen("rbmk_fuel_hecm",
            () -> new RBMKRodItem((RBMKPelletItem) SpaceMaterialItems.RBMK_PELLET_HECM.get(), new Item.Properties()).setYield(24000000D).setStats(60, 25)
                    .setFunction(RBMKRodItem.EnumBurnFunc.SLOW_LINEAR).setDepletionFunction(RBMKRodItem.EnumDepleteFunc.CF_SLOPE)
                    .setHeat(1.3D).setMeltingPoint(1880).setDiffusion(0.3D).setPellet(() -> SpaceMaterialItems.RBMK_PELLET_HECM.get()), Tab.CONTROL);

    // ── FEL (ModItems laser_crystal_iron) ────────────────────────────────────────────────────────────────────────
    public static final RegistrySupplier<Item> LASER_CRYSTAL_IRON = gen("laser_crystal_iron",
            () -> new ItemFELCrystal(ItemFELCrystal.EnumWavelengths.XRAY, new Item.Properties()), Tab.CONTROL);

    // ── Schallplatten (ModItems record_gs/gp/el, CreativeTabs.tabMisc) ─────────────────────────────────────────────
    // Laenge = OGG-Dauer (scratchpad/sR9b_ogglen.py) in Ticks, Komparator 1 wie HBM record_lc
    public static final RegistrySupplier<Item> RECORD_GS = gen("record_gs",
            () -> new ItemModRecord(1, () -> SpaceSounds.MUSIC_RECORD_GODSPEED.get(), new Item.Properties(), 7063), null);
    public static final RegistrySupplier<Item> RECORD_GP = gen("record_gp",
            () -> new ItemModRecord(1, () -> SpaceSounds.MUSIC_RECORD_GOOP.get(), new Item.Properties(), 4816), null);
    public static final RegistrySupplier<Item> RECORD_EL = gen("record_el",
            () -> new ItemModRecord(1, () -> SpaceSounds.MUSIC_RECORD_ETHEREAL.get(), new Item.Properties(), 3955), null);

    private static WatzPelletType space(WatzPelletType t) {
        t.yield = 1_000_000_000D; // Space ItemWatzPellet.EnumWatzType.yield
        return t;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Item> RegistrySupplier<Item> gen(String id, java.util.function.Supplier<T> s, Tab tab) {
        RegistrySupplier<Item> r = (RegistrySupplier<Item>) (RegistrySupplier<?>) (tab == null
                ? SpaceItems.basic(id, s, null) : SpaceItems.basic(id, s, tab));
        GENERATED.add(r);
        return r;
    }

    public static List<RegistrySupplier<Item>> all() { return Collections.unmodifiableList(GENERATED); }

    public static WatzPelletType[] watzTypes() { return WATZ_TYPES.clone(); }

    /** Klassenladen (Registrierung) + Platten im Vanilla-Werkzeug-Tab. */
    public static void init() {
        CreativeTabRegistry.modifyBuiltin(BuiltInRegistries.CREATIVE_MODE_TAB.getOrThrow(CreativeModeTabs.TOOLS_AND_UTILITIES),
                (flags, output, canUseOps) -> {
                    output.accept(new ItemStack(RECORD_GS.get()));
                    output.accept(new ItemStack(RECORD_GP.get()));
                    output.accept(new ItemStack(RECORD_EL.get()));
                });
    }
}
