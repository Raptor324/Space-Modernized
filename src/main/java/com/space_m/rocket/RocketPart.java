package com.space_m.rocket;

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * Ein Raketenteil, 1:1 NTM Space {@code MissilePart} (Hoehe, GUI-Hoehe, Modell, Textur, ausgefahrenes Modell, Verkleidung,
 * Teil-Renderer) zusammen mit den Space-Feldern von {@code ItemCustomMissilePart} (Typ, Anschlussgroessen, Treibstoff bzw.
 * Nutzlasttyp, Tankgroesse, Masse, Schub, ISP).
 *
 * <p>Abweichung (technisch noetig): HBM-Modernized fuehrt Raptors {@code ItemCustomMissilePart} im Master-Stand ohne Masse,
 * Schub und ISP; dessen Code wird nicht angefasst. Die Space-Werte stehen deshalb hier, gebunden an die Registry-ID des Items
 * ({@code hbm_m:mp_*}, {@code hbm_m:sat_*} bzw. die Space-neuen {@code space_m:*}). Eintraege, deren Item (noch) nicht
 * registriert ist (Space-Satelliten aus R6), bleiben bis dahin unaufgeloest. Die Teile-Tabelle steht in {@link RocketParts}.
 *
 * <p>Original {@code MissilePart.getId/getPart(int)} nutzen die numerische Item-ID; hier nur fuer Netzwerk/Entity-Sync
 * (innerhalb einer Sitzung stabil). Im NBT speichert {@link RocketStruct} die Registry-Namen.
 * Original-Klasse: MissilePart
 */
public final class RocketPart {

    /** Original {@code ItemCustomMissilePart.PartType}. */
    public enum PartType { CHIP, WARHEAD, FUSELAGE, FINS, THRUSTER }

    /** Original {@code ItemCustomMissilePart.PartSize} (Space: mit Radius, Groessen 2.5/3.0). */
    public enum PartSize {
        ANY, NONE, SIZE_10(1.0), SIZE_15(1.5), SIZE_20(2.0), SIZE_25(2.5), SIZE_30(3.0);

        public final double radius;

        PartSize() { this.radius = 0; }

        PartSize(double radius) { this.radius = radius; }
    }

    /** Nutzlasttyp; die Raketenlogik unterscheidet nur Kapsel ({@code APOLLO}), Satellit/Station und alles andere. */
    public enum WarheadType { OTHER, APOLLO, SATELLITE }

    /** Original {@code ItemCustomMissilePart.FuelType} (Space). */
    public enum FuelType {
        ANY("item.custom_missile_part.fuel.any"),
        KEROSENE("item.custom_missile_part.fuel.kerosene"),
        SOLID("item.custom_missile_part.fuel.solid"),
        HYDROGEN("item.custom_missile_part.fuel.hydrogen"),
        XENON("item.custom_missile_part.fuel.xenon"),
        BALEFIRE("item.custom_missile_part.fuel.balefire"),
        HYDRAZINE("item.custom_missile_part.fuel.hydrazine"),
        METHALOX("item.custom_missile_part.fuel.methalox"),
        KEROLOX("item.custom_missile_part.fuel.kerolox");

        public final String unlocalizedName;

        FuelType(String unlocalizedName) { this.unlocalizedName = unlocalizedName; }
    }

    private static final Map<ResourceLocation, RocketPart> BY_ID = new LinkedHashMap<>();
    private static final Map<Item, RocketPart> BY_ITEM = new IdentityHashMap<>();
    private static boolean defined;

    public final ResourceLocation id;
    /** Original {@code MissilePart.part}; null bis das Item registriert ist. */
    @Nullable public Item part;
    public final PartType type;
    public final PartSize top;
    public final PartSize bottom;
    /** Nur Triebwerke/Rumpfe. */
    @Nullable public final FuelType fuel;
    /** Nur Nutzlasten. */
    @Nullable public final WarheadType warhead;
    public final int tankSize;
    public final int mass;
    public final int thrust;
    public final int isp;
    public final double height;
    public final double guiheight;
    /** Client-Darstellung ("ns:pfad"). */
    public final String model;
    public final String texture;
    @Nullable public final String deployedModel;
    @Nullable public final String shroudModel;
    public final boolean renderInventoryModel;
    /** Original {@code withRenderer(...)}: Schluessel fuer {@code RocketPronter.registerPartRenderer}. */
    @Nullable public final String rendererId;

    private RocketPart(ResourceLocation id, PartType type, PartSize top, PartSize bottom, @Nullable FuelType fuel,
                       @Nullable WarheadType warhead, int tankSize, int mass, int thrust, int isp, double height, double guiheight,
                       String model, String texture, @Nullable String deployedModel, @Nullable String shroudModel,
                       boolean renderInventoryModel, @Nullable String rendererId) {
        this.id = id;
        this.type = type;
        this.top = top;
        this.bottom = bottom;
        this.fuel = fuel;
        this.warhead = warhead;
        this.tankSize = tankSize;
        this.mass = mass;
        this.thrust = thrust;
        this.isp = isp;
        this.height = height;
        this.guiheight = guiheight;
        this.model = model;
        this.texture = texture;
        this.deployedModel = deployedModel;
        this.shroudModel = shroudModel;
        this.renderInventoryModel = renderInventoryModel;
        this.rendererId = rendererId;
    }

    static void define(String id, PartType type, PartSize top, PartSize bottom, @Nullable Object kind, int tank, int mass, int thrust,
                       int isp, double height, double guiheight, String model, String texture, @Nullable String deployed,
                       @Nullable String shroud, boolean hideInventoryModel, @Nullable String renderer) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null || BY_ID.containsKey(rl)) return; // Original registriert mp_thruster_10_hydrazine doppelt
        BY_ID.put(rl, new RocketPart(rl, type, top, bottom, kind instanceof FuelType f ? f : null,
                kind instanceof WarheadType w ? w : null, tank, mass, thrust, isp, height, guiheight, model, texture, deployed, shroud,
                !hideInventoryModel, renderer));
    }

    private static void ensureDefined() {
        if (defined) return;
        defined = true;
        RocketParts.registerAll();
    }

    /** Bindet die Tabelle an die Items (beliebig oft; nicht registrierte IDs werden spaeter nachgezogen). */
    public static synchronized void resolve() {
        ensureDefined();
        for (RocketPart p : BY_ID.values()) {
            if (p.part != null) continue;
            if (!BuiltInRegistries.ITEM.containsKey(p.id)) continue;
            Item item = BuiltInRegistries.ITEM.get(p.id);
            if (item == null || item == Items.AIR) continue;
            p.part = item;
            BY_ITEM.put(item, p);
        }
    }

    private static boolean resolvedOnce;

    private static void lazyResolve() {
        if (!resolvedOnce) {
            resolvedOnce = true;
            resolve();
        }
    }

    // ── Original MissilePart.getPart / getId ─────────────────────────────────────────────────

    @Nullable
    public static RocketPart getPart(@Nullable ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        return getPart(stack.getItem());
    }

    @Nullable
    public static RocketPart getPart(@Nullable Item item) {
        if (item == null) return null;
        lazyResolve();
        RocketPart p = BY_ITEM.get(item);
        if (p == null) {
            // spaet registrierte Items (andere Runden) nachziehen
            ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
            RocketPart def = BY_ID.get(key);
            if (def != null && def.part == null) {
                resolve();
                p = BY_ITEM.get(item);
            }
        }
        return p;
    }

    /** Netzwerk/Entity-Sync: numerische Item-ID, 0 = kein Teil (Original {@code getPart(int)}, {@code id <= 0 -> null}). */
    @Nullable
    public static RocketPart getPart(int id) {
        if (id <= 0) return null;
        return getPart(BuiltInRegistries.ITEM.byId(id));
    }

    public static int getId(@Nullable RocketPart m) {
        if (m == null || m.part == null) return 0;
        return BuiltInRegistries.ITEM.getId(m.part);
    }

    /** NBT: Registry-Name (leer = kein Teil). */
    public static String getName(@Nullable RocketPart m) {
        return m == null ? "" : m.id.toString();
    }

    @Nullable
    public static RocketPart getPart(@Nullable String name) {
        if (name == null || name.isEmpty()) return null;
        ResourceLocation rl = ResourceLocation.tryParse(name);
        if (rl == null) return null;
        lazyResolve();
        RocketPart p = BY_ID.get(rl);
        if (p != null && p.part == null) resolve();
        return p;
    }

    /** Definition nach Registry-ID (auch wenn das Item noch fehlt). */
    @Nullable
    public static RocketPart byId(ResourceLocation id) {
        ensureDefined();
        return BY_ID.get(id);
    }

    public static Collection<RocketPart> all() {
        ensureDefined();
        return Collections.unmodifiableCollection(BY_ID.values());
    }

    // ── Space-Felder von ItemCustomMissilePart ────────────────────────────────────────────────

    /** Original {@code getFuel()}: Festbrennstoff = {@link Fluids#EMPTY} (Original {@code Fluids.NONE}), unbekannt = null. */
    @Nullable
    public Fluid getFuel() {
        if (fuel == null) return null;
        return switch (fuel) {
            case KEROSENE, KEROLOX -> ModFluids.KEROSENE.getSource();
            case METHALOX -> ModFluids.GAS.getSource();
            case HYDROGEN -> ModFluids.HYDROGEN.getSource();
            case XENON -> ModFluids.XENON.getSource();
            case BALEFIRE -> ModFluids.BALEFIRE.getSource();
            case HYDRAZINE -> fluidByName("hydrazine");
            case SOLID -> Fluids.EMPTY; // Requires non-fluid fuel
            default -> null;
        };
    }

    /** Original {@code getOxidizer()}. */
    @Nullable
    public Fluid getOxidizer() {
        if (fuel == null) return null;
        return switch (fuel) {
            case KEROLOX, HYDROGEN, METHALOX -> ModFluids.OXYGEN.getSource();
            case KEROSENE, BALEFIRE -> ModFluids.PEROXIDE.getSource();
            default -> null;
        };
    }

    @Nullable
    public FluidType getFuelType() {
        Fluid f = getFuel();
        return f == null || f == Fluids.EMPTY ? null : FluidType.forFluid(f);
    }

    @Nullable
    public FluidType getOxidizerType() {
        Fluid f = getOxidizer();
        return f == null ? null : FluidType.forFluid(f);
    }

    @Nullable
    private static Fluid fluidByName(String name) {
        ModFluids.FluidEntry e = ModFluids.getEntry(name);
        return e != null ? e.getSource() : null;
    }

    /** Original {@code getThrust()}: nur Triebwerke. */
    public int getThrust() {
        return type == PartType.THRUSTER ? thrust : 0;
    }

    /** Original {@code getISP()}. */
    public int getISP() {
        return type == PartType.THRUSTER ? isp : 0;
    }

    /** Original {@code getTankSize()}: nur Rumpfe. */
    public int getTankSize() {
        return type == PartType.FUSELAGE ? tankSize : 0;
    }

    /** Original {@code attributes[0] == WarheadType.APOLLO || SATELLITE}. */
    public boolean isCapsuleOrSatellite() {
        return type == PartType.WARHEAD && (warhead == WarheadType.APOLLO || warhead == WarheadType.SATELLITE);
    }

    /** Original {@code getModel(boolean deployed)}. */
    public String getModel(boolean deployed) {
        if (deployed && deployedModel != null) return deployedModel;
        return model;
    }

    /** Original {@code getShroud()}. */
    public String getShroud() {
        return shroudModel != null ? shroudModel : model;
    }

    @Override
    public String toString() {
        return "RocketPart[" + id + "]";
    }
}
