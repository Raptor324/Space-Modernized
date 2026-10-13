package com.space_m.fork.trinket;

import java.util.List;

import com.hbm_m.block.decorations.TrinketTypes;
import com.hbm_m.hazard.HazardEntry;
import com.hbm_m.hazard.HazardRegistry;
import com.hbm_m.hazard.HazardSystem;
import com.hbm_m.hazard.transformer.HazardTransformerBase;
import com.hbm_m.item.TrinketBlockItem;
import com.space_m.item.HazardTypeGlitch;

import net.minecraft.world.item.ItemStack;

/**
 * NTM Space {@code HazardRegistry.registerItems}: Gefahren an einzelnen Wackelkopf-Metadaten (Space-Reihenfolge von
 * {@code BlockBobble.BobbleType}):
 * <ul>
 *   <li>{@code bobblehead} Meta 22 = JUICE -> {@code ASBESTOS 1} (Addon-Wackelkopf {@code space_m:bobblehead}, Typ JUICE)</li>
 *   <li>{@code bobblehead} Meta 25 = MICROWAVE -> {@code GLITCH 1} (HBM-Wackelkopf {@code hbm_m:bobblehead}, Typ MICROWAVE)</li>
 * </ul>
 * Der Typ steht in HBM-Modernized im Stapel-NBT ({@link TrinketBlockItem#getType}), eine Gegenstandsregel je Typ gibt es
 * nicht - deshalb ein stapelabhaengiger {@link HazardTransformerBase} in {@link HazardSystem#trafos} (Original
 * {@code HazardSystem.register(ItemStack, data)} = Regel je Metadatum, zusaetzlich zu den Gegenstandsregeln).
 */
public final class ForkTrinketHazards extends HazardTransformerBase {

    private ForkTrinketHazards() {}

    private static boolean registered;

    /** Aus {@code SpaceRegistry.init()}: Transformer einmalig anhaengen (Gegenstaende werden erst beim Pruefen aufgeloest). */
    public static synchronized void init() {
        if (registered) return;
        registered = true;
        HazardSystem.trafos.add(new ForkTrinketHazards());
    }

    @Override
    public void transformPre(ItemStack stack, List<HazardEntry> entries) { }

    @Override
    public void transformPost(ItemStack stack, List<HazardEntry> entries) {
        if (stack.isEmpty()) return;
        if (stack.getItem() == com.hbm_m.block.ModBlocks.BOBBLEHEAD.get().asItem()) {
            if (TrinketBlockItem.getType(stack) == TrinketTypes.BobbleType.MICROWAVE.ordinal()) {
                entries.add(new HazardEntry(HazardTypeGlitch.GLITCH, 1F));
            }
        } else if (ForkTrinkets.BOBBLEHEAD.isPresent() && stack.getItem() == ForkTrinkets.BOBBLEHEAD.get().asItem()) {
            if (TrinketBlockItem.getType(stack) == ForkTrinkets.ForkBobbleType.JUICE.ordinal()) {
                entries.add(new HazardEntry(HazardRegistry.ASBESTOS, 1F));
            }
        }
    }
}
