package com.space_m.resources;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.inventory.material.MaterialShapes;
import com.hbm_m.inventory.material.Mats;
import com.hbm_m.inventory.material.NTMMaterial;
import com.space_m.item.SpaceItems;
import com.space_m.main.SpaceCreativeTabs;
import com.space_m.material.SpaceMats;
import com.space_m.resources.ItemCelestialBedrockOre.BedrockOreGrade;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Registrierung der Space-Bedrock-Erzstufen ({@link ItemCelestialBedrockOre}, 54 Sorten x 26 Grade = 1404 Items
 * {@code space_m:bedrock_ore_<grade>_<koerper>_<sorte>}) und der Bedrock-Erz-Fragmente der Materialien, die in
 * HBM-Modernized keine Fragment-Form haben (Original: zusaetzliche {@code FRAGMENT}-Autogen-Formen in NTM Space
 * {@code Mats}, Item {@code bedrock_ore_fragment} mit Metadatum = Material-ID).
 *
 * <p>Creative-Tab 1:1 {@code getSubItems}: nur {@code BASE} im partsTab (NEI/JEI zeigt alle Stufen); die Fragmente
 * wie {@code ItemAutogen} ebenfalls im partsTab. Modelle/Texturen/Rezept-JSON erzeugt {@code scratchpad/sBO_gen.py}.
 */
public final class CelestialBedrockOreItems {

    private CelestialBedrockOreItems() {}

    private static final Map<BedrockOreGrade, List<RegistrySupplier<Item>>> ORES = new EnumMap<>(BedrockOreGrade.class);
    private static final List<RegistrySupplier<Item>> ALL = new ArrayList<>();

    /**
     * Materialien der Sortentabelle ohne HBM-Fragment ({@code hbm_m:bedrock_ore_fragment_<port>}) -> Addon-Item
     * {@code space_m:bedrock_ore_fragment_<name>}. Muss mit {@code ADDON_FRAG} in {@code sBO_gen.py} uebereinstimmen.
     */
    private static final Map<String, NTMMaterial> FRAGMENT_MATS = new LinkedHashMap<>();
    static {
        FRAGMENT_MATS.put("zinc", SpaceMats.MAT_ZINC);
        FRAGMENT_MATS.put("netherquartz", CelestialBedrockMats.MAT_QUARTZ);
        FRAGMENT_MATS.put("lapis", CelestialBedrockMats.MAT_LAPIS);
        FRAGMENT_MATS.put("nickel", SpaceMats.MAT_NICKEL);
        FRAGMENT_MATS.put("u233", Mats.MAT_U233);
        FRAGMENT_MATS.put("u235", Mats.MAT_U235);
        FRAGMENT_MATS.put("bromine", SpaceMats.MAT_BROMINE);
        FRAGMENT_MATS.put("australium", CelestialBedrockMats.MAT_AUSTRALIUM);
        FRAGMENT_MATS.put("tasmanite", CelestialBedrockMats.MAT_TASMANITE);
        FRAGMENT_MATS.put("ayerite", CelestialBedrockMats.MAT_AYERITE);
        FRAGMENT_MATS.put("glowstone", SpaceMats.MAT_GLOWSTONE);
        FRAGMENT_MATS.put("white_phosphorus", CelestialBedrockMats.MAT_PHOSPHORUS_W);
        FRAGMENT_MATS.put("cadmium", Mats.MAT_CADMIUM);
        FRAGMENT_MATS.put("gallium", SpaceMats.MAT_GALLIUM);
        FRAGMENT_MATS.put("arsenic", Mats.MAT_ARSENIC);
        FRAGMENT_MATS.put("co60", Mats.MAT_CO60);
        FRAGMENT_MATS.put("pb209", Mats.MAT_PB209);
        FRAGMENT_MATS.put("iodine", SpaceMats.MAT_IODINE);
        FRAGMENT_MATS.put("au198", Mats.MAT_AU198);
        FRAGMENT_MATS.put("plutonium", Mats.MAT_PLUTONIUM);
        FRAGMENT_MATS.put("neptunium", Mats.MAT_NEPTUNIUM);
        FRAGMENT_MATS.put("pu239", Mats.MAT_PU239);
        FRAGMENT_MATS.put("schrabidium", Mats.MAT_SCHRABIDIUM);
        FRAGMENT_MATS.put("solinium", Mats.MAT_SOLINIUM);
        FRAGMENT_MATS.put("ghiorsium", Mats.MAT_GHIORSIUM);
        FRAGMENT_MATS.put("hafnium", SpaceMats.MAT_HAFNIUM);
        FRAGMENT_MATS.put("polymer", Mats.MAT_POLYMER);
        FRAGMENT_MATS.put("rubber", Mats.MAT_RUBBER);
        FRAGMENT_MATS.put("semtex", SpaceMats.MAT_SEMTEX);
        FRAGMENT_MATS.put("pvc", Mats.MAT_PVC);
    }

    private static final Map<NTMMaterial, RegistrySupplier<Item>> FRAGMENTS = new LinkedHashMap<>();

    private static boolean initialized;

    /** Aus {@code SpaceItems.init()} (vor {@code ITEMS.register()}). */
    public static void init() {
        if (initialized) return;
        initialized = true;

        List<CelestialBedrockOre.Type> types = CelestialBedrockOre.getAllTypes();
        for (BedrockOreGrade grade : BedrockOreGrade.values()) {
            List<RegistrySupplier<Item>> list = new ArrayList<>(types.size());
            for (CelestialBedrockOre.Type type : types) {
                RegistrySupplier<Item> reg = SpaceItems.register(id(grade, type),
                        () -> new ItemCelestialBedrockOre(new Item.Properties(), grade, type),
                        grade == BedrockOreGrade.BASE ? SpaceCreativeTabs.Tab.PARTS : null);
                list.add(reg);
                ALL.add(reg);
            }
            ORES.put(grade, list);
        }

        for (Map.Entry<String, NTMMaterial> e : FRAGMENT_MATS.entrySet()) {
            NTMMaterial mat = e.getValue();
            RegistrySupplier<Item> reg = SpaceItems.register("bedrock_ore_fragment_" + e.getKey(),
                    () -> new ItemFragment(new Item.Properties(), mat), SpaceCreativeTabs.Tab.PARTS);
            FRAGMENTS.put(mat, reg);
        }
    }

    /** Registry-Pfad {@code bedrock_ore_<grade>_<koerper>_<sorte>}. */
    public static String id(BedrockOreGrade grade, CelestialBedrockOre.Type type) {
        return "bedrock_ore_" + grade.key() + "_" + type.body.name + "_" + type.suffix;
    }

    public static Item ore(BedrockOreGrade grade, CelestialBedrockOre.Type type) {
        return ORES.get(grade).get(type.index).get();
    }

    public static List<RegistrySupplier<Item>> all() {
        return Collections.unmodifiableList(ALL);
    }

    public static Map<NTMMaterial, RegistrySupplier<Item>> fragments() {
        return Collections.unmodifiableMap(FRAGMENTS);
    }

    private static boolean shapesDone;

    /**
     * Common-Setup: Fragment-Items bei HBM als {@code FRAGMENT}-Form anmelden ({@code Mats.registerShapeItem}, nur wenn
     * HBM keine eigene hat) und die Autogen-Form wie NTM Space {@code Mats} ergaenzen.
     */
    public static synchronized void registerShapes() {
        if (shapesDone) return;
        shapesDone = true;
        for (Map.Entry<NTMMaterial, RegistrySupplier<Item>> e : FRAGMENTS.entrySet()) {
            NTMMaterial mat = e.getKey();
            mat.autogen.add(MaterialShapes.FRAGMENT);
            if (Mats.getItemForShape(mat, MaterialShapes.FRAGMENT) == null) {
                Mats.registerShapeItem(mat, MaterialShapes.FRAGMENT, e.getValue());
            }
        }
    }

    /** Original {@code ItemAutogen(FRAGMENT)}: Name "%s Bedrock Ore Fragment" mit Materialname. */
    public static final class ItemFragment extends Item {
        private final NTMMaterial mat;

        public ItemFragment(Properties properties, NTMMaterial mat) {
            super(properties);
            this.mat = mat;
        }

        public NTMMaterial getMaterial() {
            return mat;
        }

        @Override
        public @NotNull Component getName(@NotNull ItemStack stack) {
            return Component.translatable("item.hbm_m.bedrock_ore_fragment", mat.getLocalizedName());
        }
    }
}
