package com.space_m.resources;

import static com.hbm_m.inventory.material.MaterialShapes.BLOCK;
import static com.hbm_m.inventory.material.MaterialShapes.DUST;
import static com.hbm_m.inventory.material.MaterialShapes.FRAGMENT;
import static com.hbm_m.inventory.material.MaterialShapes.GEM;

import com.hbm_m.inventory.material.Mats;
import com.hbm_m.inventory.material.NTMMaterial;

/**
 * Die Materialien der Space-Bedrock-Erztabelle ({@link CelestialBedrockOre}), die weder HBM-Modernized noch
 * {@code SpaceMats} (R4-B) kennen. IDs, Ore-Dictionary-Namen ({@code OreDictManager.DictFrame}), Farben und
 * Autogen-Formen 1:1 NTM Space {@code Mats}:
 * {@code MAT_LAPIS} 2701, {@code MAT_QUARTZ} 1402 ("NetherQuartz"), {@code MAT_AUSTRALIUM} 13800,
 * {@code MAT_TASMANITE} 13895, {@code MAT_AYERITE} 13851, {@code MAT_PHOSPHORUS_W} 1501 ("WhitePhosphorus").
 * Die Autogen-Fragmente selbst (Bedrock-Erz-Splitter) erzeugt HBM bei der Bedrock-Verarbeitung (W-R8B-1).
 */
public final class CelestialBedrockMats {

    private CelestialBedrockMats() {}

    private static String[] n(String... names) { return names; }

    public static final NTMMaterial MAT_LAPIS = Mats.makeNonSmeltable(2701, n("Lapis"), 0x567DF9, 0x001793, 0x264DC9).setAutogen(FRAGMENT, DUST, GEM, BLOCK).m();
    public static final NTMMaterial MAT_QUARTZ = Mats.makeNonSmeltable(1402, n("NetherQuartz"), 0xF7F5F2, 0x6F5D5A, 0xF7F5F2).setAutogen(FRAGMENT).n();
    public static final NTMMaterial MAT_AUSTRALIUM = Mats.makeSmeltable(13800, n("Australium"), 0xFFFF00, 0x935B00, 0xFFFF00).setAutogen(FRAGMENT).m();
    public static final NTMMaterial MAT_TASMANITE = Mats.makeSmeltable(13895, n("Tasmanite"), 0xFFFF00, 0x935B00, 0xFFFF00).setAutogen(FRAGMENT).m();
    public static final NTMMaterial MAT_AYERITE = Mats.makeSmeltable(13851, n("Ayerite"), 0xFFFF00, 0x935B00, 0xFFFF00).setAutogen(FRAGMENT).m();
    public static final NTMMaterial MAT_PHOSPHORUS_W = Mats.makeNonSmeltable(1501, n("WhitePhosphorus"), 0xF5F5ED, 0xC4BD9A, 0xC4BD9A).setAutogen(FRAGMENT, DUST).n();

    /** Laedt die Klasse (Materialien beim Mod-Start anmelden). */
    public static void init() {}
}
