package com.space_m.fork;

import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.space.ForkRules;
import com.hbm_m.api.space.SpaceApi;
import com.hbm_m.inventory.material.NTMMaterial;
import com.hbm_m.item.nuclear.WatzPelletType;
import com.space_m.config.SpaceConfig;
import com.space_m.fluid.SpaceFluids;
import com.space_m.item.SpaceMaterialItems;
import com.space_m.material.SpaceMats;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

/**
 * Nutzerentscheidung E1-E17 (2026-10-12): Im Addon gilt alles 1:1 wie NTM Space - auch die Fork-Abweichungen, die
 * das Spiel auf der Erde veraendern. HBM-Modernized fragt sie ueber {@link ForkRules} ab ({@code SpaceApi.fork()});
 * ohne Addon bleibt HBM beim Master.
 *
 * <ul>
 *   <li>E1 {@code GeneralConfig.enableUnavoidableGravity} (Addon-Config "1.45_enableUnavoidableGravity").</li>
 *   <li>E4 {@code TileEntityDiFurnace.getItemPower}: {@code powder_coke} = 400 (Kuehlmittel-BOILER: {@code SpaceFluids}).</li>
 *   <li>E6 {@code ItemWatzPellet.EnumWatzType.yield = 1_000_000_000} fuer <b>alle</b> Pellets (auch HBMs).</li>
 *   <li>E8 Rotguss aus Kupfer + Zink ({@link SpaceMats#MAT_ZINC}).</li>
 *   <li>E11 Gwen-Wackelkopf sprengt den Schredder.</li>
 *   <li>E16 Teleporter-Tank N-Masse ({@link SpaceFluids#NMASS}).</li>
 * </ul>
 * Alle uebrigen Punkte (E2, E3, E5, E7, E8, E9, E11, E14, E15, E17) schaltet {@link #active()} in HBM ein.
 */
public final class SpaceForkRules implements ForkRules {

    public static final SpaceForkRules INSTANCE = new SpaceForkRules();

    /** E11: wird vom Wackelkopf-Paket gesetzt (Gwen-Wackelkopf erkennen). */
    public static volatile Predicate<ItemStack> gwenBobble = stack -> false;

    private SpaceForkRules() {}

    /** Im Mod-Konstruktor, vor der Blockregistrierung (Elektrolyseur-Struktur haengt davon ab). */
    public static void init() {
        SpaceApi.setForkRules(INSTANCE);
        // E6: Ertrag aller Watz-Pellets 500 Mio -> 1 Mrd (Space ItemWatzPellet.EnumWatzType.yield)
        for (WatzPelletType t : WatzPelletType.all()) t.yield = 1_000_000_000D;
    }

    @Override
    public boolean active() {
        return true;
    }

    @Override
    public boolean unavoidableGravity() {
        return SpaceConfig.get().enableUnavoidableGravity;
    }

    @Override
    public int blastFurnaceFuel(ItemStack stack) {
        if (stack.is(SpaceMaterialItems.POWDER_COKE_COAL.get()) || stack.is(SpaceMaterialItems.POWDER_COKE_LIGNITE.get())
                || stack.is(SpaceMaterialItems.POWDER_COKE_PETROLEUM.get())) return 400;
        return 0;
    }

    @Nullable
    @Override
    public NTMMaterial gunmetalZinc() {
        return SpaceMats.MAT_ZINC;
    }

    @Override
    public boolean shredderExplodes(ItemStack stack) {
        return gwenBobble.test(stack);
    }

    @Nullable
    @Override
    public Fluid teleporterFluid() {
        return SpaceFluids.NMASS.fluid();
    }
}
