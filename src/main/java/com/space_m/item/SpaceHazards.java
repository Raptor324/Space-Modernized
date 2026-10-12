package com.space_m.item;

import static com.hbm_m.hazard.HazardRegistry.DIGAMMA;
import static com.hbm_m.hazard.HazardRegistry.EXPLOSIVE;
import static com.hbm_m.hazard.HazardRegistry.HOT;
import static com.hbm_m.hazard.HazardRegistry.RADIATION;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.hbm_m.hazard.HazardData;
import com.hbm_m.hazard.HazardEntry;
import com.hbm_m.hazard.HazardSystem;
import com.hbm_m.hazard.modifier.HazardModifierRBMKHot;
import com.hbm_m.hazard.modifier.HazardModifierRBMKRadiation;
import com.hbm_m.hazard.type.HazardTypeBase;

import net.minecraft.world.item.Item;

/**
 * R4-B: Gefahren der Space-Gegenstaende ueber die vorhandene HBM-API ({@link HazardSystem#register(Item, HazardData)}),
 * rein additiv - nur Addon-Items, Raptors Strahlungssystem bleibt unberuehrt.
 *
 * <p>Quelle 1:1: {@code OreDictManager} (DictFrame {@code .rad/.neutron/.hot} je Form mit {@code hazMult}: Nugget 0.1,
 * Billet 0.5, Barren 1, Pulver 3, Platte 1) und {@code HazardRegistry} (Space-Konstanten {@code cm242 = 9.3} ...,
 * {@code powder_wd2004}, {@code ammonium_nitrate}, RBMK-Stab/-Pellets, verfallenes Americium-RTG-Pellet).
 *
 * <p>Nicht hier (W-R4b-2): Neutronen-Zusaetze des Space-Zweigs an HBM-eigenen Gegenstaenden (Daemonenkern,
 * Pu-RG/Pu-240, Be-Quellen, Pile-Staebe) - die wuerden HBM-Regeln ueberschreiben.
 */
public final class SpaceHazards {

    private SpaceHazards() {}

    // HazardRegistry (Space-Zuwachs)
    public static final float cm242 = 9.3F, cm243 = 5.6F, cm244 = 2.0F, cm245 = 0.8F, cm246 = 2.5F, cm247 = 0.2F;
    public static final float cmrg = 6.0F, cmf = 2.2F, bk247 = 10.5F, cf251 = 14.3F, cf252 = 15.3F, es253 = 18.3F, es255 = 19.3F;
    public static final float cn989 = 89.0F;
    static final float amrg = 9.0F, wst = 15.0F;
    static final float nugget = 0.1F, ingot = 1.0F, billet = 0.5F, powder = ingot * 3F, plate = ingot, rtg = billet * 3, rod_rbmk = 0.5F * 8;

    private static boolean done;

    public static synchronized void register() {
        if (done) return;
        done = true;
        HazardTypeBase N = HazardTypeNeutron.NEUTRON;

        // OreDictManager: Actinide (nugget/billet/ingot)
        frame(f(RADIATION, cm242), SpaceMaterialItems.NUGGET_CM242, SpaceMaterialItems.BILLET_CM242, SpaceMaterialItems.INGOT_CM242);
        frame(f(RADIATION, cm243, N, cm243 / 80f), SpaceMaterialItems.NUGGET_CM243, SpaceMaterialItems.BILLET_CM243, SpaceMaterialItems.INGOT_CM243);
        frame(f(RADIATION, cm244, N, cm244 / 50f), SpaceMaterialItems.NUGGET_CM244, SpaceMaterialItems.BILLET_CM244, SpaceMaterialItems.INGOT_CM244);
        frame(f(RADIATION, cm245, N, cm245 / 100f), SpaceMaterialItems.NUGGET_CM245, SpaceMaterialItems.BILLET_CM245, SpaceMaterialItems.INGOT_CM245);
        frame(f(RADIATION, cm246, N, cm246 / 50f), SpaceMaterialItems.NUGGET_CM246, SpaceMaterialItems.BILLET_CM246, SpaceMaterialItems.INGOT_CM246);
        frame(f(RADIATION, cm247), SpaceMaterialItems.NUGGET_CM247, SpaceMaterialItems.BILLET_CM247, SpaceMaterialItems.INGOT_CM247);
        frame(f(RADIATION, cmrg, N, cmrg / 60f), SpaceMaterialItems.NUGGET_CM_MIX, SpaceMaterialItems.BILLET_CM_MIX, SpaceMaterialItems.INGOT_CM_MIX);
        frame(f(RADIATION, cmf, N, cmf / 120f), SpaceMaterialItems.NUGGET_CM_FUEL, SpaceMaterialItems.BILLET_CM_FUEL, SpaceMaterialItems.INGOT_CM_FUEL);
        frame(f(RADIATION, bk247), SpaceMaterialItems.NUGGET_BK247, SpaceMaterialItems.BILLET_BK247, SpaceMaterialItems.INGOT_BK247);
        frame(f(RADIATION, cf251, HOT, 2), SpaceMaterialItems.NUGGET_CF251, SpaceMaterialItems.BILLET_CF251, SpaceMaterialItems.INGOT_CF251);
        frame(f(RADIATION, cf252, N, cf252 / 5f, HOT, 3), SpaceMaterialItems.NUGGET_CF252, SpaceMaterialItems.BILLET_CF252, SpaceMaterialItems.INGOT_CF252);
        frame(f(RADIATION, es253, N, es253 / 40, HOT, 4), SpaceMaterialItems.NUGGET_ES253, SpaceMaterialItems.BILLET_ES253, SpaceMaterialItems.INGOT_ES253);
        one(ingot, f(RADIATION, es255), SpaceMaterialItems.INGOT_ES255);
        // CN989: .rad(cn989).hot(4F) nugget/billet/ingot/dust/plate
        Object[] cn = f(RADIATION, cn989, HOT, 4F);
        frame(cn, SpaceMaterialItems.NUGGET_CN989, SpaceMaterialItems.BILLET_CN989, SpaceMaterialItems.INGOT_CN989);
        one(powder, cn, SpaceMaterialItems.POWDER_CN989);
        one(plate, cn, SpaceMaterialItems.PLATE_CN989);

        // HazardRegistry.registerItems (Space)
        HazardSystem.register(SpaceMaterialItems.AMMONIUM_NITRATE.get(), new HazardData(new HazardEntry(EXPLOSIVE, 4F)));
        HazardSystem.register(SpaceMaterialItems.POWDER_WD2004.get(), new HazardData(new HazardEntry(DIGAMMA, 1F)));
        HazardSystem.register(SpaceMaterialItems.POWDER_WD2004_TINY.get(), new HazardData(new HazardEntry(DIGAMMA, 0.05F)));
        HazardSystem.register(SpaceMaterialItems.PELLET_RTG_DEPLETED_AMERICIUM.get(), new HazardData(new HazardEntry(RADIATION, amrg * rtg)));

        rbmkRod(SpaceMaterialItems.RBMK_FUEL_BK247.get(), bk247 * rod_rbmk, wst * rod_rbmk * 38F);
        rbmkPellet(SpaceMaterialItems.RBMK_PELLET_BK247.get(), bk247 * billet, wst * billet * 37F);
        rbmkPellet(SpaceMaterialItems.RBMK_PELLET_LECM.get(), cmrg * billet, wst * cf252 * 2.5F);
        rbmkPellet(SpaceMaterialItems.RBMK_PELLET_MECM.get(), cmrg * billet, wst * cf252 * 2.6F);
        rbmkPellet(SpaceMaterialItems.RBMK_PELLET_HECM.get(), cmrg * billet, wst * cf252 * 2.7F);

        // R9-B: Curium-RBMK-Staebe (HazardRegistry.registerRBMKRod) und Space-Watz-Pellets (DictFrame.fromOne(watz_pellet, typ),
        // AMF/CF251 haben im Original keinen Eintrag)
        rbmkRod(SpaceNuclearItems.RBMK_FUEL_LECM.get(), cmrg * 2.2F * rod_rbmk / 5F, wst * rod_rbmk);
        rbmkRod(SpaceNuclearItems.RBMK_FUEL_MECM.get(), cmrg * 2.2F * rod_rbmk / 3F, wst * rod_rbmk * 3F);
        rbmkRod(SpaceNuclearItems.RBMK_FUEL_HECM.get(), cmrg * 2.2F * rod_rbmk / 1.5F, wst * rod_rbmk * 5F);
        watz(SpaceNuclearItems.PU241, 25.0F * ingot * 4);
        watz(SpaceNuclearItems.AMRG, amrg * ingot * 4);
        watz(SpaceNuclearItems.CMRG, cmrg * ingot * 4);
        watz(SpaceNuclearItems.CMF, cmf * ingot * 4);
        watz(SpaceNuclearItems.BK247, bk247 * ingot * 4);
        watz(SpaceNuclearItems.CF252, cf252 * ingot * 4);
        watz(SpaceNuclearItems.ES253, es253 * ingot * 4);

        // R9-B: Neutronen an HBM-Gegenstaenden, erst nach HBMs eigener Gefahrenbelegung (HazardRegistry.registerItems
        // laeuft in HBMs commonSetup vor ModFluidTraitsBootstrap; afterHbmFluidDefaults = Ende dieses Setups)
        com.hbm_m.api.space.HbmFluidApi.afterHbmFluidDefaults(NeutronEffects::registerHbmNeutrons);
        NeutronEffects.init();
    }

    private static void watz(com.hbm_m.item.nuclear.WatzPelletType type, float rad) {
        HazardSystem.register(SpaceNuclearItems.WATZ_PELLET.get(type).get(), new HazardData(new HazardEntry(RADIATION, rad)));
    }

    /** DictFrame-Gefahren: abwechselnd Typ, Wert. */
    private static Object[] f(Object... typeLevel) {
        return typeLevel;
    }

    /** nugget/billet/ingot eines DictFrames. */
    private static void frame(Object[] haz, Supplier<? extends Item> nug, Supplier<? extends Item> bil, Supplier<? extends Item> ing) {
        one(nugget, haz, nug);
        one(billet, haz, bil);
        one(ingot, haz, ing);
    }

    private static void one(float mult, Object[] haz, Supplier<? extends Item> item) {
        List<HazardEntry> list = new ArrayList<>();
        for (int i = 0; i < haz.length; i += 2) {
            list.add(new HazardEntry((HazardTypeBase) haz[i], ((Number) haz[i + 1]).floatValue() * mult));
        }
        HazardSystem.register(item.get(), new HazardData(list.toArray(new HazardEntry[0])));
    }

    /** {@code HazardRegistry.registerRBMKRod(rod, base, dep)}: Strahlung mit RBMK-Modifikator + Hitze. */
    private static void rbmkRod(Item rod, float base, float dep) {
        HazardSystem.register(rod, new HazardData(
                new HazardEntry(RADIATION, base).addMod(new HazardModifierRBMKRadiation(dep, false)),
                new HazardEntry(HOT, 0).addMod(new HazardModifierRBMKHot())));
    }

    /** {@code HazardRegistry.registerRBMKPellet(pellet, base, dep)}: ohne Hitze. */
    private static void rbmkPellet(Item pellet, float base, float dep) {
        HazardSystem.register(pellet, new HazardData(new HazardEntry(RADIATION, base).addMod(new HazardModifierRBMKRadiation(dep, false))));
    }
}
