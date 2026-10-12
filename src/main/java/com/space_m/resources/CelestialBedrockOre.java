package com.space_m.resources;

import static com.hbm_m.inventory.material.MaterialShapes.FRAGMENT;
import static com.hbm_m.inventory.material.Mats.*;
import static com.space_m.material.SpaceMats.MAT_BROMINE;
import static com.space_m.material.SpaceMats.MAT_GALLIUM;
import static com.space_m.material.SpaceMats.MAT_GLOWSTONE;
import static com.space_m.material.SpaceMats.MAT_HAFNIUM;
import static com.space_m.material.SpaceMats.MAT_IODINE;
import static com.space_m.material.SpaceMats.MAT_NICKEL;
import static com.space_m.material.SpaceMats.MAT_SEMTEX;
import static com.space_m.material.SpaceMats.MAT_ZINC;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.space.SpaceResources;
import com.hbm_m.inventory.material.Mats;
import com.hbm_m.inventory.material.NTMMaterial;
import com.hbm_m.world.gen.legacy.NoiseGeneratorPerlin;
import com.space_m.SpaceMod;
import com.space_m.celestial.CelestialBody;
import com.space_m.celestial.CelestialEnvironment;
import com.space_m.celestial.SolarSystem;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 1:1 NTM Space {@code ItemBedrockOreNew.CelestialBedrockOre}/{@code CelestialBedrockOreType} und die koerper-
 * abhaengigen Teile von {@code ItemBedrockOreBase} ({@code getOreLevel}, {@code setOreAmount}, {@code getOreBody}):
 * je Himmelskoerper 4-6 Bedrock-Erzsorten mit Haupt-, Schwefelsaeure-, Loesungsmittel- und Hochdruck-Ausbeute
 * (Menge in Fragmenten), Farben aus dem Mittel der Materialfarben, Dichte aus zwei Perlin-Rauschen je Welt-Seed +
 * Dimension.
 *
 * <p>Jede Sorte wird bei HBM ueber {@link SpaceResources#registerBedrockOre} angemeldet
 * ({@code space_m:<koerper>_<suffix>}, Gewicht 1; Kerbin unter {@code hbm_m:earth}). Die Verarbeitung (Erzschlammer,
 * Zentrifuge/Kristallisator/..., Erzdichtescanner, Bagger-{@code setOreAmount}) liegt in HBM und ist dort noch nicht
 * auf diese Tabelle umgestellt - Wunsch W-R8B-1 in {@code SPACE_API_wishes.md}. Dafuer stellt diese Klasse alle
 * Werte oeffentlich bereit ({@link #get}, {@link #getAllTypes}, {@link #byId}).
 */
public final class CelestialBedrockOre {

    /** Original {@code BedrockOreOutput}. */
    public record Output(NTMMaterial mat, int amount) {}

    /** Original {@code CelestialBedrockOreType}. */
    public static final class Type {
        public SolarSystem.Body body;
        public int index;
        public int light;
        public int dark;
        public String suffix;
        public Output primary;
        public Output byproductAcid;
        public Output byproductSolvent;
        public Output byproductRad;

        /** Kennung bei HBM ({@code space_m:<koerper>_<suffix>}). */
        public ResourceLocation id() {
            return SpaceMod.id(body.name + "_" + suffix);
        }

        public List<Output> outputs() {
            return List.of(primary, byproductAcid, byproductSolvent, byproductRad);
        }
    }

    public final Type[] types;

    private CelestialBedrockOre(Type... types) {
        this.types = types;
    }

    private static final Map<SolarSystem.Body, CelestialBedrockOre> oreMap = new EnumMap<>(SolarSystem.Body.class);
    private static final List<Type> oreTypes = new ArrayList<>();
    private static int index;

    // TODO (Original): rebalance cryolite, Laythe gets regular aluminium for now
    static {
        //					primary						sulfuric					solvent						dcm
        register(
            SolarSystem.Body.KERBIN,
            T("light",		o(MAT_IRON, 18),			o(MAT_COPPER, 9),			o(MAT_CRYOLITE, 6),			o(MAT_SODIUM, 3)),
            T("heavy",		o(MAT_TUNGSTEN, 18),		o(MAT_TUNGSTEN, 9),			o(MAT_ZINC, 6),				o(MAT_ZINC, 3)),
            T("nonmetal",	o(MAT_COAL, 18),			o(MAT_LIGNITE, 9),			o(MAT_SULFUR, 6),			o(MAT_KNO, 3)),
            T("crystal",	o(MAT_REDSTONE, 18),		o(MAT_ASBESTOS, 9),			o(MAT_DIAMOND, 6),			o(MAT_EMERALD, 3))
        );

        register(
            SolarSystem.Body.MUN,
            T("light",		o(MAT_LITHIUM, 18),			o(MAT_IRON, 9),				o(MAT_SODIUM, 6),			o(MAT_CHLOROCALCITE, 3)),
            T("heavy",		o(MAT_LEAD, 18),			o(MAT_ZINC, 9),				o(MAT_GOLD, 6),				o(MAT_BISMUTH, 3)),
            T("rare",		o(MAT_COBALT, 18),			o(MAT_RAREEARTH, 9),		o(MAT_NEODYMIUM, 6),		o(MAT_STRONTIUM, 3)),
            T("nonmetal",	o(MAT_SULFUR, 18),			o(MAT_FLUORITE, 9),			o(MAT_KNO, 6),				o(MAT_SILICON, 3)),
            T("crystal",	o(CelestialBedrockMats.MAT_QUARTZ, 18),	o(CelestialBedrockMats.MAT_LAPIS, 9),	o(MAT_EMERALD, 6),	o(MAT_CINNABAR, 3))
        );

        register(
            SolarSystem.Body.MINMUS,
            T("light",		o(MAT_COPPER, 18),			o(MAT_TITANIUM, 9),			o(MAT_CHLOROCALCITE, 6),	o(MAT_COPPER, 3)),
            T("heavy",		o(MAT_LEAD, 18),			o(MAT_GOLD, 9),				o(MAT_TUNGSTEN, 6),			o(MAT_BISMUTH, 3)),
            T("rare",		o(MAT_ZIRCONIUM, 18),		o(MAT_BORON, 9),			o(MAT_COBALT, 6),			o(MAT_STRONTIUM, 3)),
            T("nonmetal",	o(MAT_SULFUR, 18),			o(MAT_KNO, 9),				o(MAT_FLUORITE, 6),			o(MAT_SILICON, 3)),
            T("crystal",	o(MAT_EMERALD, 18),			o(CelestialBedrockMats.MAT_LAPIS, 9),	o(MAT_DIAMOND, 6),	o(MAT_EMERALD, 3))
        );

        register(
            SolarSystem.Body.DUNA,
            T("light",		o(MAT_IRON, 18),			o(MAT_NICKEL, 9),			o(MAT_TITANIUM, 6),			o(MAT_CHLOROCALCITE, 3)),
            T("heavy",		o(MAT_BERYLLIUM, 18),		o(MAT_TUNGSTEN, 9),			o(MAT_ZINC, 6),				o(MAT_BISMUTH, 3)),
            T("rare",		o(MAT_RAREEARTH, 18),		o(MAT_BORON, 9),			o(MAT_ZIRCONIUM, 6),		o(MAT_STRONTIUM, 3)),
            T("actinide",	o(MAT_THORIUM, 18),			o(MAT_RADIUM, 9),			o(MAT_POLONIUM, 6),			o(MAT_U233, 3)),
            T("nonmetal",	o(MAT_FLUORITE, 18),		o(MAT_SULFUR, 9),			o(MAT_SILICON, 6),			o(MAT_PHOSPHORUS, 3)),
            T("crystal",	o(MAT_REDSTONE, 18),		o(MAT_CINNABAR, 9),			o(MAT_DIAMOND, 6),			o(MAT_MOLYSITE, 3))
        );

        register(
            SolarSystem.Body.MOHO,
            T("light",		o(MAT_TITANIUM, 18),		o(MAT_CHLOROCALCITE, 9),	o(MAT_NICKEL, 6),			o(MAT_LITHIUM, 3)),
            T("heavy",		o(MAT_GOLD, 18),			o(MAT_ZINC, 9),				o(MAT_LEAD, 6),				o(MAT_BISMUTH, 3)),
            T("rare",		o(MAT_NEODYMIUM, 18),		o(MAT_ZIRCONIUM, 9),		o(MAT_BROMINE, 6),			o(MAT_STRONTIUM, 3)),
            T("actinide",	o(CelestialBedrockMats.MAT_AUSTRALIUM, 18),	o(CelestialBedrockMats.MAT_AUSTRALIUM, 9),	o(CelestialBedrockMats.MAT_TASMANITE, 6),	o(CelestialBedrockMats.MAT_AYERITE, 3)),
            T("nonmetal",	o(MAT_GLOWSTONE, 18),		o(MAT_PHOSPHORUS, 9),		o(MAT_SULFUR, 6),			o(CelestialBedrockMats.MAT_PHOSPHORUS_W, 3)),
            T("crystal",	o(MAT_CINNABAR, 18),		o(MAT_REDSTONE, 9),			o(CelestialBedrockMats.MAT_QUARTZ, 6),	o(MAT_MOLYSITE, 3))
        );

        register(
            SolarSystem.Body.DRES,
            T("light",		o(MAT_NICKEL, 18),			o(MAT_TITANIUM, 9),			o(MAT_CADMIUM, 6),			o(MAT_GALLIUM, 3)),
            T("heavy",		o(MAT_ZINC, 18),			o(MAT_GOLD, 9),				o(MAT_BISMUTH, 6),			o(MAT_ARSENIC, 3)),
            T("rare",		o(MAT_TANTALIUM, 18),		o(MAT_LANTHANIUM, 9),		o(MAT_NIOBIUM, 6),			o(MAT_STRONTIUM, 3)),
            T("actinide",	o(MAT_URANIUM, 18),			o(MAT_RADIUM, 9),			o(MAT_TECHNETIUM, 6),		o(MAT_U238, 3)),
            T("nonmetal",	o(MAT_SILICON, 18),			o(MAT_SILICON, 9),			o(MAT_FLUORITE, 6),			o(MAT_FLUORITE, 3)),
            T("crystal",	o(MAT_DIAMOND, 18),			o(MAT_BORAX, 9),			o(MAT_MOLYSITE, 6),			o(MAT_MOLYSITE, 3))
        );

        register(
            SolarSystem.Body.EVE,
            T("light",		o(MAT_SODIUM, 18),			o(MAT_CHLOROCALCITE, 9),	o(MAT_IRON, 6),				o(MAT_CO60, 3)),
            T("heavy",		o(MAT_TUNGSTEN, 18),		o(MAT_LEAD, 9),				o(MAT_ARSENIC, 6),			o(MAT_PB209, 3)),
            T("rare",		o(MAT_NIOBIUM, 18),			o(MAT_STRONTIUM, 9),		o(MAT_IODINE, 6),			o(MAT_AU198, 3)),
            T("actinide",	o(MAT_PLUTONIUM, 18),		o(MAT_POLONIUM, 9),			o(MAT_NEPTUNIUM, 6),		o(MAT_PU239, 3)),
            T("schrabidic",	o(MAT_SCHRABIDIUM, 18),		o(MAT_SOLINIUM, 9),			o(MAT_GHIORSIUM, 6),		o(MAT_SCHRABIDIUM, 3)),
            T("crystal",	o(MAT_SODALITE, 18),		o(MAT_MOLYSITE, 9),			o(MAT_DIAMOND, 6),			o(MAT_BORAX, 3))
        );

        register(
            SolarSystem.Body.IKE,
            T("light",		o(MAT_COPPER, 18),			o(MAT_BAUXITE, 9),			o(MAT_NICKEL, 6),			o(MAT_SODIUM, 3)),
            T("heavy",		o(MAT_LEAD, 18),			o(MAT_ZINC, 9),				o(MAT_GOLD, 6),				o(MAT_ARSENIC, 3)),
            T("rare",		o(MAT_BORON, 18),			o(MAT_NEODYMIUM, 9),		o(MAT_STRONTIUM, 6),		o(MAT_LANTHANIUM, 3)),
            T("hazard",		o(MAT_URANIUM, 18),			o(MAT_U238, 9),				o(MAT_PLUTONIUM, 6),		o(MAT_TECHNETIUM, 3))
        );

        register(
            SolarSystem.Body.LAYTHE,
            T("light",		o(MAT_ALUMINIUM, 18),		o(MAT_TITANIUM, 9),			o(MAT_GALLIUM, 6),			o(MAT_HAFNIUM, 3)),
            T("heavy",		o(MAT_BERYLLIUM, 18),		o(MAT_TUNGSTEN, 9),			o(MAT_LEAD, 6),				o(MAT_ARSENIC, 3)),
            T("rare",		o(MAT_RAREEARTH, 18),		o(MAT_NEODYMIUM, 9),		o(MAT_STRONTIUM, 6),		o(MAT_NIOBIUM, 3)),
            T("actinide",	o(MAT_URANIUM, 18),			o(MAT_THORIUM, 9),			o(MAT_POLONIUM, 6),			o(MAT_U235, 3)),
            T("nonmetal",	o(MAT_CHLOROCALCITE, 18),	o(MAT_COAL, 9),				o(MAT_FLUORITE, 6),			o(MAT_SILICON, 3)),
            T("crystal",	o(MAT_ASBESTOS, 18),		o(MAT_SODALITE, 9),			o(MAT_DIAMOND, 6),			o(MAT_SODALITE, 3))
        );
        register(
            SolarSystem.Body.TEKTO,
            T("light",		o(MAT_TITANIUM, 18),		o(MAT_COPPER, 9),			o(MAT_NICKEL, 6),			o(MAT_LITHIUM, 3)),
            T("heavy",		o(MAT_BERYLLIUM, 18),		o(MAT_LEAD, 9),				o(MAT_ZINC, 6),				o(MAT_ZINC, 3)),
            T("rare",		o(MAT_BORON, 18),			o(MAT_RAREEARTH, 9),		o(MAT_TANTALIUM, 6),		o(MAT_BISMUTH, 3)),
            T("actinide",	o(MAT_URANIUM, 18),			o(MAT_NEPTUNIUM, 9),		o(MAT_RADIUM, 6),			o(MAT_TECHNETIUM, 3)),
            T("crystal",	o(MAT_EMERALD, 18),			o(MAT_SILICON, 9),			o(MAT_MOLYSITE, 6),			o(MAT_BORAX, 3)),
            T("plastic",	o(MAT_POLYMER, 18),			o(MAT_RUBBER, 9),			o(MAT_SEMTEX, 6),			o(MAT_PVC, 3))
        );
        // THATMO im Original auskommentiert ("sit this one out buddy")
    }

    private static void register(SolarSystem.Body body, Type... types) {
        oreMap.put(body, new CelestialBedrockOre(types));
        for (Type type : types) {
            type.body = body;
            oreTypes.add(type);
        }
    }

    @Nullable
    public static CelestialBedrockOre get(@Nullable SolarSystem.Body body) {
        return body == null ? null : oreMap.get(body);
    }

    public static List<Type> getAllTypes() {
        return Collections.unmodifiableList(oreTypes);
    }

    /** Original {@code getTotalTypeCount()} (= {@code index + 1}, 1:1 inklusive der Eins zu viel). */
    public static int getTotalTypeCount() {
        return index + 1;
    }

    @Nullable
    public static Type byId(ResourceLocation id) {
        for (Type t : oreTypes) if (t.id().equals(id)) return t;
        return null;
    }

    /** Original {@code getType(meta)}: Index modulo Anzahl. */
    public static Type byIndex(int index) {
        return oreTypes.get(Math.floorMod(index, oreTypes.size()));
    }

    private static Type T(String suffix, Output primary, Output byproductAcid, Output byproductSolvent, Output byproductRad) {
        Type type = new Type();
        type.index = index++;

        type.suffix = suffix;
        type.primary = primary;
        type.byproductAcid = byproductAcid;
        type.byproductSolvent = byproductSolvent;
        type.byproductRad = byproductRad;

        // Colours are autogenerated because otherwise I'd have to manually specify like 80 different colours
        type.light = getAverageColor(false, primary, byproductAcid, byproductSolvent, byproductRad);
        type.dark = getAverageColor(true, primary, byproductAcid, byproductSolvent, byproductRad);

        return type;
    }

    private static Output o(NTMMaterial mat, int amount) {
        return new Output(mat, amount);
    }

    private static int getAverageColor(boolean dark, Output... outputs) {
        int r = 0, g = 0, b = 0;

        for (Output output : outputs) {
            int color = dark ? output.mat().solidColorDark : output.mat().solidColorLight;
            r += color >> 16 & 255;
            g += color >> 8 & 255;
            b += color & 255;
        }

        r /= outputs.length;
        g /= outputs.length;
        b /= outputs.length;

        return r << 16 | g << 8 | b;
    }

    // ── ItemBedrockOreBase (Space-Teil) ──

    /** Koerper-Enum eines Levels; Thatmo (ohne Enum) wie im Original ueber den Kerbin-Rueckfall. */
    public static SolarSystem.Body bodyOf(Level level) {
        SolarSystem.Body body = CelestialBody.getEnum(level);
        return body == null || oreMap.get(body) == null ? SolarSystem.Body.KERBIN : body;
    }

    private static final Map<Long, NoiseGeneratorPerlin> generators = Collections.synchronizedMap(new HashMap<>());

    private static NoiseGeneratorPerlin getGenerator(long seed) {
        return generators.computeIfAbsent(seed, key -> new NoiseGeneratorPerlin(key, 4));
    }

    /** Original {@code ItemBedrockOreBase.getOreLevel(world, x, z, type)}: Seed = Welt-Seed + Dimensions-ID. */
    public static double getOreLevel(long worldSeed, int legacyDimensionId, int x, int z, Type type) {
        long seed = worldSeed + legacyDimensionId;

        NoiseGeneratorPerlin level = getGenerator(seed);
        NoiseGeneratorPerlin ore = getGenerator(seed - 4096 + type.index);

        double scale = 0.01D;

        return Mth.clamp(Math.abs(level.getValue(x * scale, z * scale) * ore.getValue(x * scale, z * scale)) * 0.05, 0, 2);
    }

    public static double getOreLevel(ServerLevel world, int x, int z, Type type) {
        return getOreLevel(world.getSeed(), legacyDimensionId(world), x, z, type);
    }

    /** Original {@code world.provider.dimensionId}; Kerbin/Overworld = 0. */
    public static int legacyDimensionId(Level world) {
        CelestialBody body = CelestialBody.getBodyOrNull(world.dimension());
        return body != null ? body.legacyDimensionId : 0;
    }

    /** Durchschnittsdichte aller Sorten des Koerpers (Original {@code BedrockOre.generateAuto}, Erzdichtescanner). */
    public static double getTotalLevel(ServerLevel world, int x, int z) {
        CelestialBedrockOre ore = get(bodyOf(world));
        double totalLevel = 0;
        for (Type type : ore.types) totalLevel += getOreLevel(world, x, z, type);
        return totalLevel / ore.types.length;
    }

    /** Original {@code getOreAmount(stack, type)}: ohne NBT 1. */
    public static double getOreAmount(ItemStack stack, Type type) {
        CompoundTag data = tag(stack, false);
        if (data == null) return 1;
        return data.getDouble(type.suffix);
    }

    /** Original {@code getOreBody(stack)} (Metadatum = Koerper; hier NBT {@code body}). */
    public static SolarSystem.Body getOreBody(ItemStack stack) {
        CompoundTag data = tag(stack, false);
        int ordinal = data != null ? data.getInt("body") : 0;
        if (ordinal >= SolarSystem.Body.values().length || ordinal <= 0) return SolarSystem.Body.KERBIN;
        return SolarSystem.Body.values()[ordinal];
    }

    /** Original {@code setOreAmount(world, stack, x, z, mult)}. */
    public static void setOreAmount(ServerLevel world, ItemStack stack, int x, int z, double mult) {
        CompoundTag data = tag(stack, true);
        SolarSystem.Body body = bodyOf(world);

        data.putInt("body", body.ordinal());

        for (Type type : get(body).types) {
            data.putDouble(type.suffix, getOreLevel(world, x, z, type) * mult);
        }
        write(stack, data);
    }

    // Stack-NBT versionsneutral (1.20: Tag, 1.21: custom_data)
    @Nullable
    private static CompoundTag tag(ItemStack stack, boolean create) {
        //? if < 1.21.1 {
        return create ? stack.getOrCreateTag() : stack.getTag();
        //?} else {
        /*net.minecraft.world.item.component.CustomData cd = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (cd == null) return create ? new CompoundTag() : null;
        return cd.copyTag();
        *///?}
    }

    private static void write(ItemStack stack, CompoundTag data) {
        //? if >= 1.21.1 {
        /*stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(data));
        *///?}
    }

    // ── Anmeldung bei HBM ──

    private static boolean registered;

    /** Jede Sorte bei {@link SpaceResources} (Common-Setup). */
    public static synchronized void registerWithHbm() {
        if (registered) return;
        registered = true;
        for (Type type : oreTypes) {
            CelestialBody body = CelestialBody.getBodyOrNull(type.body.name);
            ResourceLocation bodyId = body != null ? CelestialEnvironment.bodyId(body) : SpaceMod.id(type.body.name);
            SpaceResources.registerBedrockOre(bodyId, type.id(), 1);
        }
    }

    /** Fragment-Item eines Ausgangsmaterials (Original {@code extract}: Fragment-Form, sonst Bedrock-Erzsplitter). */
    @Nullable
    public static net.minecraft.world.item.Item fragment(Output o) {
        return Mats.getItemForShape(o.mat(), FRAGMENT);
    }
}
