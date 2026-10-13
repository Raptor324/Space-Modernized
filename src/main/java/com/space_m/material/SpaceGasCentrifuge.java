package com.space_m.material;

import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;
import com.hbm_m.recipe.PseudoFluidType;
import com.space_m.fluid.SpaceFluids;
import com.space_m.item.SpaceMaterialItems;

import dev.architectury.event.events.common.LifecycleEvent;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 NTM Space {@code GasCentrifugeRecipes}: Mineralschlamm in der Gaszentrifuge (W-R9B5-5).
 * <pre>
 * MINSOLSEP = new PseudoFluidType("MINSOLSEP", 500, 0,   NONE,      false, crystal_cleaned)
 * MINSOL    = new PseudoFluidType("MINSOL",    500, 500, MINSOLSEP, false, powder_iron)
 * fluidConversions.put(Fluids.MINSOL, PseudoFluidType.MINSOL)
 * gasCent.put(1000 MINSOL, {powder_iron, crystal_cleaned}, false, 2)   // JEI: data/space_m/recipes/r9b5/gas_centrifuge/minsol.json
 * </pre>
 * Anzeige-Namen {@code pseudofluid.hbm_m.minsol/minsolsep} liefert {@code SpaceLang}.
 */
public final class SpaceGasCentrifuge {

    private SpaceGasCentrifuge() {}

    public static PseudoFluidType MINSOLSEP;
    public static PseudoFluidType MINSOL;

    /** Aus {@code SpaceRegistry.init()}; die Typen brauchen Gegenstaende und entstehen daher im Common-Setup. */
    public static void init() {
        LifecycleEvent.SETUP.register(SpaceGasCentrifuge::register);
    }

    static synchronized void register() {
        if (MINSOL != null) return;
        MINSOLSEP = PseudoFluidType.register("minsolsep", 500, 0, PseudoFluidType.NONE, false,
                new ItemStack(SpaceMaterialItems.CRYSTAL_CLEANED.get(), 1));
        MINSOL = PseudoFluidType.register("minsol", 500, 500, MINSOLSEP, false,
                new ItemStack(ModMaterialItems.item(ModMaterials.IRON, MaterialShape.POWDER), 1));
        PseudoFluidType.FLUID_CONVERSIONS.put(SpaceFluids.MINSOL.fluid(), MINSOL);
    }
}
