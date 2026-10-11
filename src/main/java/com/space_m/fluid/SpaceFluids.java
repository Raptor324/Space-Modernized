package com.space_m.fluid;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.HbmFluidRegistry;
import com.hbm_m.api.fluids.bootstrap.ModFluidPollutionPresets;
import com.hbm_m.api.space.HbmFluidApi;
import com.hbm_m.inventory.FluidContainerRegistry;
import com.hbm_m.inventory.fluid.FluidContainerDefs;
import com.hbm_m.inventory.fluid.FluidHazardSymbol;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.ModFluids.FluidEntry;
import com.hbm_m.inventory.fluid.trait.FT_Combustible;
import com.hbm_m.inventory.fluid.trait.FT_Combustible.FuelGrade;
import com.hbm_m.inventory.fluid.trait.FT_Coolable;
import com.hbm_m.inventory.fluid.trait.FT_Corrosive;
import com.hbm_m.inventory.fluid.trait.FT_Flammable;
import com.hbm_m.inventory.fluid.trait.FT_Heatable;
import com.hbm_m.inventory.fluid.trait.FT_Heatable.HeatingType;
import com.hbm_m.inventory.fluid.trait.FT_PWRModerator;
import com.hbm_m.inventory.fluid.trait.FT_Pheromone;
import com.hbm_m.inventory.fluid.trait.FT_Poison;
import com.hbm_m.inventory.fluid.trait.FT_Polluting;
import com.hbm_m.inventory.fluid.trait.FT_VentRadiation;
import com.hbm_m.inventory.fluid.trait.FluidTrait;
import com.hbm_m.inventory.fluid.trait.FluidTraitSimple.FT_Delicious;
import com.hbm_m.inventory.fluid.trait.FluidTraitSimple.FT_Gaseous;
import com.hbm_m.inventory.fluid.trait.FluidTraitSimple.FT_Gaseous_ART;
import com.hbm_m.inventory.fluid.trait.FluidTraitSimple.FT_Liquid;
import com.hbm_m.inventory.fluid.trait.FluidTraitSimple.FT_NoContainer;
import com.hbm_m.inventory.fluid.trait.FluidTraitSimple.FT_NoID;
import com.hbm_m.inventory.fluid.trait.FluidTraitSimple.FT_Viscous;
import com.hbm_m.inventory.fluid.trait.PollutionType;
import com.space_m.SpaceMod;
import com.space_m.fluid.trait.FT_Rocket;
import com.space_m.fluid.trait.SpaceTraitsSimple;
import com.space_m.machine.atmo.FT_Terraformer;
import com.space_m.main.SpaceRegistry;

import dev.architectury.registry.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;

/**
 * Die 59 neuen Fluide von NTM Space ({@code com.hbm.inventory.fluid.Fluids}, Space-Zusaetze) als HBM-Fluide ueber
 * {@link HbmFluidApi}: Registry-ID {@code space_m:<name>} (Name = Original-{@code FluidType}-Name klein, z.B.
 * {@code KMnO4 -> kmno4}), Farbe = Rohr-/Textfarbe, Gefahrenraute, Temperatur, Traits, Kanister/Gasflaschen und
 * Einsortierung in die Fluid-Identifier-Reihenfolge ({@code Fluids.metaOrder} des Space-Zweigs).
 *
 * <p>Von den 62 im Original deklarierten Feldern sind drei keine eigenen Space-Fluide:
 * {@code "earthair"} (EARTHAIR) ist HBM-Modernized {@code hbm_m:air} (Entscheidung R1-A, {@code CelestialFluids.EARTHAIR});
 * {@code "hyaluronic"} und {@code "laytheair"} sind im Original auskommentiert ({@code //public static FluidType ...})
 * und existieren dort nicht. Siehe {@link #NOT_REGISTERED}.
 *
 * <p>Ablauf: Registrierung + Reihenfolge im Mod-Konstruktor ({@link #init()}); Temperatur/Traits/Gefahrenraute,
 * Treibstoff-Tabellenwerte ({@code registerCalculatedFuel}), Heiz-/Kuehlketten, {@code FT_Rocket} an HBM-Fluiden,
 * Behaelter und Weltfluid-Eimer in {@link SpaceFluidsSetup} ({@code FMLLoadCompleteEvent}) - sicher <b>nach</b> HBMs
 * {@code ModFluidTraitsBootstrap} (SETUP), damit HBMs geschaetzte Gefahrenraute die Originalwerte nicht ueberschreibt.
 * Original-Klasse: Fluids (Space-Zusaetze)
 */
public final class SpaceFluids {
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(SpaceMod.MOD_ID, Registries.FLUID);

    /** Art des Original-FluidType (bestimmt Dichte/Viskositaet wie {@code ModFluids.registerFluid/Gas/HeavyFluid/Plasma}). */
    public enum Kind { LIQUID, GAS, HEAVY, PLASMA }

    /**
     * Felder des Originals ohne eigenes Space-Fluid: EARTHAIR = {@code hbm_m:air}; HYALURONIC/LAYTHEAIR im Original
     * auskommentiert.
     */
    public static final List<String> NOT_REGISTERED = List.of("earthair", "hyaluronic", "laytheair");

    /** Definition eines Space-Fluids (Original {@code new FluidType(name, color, p, f, r, symbol)...}). */
    public static final class Def {
        public final String name;
        public final int color;
        public final Kind kind;
        public final FluidEntry entry;
        int poison, flammability, reactivity;
        FluidHazardSymbol symbol = FluidHazardSymbol.NONE;
        @Nullable Integer temp;
        final List<FluidTrait> traits = new ArrayList<>();
        int canister = -1;
        int gasBottle = -1, gasLabel = -1;

        Def(String name, int color, Kind kind, FluidEntry entry) {
            this.name = name;
            this.color = color;
            this.kind = kind;
            this.entry = entry;
        }

        Def nfpa(int p, int f, int r, FluidHazardSymbol s) {
            poison = p; flammability = f; reactivity = r; symbol = s;
            return this;
        }

        Def temp(int t) { temp = t; return this; }

        Def traits(FluidTrait... t) { Collections.addAll(traits, t); return this; }

        /** Original {@code addContainers(new CD_Canister(color))}. */
        Def canister(int c) { canister = c; return this; }

        /** Original {@code addContainers(new CD_Gastank(bottle, label))}. */
        Def gastank(int bottle, int label) { gasBottle = bottle; gasLabel = label; return this; }

        public Fluid fluid() { return entry.getSource(); }

        public ResourceLocation id() { return SpaceMod.id(name); }
    }

    private static final List<Def> DEFS = new ArrayList<>();

    // Original-Konstanten (Fluids.LIQUID/GASEOUS/...): Traits sind zustandslos und duerfen geteilt werden
    private static final FT_Liquid LIQUID = new FT_Liquid();
    private static final FT_Gaseous GASEOUS = new FT_Gaseous();
    private static final FT_Gaseous_ART EVAP = new FT_Gaseous_ART();
    private static final FT_Viscous VISCOUS = new FT_Viscous();
    private static final FT_Delicious DELICIOUS = new FT_Delicious();
    private static final FT_NoContainer NOCON = new FT_NoContainer();
    private static final FT_NoID NOID = new FT_NoID();
    private static final FluidHazardSymbol NONE = FluidHazardSymbol.NONE;

    private static Def def(Kind kind, String name, int color, @Nullable String anchor) {
        FluidEntry e = switch (kind) {
            case GAS -> HbmFluidApi.registerGas(FLUIDS, SpaceMod.MOD_ID, name, color);
            case HEAVY -> HbmFluidApi.registerHeavyFluid(FLUIDS, SpaceMod.MOD_ID, name, color);
            case PLASMA -> HbmFluidApi.registerPlasma(FLUIDS, SpaceMod.MOD_ID, name, color);
            default -> HbmFluidApi.registerFluid(FLUIDS, SpaceMod.MOD_ID, name, color);
        };
        // metaOrder: direkt hinter dem Vorgaenger im Space-metaOrder (HBM- oder Space-Fluid)
        HbmFluidApi.insertAfter(anchor == null ? null : ModFluids.getEntry(anchor), e);
        Def d = new Def(name, color, kind, e);
        DEFS.add(d);
        return d;
    }

    private static Def liquid(String name, int color, String anchor) { return def(Kind.LIQUID, name, color, anchor); }
    private static Def gas(String name, int color, String anchor) { return def(Kind.GAS, name, color, anchor); }
    private static Def heavy(String name, int color, String anchor) { return def(Kind.HEAVY, name, color, anchor); }

    private static FT_Polluting releasePoison(float amount) {
        return new FT_Polluting().release(PollutionType.POISON, amount);
    }

    // ── Eintraege in Original-metaOrder (Space-Zweig); dritter Parameter = Vorgaenger im metaOrder ──────────────

    // coolants (hinter MUG_HOT)
    public static final Def SUPERHEATED_HYDROGEN = gas("superheated_hydrogen", 0xE39393, "mug_hot")
            .nfpa(0, 0, 0, NONE).temp(2200).traits(GASEOUS, NOCON, NOID, new FT_Rocket(900, 700_000));
    public static final Def GAS_WATZ = gas("gas_watz", 0x86653E, "superheated_hydrogen")
            .nfpa(4, 0, 3, FluidHazardSymbol.ACID).temp(2500).traits(GASEOUS, NOCON, NOID,
                    releasePoison(ModFluidPollutionPresets.POISON_EXTREME), new FT_Rocket(1200, 700_000));
    public static final Def GASEOUS_URANIUM_BROMIDE = gas("gaseous_uranium_bromide", 0xD1CEBE, "gas_watz")
            .nfpa(0, 0, 0, NONE).temp(2500).traits(GASEOUS, NOCON, NOID, new FT_Rocket(1500, 700_000));
    public static final Def GASEOUS_PLUTONIUM_BROMIDE = gas("gaseous_plutonium_bromide", 0x4C4C4C, "gaseous_uranium_bromide")
            .nfpa(0, 0, 0, NONE).temp(2600).traits(GASEOUS, NOCON, NOID, new FT_Rocket(2000, 700_000));
    public static final Def GASEOUS_SCHRABIDIUM_BROMIDE = gas("gaseous_schrabidium_bromide", 0x006B6B, "gaseous_plutonium_bromide")
            .nfpa(0, 0, 0, NONE).temp(3000).traits(GASEOUS, NOCON, NOID, new FT_Rocket(3000, 700_000));
    public static final Def GASEOUS_THORIUM_BROMIDE = gas("gaseous_thorium_bromide", 0x7A5542, "gaseous_schrabidium_bromide")
            .nfpa(0, 0, 0, NONE).temp(2300).traits(GASEOUS, NOCON, NOID, new FT_Rocket(1300, 700_000));
    // blood
    public static final Def BLOODGAS = liquid("bloodgas", 0x591000, "blood")
            .nfpa(3, 1, 1, NONE).canister(0x591000)
            .traits(new FT_Flammable(1_000_000), new FT_Combustible(FuelGrade.AERO, 2_500_000), LIQUID);
    public static final Def SCUTTERBLOOD = heavy("scutterblood", 0x6C166C, "blood_hot")
            .nfpa(0, 0, 0, NONE).traits(LIQUID, VISCOUS, DELICIOUS);
    // pure elements, cryogenic gasses
    public static final Def FLUORINE = gas("fluorine", 0xC5C539, "chlorine")
            .nfpa(4, 4, 4, FluidHazardSymbol.OXIDIZER).traits(GASEOUS, new FT_Corrosive(40), new FT_Poison(true, 1), new FT_Flammable(10_000));
    public static final Def NITROGEN = liquid("nitrogen", 0xB3C6D2, "mercury")
            .nfpa(1, 0, 0, FluidHazardSymbol.CROYGENIC).temp(-90).traits(LIQUID, EVAP);
    // oils, fuels
    public static final Def NMASSTETRANOL = liquid("nmasstetranol", 0xF1DB0F, "ethanol")
            .nfpa(1, 3, 0, NONE).canister(0xF1DB0F)
            .traits(new FT_Flammable(1_000_000), LIQUID, new FT_Corrosive(70), new FT_Poison(true, 0), new FT_VentRadiation(0.01F));
    public static final Def HYDRAZINE = liquid("hydrazine", 0x31517D, "nitan")
            .nfpa(2, 3, 2, NONE).canister(0x31517D)
            .traits(new FT_Flammable(500_000), new FT_Combustible(FuelGrade.HIGH, 1_250_000), new FT_Corrosive(30), LIQUID, new FT_Rocket(210, 600_000));
    public static final Def MAGMA = heavy("magma", 0xFF3300, "balefire")
            .nfpa(3, 3, 3, NONE).traits(LIQUID, VISCOUS);
    public static final Def BRINE = heavy("brine", 0xD1A73E, "magma")
            .nfpa(3, 3, 3, NONE).traits(LIQUID, VISCOUS);
    public static final Def CONGLOMERA = heavy("conglomera", 0x364D47, "brine")
            .nfpa(0, 0, 2, NONE).traits(LIQUID, VISCOUS);
    public static final Def DICYANOACETYLENE = gas("dicyanoacetylene", 0x675A9F, "conglomera")
            .nfpa(1, 2, 1, NONE).traits(new FT_Flammable(4_000_000), GASEOUS);
    public static final Def HGAS = gas("hgas", 0x999368, "dicyanoacetylene")
            .nfpa(0, 0, 0, FluidHazardSymbol.ACID).traits(GASEOUS, new FT_Corrosive(120));
    public static final Def VINYL = liquid("vinyl", 0xA2A2A2, "hgas").nfpa(0, 0, 0, NONE).traits(LIQUID);
    public static final Def TCRUDE = liquid("tcrude", 0x051914, "vinyl").nfpa(0, 0, 0, NONE).traits(LIQUID);
    public static final Def CBENZ = liquid("cbenz", 0x91C6BB, "tcrude").nfpa(0, 0, 0, NONE).traits(LIQUID);
    public static final Def HALOLIGHT = liquid("halolight", 0xB6F9CF, "cbenz").nfpa(0, 0, 0, NONE).traits(LIQUID);
    // processing fluids (hinter SULFURIC_ACID)
    public static final Def BROMINE = heavy("bromine", 0xAF2214, "sulfuric_acid")
            .nfpa(2, 0, 1, NONE).traits(LIQUID, VISCOUS, new FT_Corrosive(10));
    public static final Def URANIUM_BROMIDE = heavy("uranium_bromide", 0xD1CEBE, "bromine")
            .nfpa(0, 0, 0, NONE).temp(200).traits(LIQUID, VISCOUS, new FT_Corrosive(65), new FT_VentRadiation(0.1F));
    public static final Def THORIUM_BROMIDE = heavy("thorium_bromide", 0x7A5542, "uranium_bromide")
            .nfpa(0, 0, 0, NONE).temp(200).traits(LIQUID, VISCOUS, new FT_Corrosive(65), new FT_VentRadiation(0.1F));
    public static final Def AQUEOUS_COPPER = heavy("aqueous_copper", 0x4CC2A2, "thorium_bromide")
            .nfpa(0, 0, 0, NONE).traits(LIQUID, VISCOUS);
    public static final Def COPPERSULFATE = heavy("coppersulfate", 0x55E5CF, "aqueous_copper")
            .nfpa(0, 0, 0, NONE).traits(LIQUID, VISCOUS);
    public static final Def LITHYDRO = gas("lithydro", 0xD1CEBE, "coppersulfate").nfpa(0, 0, 0, NONE).traits(GASEOUS);
    public static final Def LITHCARBONATE = gas("lithcarbonate", 0xD1CEBE, "lithydro").nfpa(0, 0, 0, NONE).traits(GASEOUS);
    // meths
    public static final Def CHLOROMETHANE = gas("chloromethane", 0xD3CF9E, "lithcarbonate")
            .nfpa(2, 4, 0, NONE).traits(GASEOUS, new FT_Corrosive(15), new FT_Flammable(50_000));
    public static final Def METHANOL = liquid("methanol", 0x88739F, "chloromethane")
            .nfpa(3, 4, 0, NONE).traits(GASEOUS, new FT_Flammable(400_000), new FT_Combustible(FuelGrade.HIGH, 600_000), LIQUID);
    public static final Def POLYTHYLENE = liquid("polythylene", 0x35302E, "methanol")
            .nfpa(1, 2, 0, NONE).traits(LIQUID, new FT_Flammable(50_000));
    public static final Def CHLOROETHANE = gas("chloroethane", 0xBBA9A0, "polythylene").nfpa(2, 0, 0, NONE).traits(GASEOUS);
    // airs (EARTHAIR = hbm_m:air, steht in HBM am Listenanfang)
    public static final Def DUNAAIR = gas("dunaair", 0xD4704E, "chloroethane")
            .nfpa(3, 0, 0, FluidHazardSymbol.ASPHYXIANT).gastank(0xCF5B34, 0xFFFFFF)
            .traits(GASEOUS, releasePoison(ModFluidPollutionPresets.POISON_MINOR));
    public static final Def EVEAIR = gas("eveair", 0xDCABF8, "dunaair")
            .nfpa(4, 0, 0, FluidHazardSymbol.OXIDIZER).gastank(0x8C21FF, 0xFFFFFF)
            .traits(GASEOUS, new FT_Corrosive(25), new FT_Poison(true, 1));
    public static final Def JOOLGAS = gas("joolgas", 0x96B596, "eveair")
            .nfpa(0, 0, 0, FluidHazardSymbol.ASPHYXIANT).gastank(0x78B878, 0xFF8400).traits(GASEOUS);
    public static final Def SARNUSGAS = gas("sarnusgas", 0xE47D5C, "joolgas")
            .nfpa(0, 0, 0, FluidHazardSymbol.ASPHYXIANT).gastank(0xB56847, 0xFF8400).traits(GASEOUS);
    public static final Def UGAS = gas("ugas", 0x718C9A, "sarnusgas")
            .nfpa(0, 0, 0, FluidHazardSymbol.ASPHYXIANT).gastank(0x5099BF, 0xFF8400).traits(GASEOUS);
    public static final Def NGAS = gas("ngas", 0xA37BA3, "ugas")
            .nfpa(0, 0, 0, FluidHazardSymbol.ASPHYXIANT).gastank(0xBE92E0, 0xFF8400).traits(GASEOUS);
    public static final Def TEKTOAIR = gas("tektoair", 0x245F46, "ngas")
            .nfpa(4, 2, 0, FluidHazardSymbol.OXIDIZER).gastank(0x388C69, 0x303030)
            .traits(GASEOUS, new FT_Poison(true, 1), new FT_Flammable(30_000));
    // acids (hinter NITRIC_ACID)
    public static final Def HCL = liquid("hcl", 0x00D452, "nitric_acid")
            .nfpa(3, 0, 3, FluidHazardSymbol.ACID).traits(new FT_Corrosive(30), LIQUID);
    public static final Def AMMONIA = gas("ammonia", 0x00A0F7, "hcl")
            .nfpa(2, 0, 1, FluidHazardSymbol.ASPHYXIANT).traits(new FT_Poison(true, 4), GASEOUS, new FT_Terraformer("DUNA"));
    public static final Def HTCO4 = liquid("htco4", 0x675454, "radiosolvent")
            .nfpa(1, 3, 0, FluidHazardSymbol.RADIATION).traits(LIQUID, new FT_Corrosive(10), new FT_VentRadiation(0.5F));
    public static final Def KMNO4 = liquid("kmno4", 0x560046, "htco4")
            .nfpa(4, 0, 0, FluidHazardSymbol.ACID).traits(LIQUID, new FT_Corrosive(15), new FT_Poison(true, 1));
    // salts (hinter ALUMINA)
    public static final Def AQUEOUS_NICKEL = liquid("aqueous_nickel", 0xDACEBA, "alumina").nfpa(0, 0, 0, NONE).traits(LIQUID);
    // slurry (hinter ENDERJUICE)
    public static final Def MINSOL = liquid("minsol", 0xFADF6A, "enderjuice")
            .nfpa(3, 0, 3, FluidHazardSymbol.ACID).traits(new FT_Corrosive(10), LIQUID);
    public static final Def NMASS = liquid("nmass", 0x53A9F4, "minsol")
            .nfpa(1, 2, 0, NONE).traits(LIQUID, new FT_Corrosive(10), new FT_Poison(true, 0), new FT_VentRadiation(0.04F));
    // noble gasses (hinter PLASMA_BF): KRYPTON, XENON, ARGON, NEON
    public static final Def KRYPTON = gas("krypton", 0x9AC6E6, "plasma_bf")
            .nfpa(0, 0, 0, FluidHazardSymbol.CROYGENIC).gastank(0x9AC6E6, 0x303030).traits(GASEOUS);
    static {
        // Space verschiebt XENON aus den "pure elements" zu den Edelgasen (metaOrder: KRYPTON, XENON, ARGON, NEON)
        HbmFluidRegistry.getOrderedFluids().remove(ModFluids.XENON);
        HbmFluidApi.insertAfter(KRYPTON.entry, ModFluids.XENON);
    }
    public static final Def ARGON = gas("argon", 0xFD70D0, "xenon")
            .nfpa(0, 0, 0, FluidHazardSymbol.CROYGENIC).gastank(0xFD70D0, 0x303030).traits(GASEOUS);
    public static final Def NEON = gas("neon", 0xF1F600, "argon")
            .nfpa(0, 0, 0, FluidHazardSymbol.CROYGENIC).gastank(0xF1F600, 0x303030).traits(GASEOUS);
    // misc for cute dates
    public static final Def TEA = liquid("tea", 0x76523C, "neon").nfpa(0, 0, 0, NONE).traits(DELICIOUS, LIQUID);
    public static final Def HONEY = liquid("honey", 0xD99A02, "tea").nfpa(0, 0, 0, NONE).traits(DELICIOUS, LIQUID);
    public static final Def COFFEE = liquid("coffee", 0x57493D, "honey").nfpa(0, 0, 0, NONE).traits(DELICIOUS, LIQUID);
    public static final Def MILK = liquid("milk", 0xCFCFCF, "coffee").nfpa(0, 0, 0, NONE).traits(DELICIOUS, LIQUID);
    public static final Def SMILK = liquid("smilk", 0xF5DEE4, "milk").nfpa(0, 0, 0, NONE).traits(DELICIOUS, LIQUID);
    public static final Def CCL = liquid("ccl", 0x0C3B2F, "smilk").nfpa(0, 0, 0, NONE).traits(LIQUID, new FT_Corrosive(10));
    public static final Def ELBOWGREASE = liquid("elbowgrease", 0xCBC433, "ccl") // cute date with a mechanic
            .nfpa(1, 3, 0, NONE).canister(0xCBC433).traits(new FT_Flammable(600_000), LIQUID);
    public static final Def EMILK = liquid("emilk", 0xCFCFCF, "elbowgrease").nfpa(0, 0, 0, NONE).traits(DELICIOUS, LIQUID);
    public static final Def CMILK = liquid("cmilk", 0xCFCFCF, "emilk").nfpa(0, 0, 0, NONE).traits(DELICIOUS, LIQUID);
    public static final Def CREAM = liquid("cream", 0xCFCFCF, "cmilk").nfpa(0, 0, 0, NONE).traits(DELICIOUS, LIQUID);

    private SpaceFluids() {}

    /** Alle Space-Fluide in Original-metaOrder. */
    public static List<Def> definitions() {
        return Collections.unmodifiableList(DEFS);
    }

    /** Mod-Konstruktor: Register anmelden (Eintraege stehen schon durch die Klasseninitialisierung), Traits laden. */
    public static void init() {
        FLUIDS.register();
        registerTraits();
        // Traits/Gefahrenraute/Behaelter: SpaceFluidsSetup (FMLLoadCompleteEvent, nach HBMs Fluid-Bootstrap)
    }

    /**
     * Neue Trait-Namen "rocket"/"terraformer" (Original direkt hinter "pheromone") und "uk"/"explosive" (am Ende).
     * {@code FluidTrait.traitList} bestimmt die Tooltip-Reihenfolge; das Addon kann nur anhaengen und sortiert daher nach.
     */
    private static void registerTraits() {
        FT_Rocket.register();
        FT_Terraformer.register();
        SpaceTraitsSimple.register();
        List<Class<? extends FluidTrait>> list = FluidTrait.traitList;
        list.remove(FT_Rocket.class);
        list.remove(FT_Terraformer.class);
        int at = list.indexOf(FT_Pheromone.class);
        at = at < 0 ? list.size() : at + 1;
        list.add(at, FT_Rocket.class);
        list.add(at + 1, FT_Terraformer.class);
    }

    // ═══════════════════════════════ Common-Setup ═══════════════════════════════

    /** Nach HBMs {@code ModFluidTraitsBootstrap} (s. {@link SpaceFluidsSetup}), genau einmal. */
    static synchronized void setup() {
        for (Def d : DEFS) {
            FluidType type = FluidType.forFluid(d.fluid());
            if (d.temp != null) type.setTemp(d.temp);
            type.addTraits(d.traits.toArray(new FluidTrait[0]));
        }
        registerCalculatedFuels();
        registerHeatChains();
        registerRocketFuels();
        for (Def d : DEFS) {
            FluidType.setHazardDiamond(d.fluid(), d.poison, d.flammability, d.reactivity, d.symbol);
        }
        registerContainerDefs();
        registerContainers();
    }

    /** Original {@code registerCalculatedFuel} (Spreadsheet) fuer die vier Space-Treibstoffe, nach HBMs eigenen Werten. */
    private static void registerCalculatedFuels() {
        double baseline = 100_000D;
        double demandHigh = 2.0D;
        double complexityRefinery = 1.1D;
        double complexityCracking = 1.25D;
        double flammabilityHigh = 2.0D;

        long kerosene = heat(ModFluids.KEROSENE);
        long balefire = heat(ModFluids.BALEFIRE);
        long unsaturateds = heat(ModFluids.UNSATURATEDS);

        calculatedFuel(BLOODGAS, kerosene * 0.8, 2.5, FuelGrade.AERO); //0.8
        calculatedFuel(NMASSTETRANOL, balefire * 1000, 10.5, FuelGrade.HIGH); //0.8
        calculatedFuel(DICYANOACETYLENE, (baseline / 0.15 * flammabilityHigh * demandHigh * complexityRefinery * complexityCracking) + unsaturateds, 0, null);
        calculatedFuel(METHANOL, 375_000D /* diesel / 2 */, 2.5D, FuelGrade.HIGH);
    }

    private static long heat(FluidEntry e) {
        FT_Flammable f = FluidType.getTrait(e.getSource(), FT_Flammable.class);
        return f != null ? f.getHeatEnergy() : 0L;
    }

    private static void calculatedFuel(Def d, double base, double combustMult, @Nullable FuelGrade grade) {
        long flammable = round((long) base);
        long combustible = round((long) (base * combustMult));
        FluidType type = FluidType.forFluid(d.fluid());
        type.addTraits(new FT_Flammable(flammable));
        if (combustible > 0 && grade != null) type.addTraits(new FT_Combustible(grade, combustible));
    }

    /** ugly but it does the thing well enough */
    private static long round(long l) {
        if (l > 10_000_000L) return l - (l % 100_000L);
        if (l > 1_000_000L) return l - (l % 10_000L);
        if (l > 100_000L) return l - (l % 1_000L);
        if (l > 10_000L) return l - (l % 100L);
        if (l > 1_000L) return l - (l % 10L);
        return l;
    }

    /** Space-Zusaetze der Heiz-/Kuehlketten (PWR/HTR), 1:1 inkl. WASTEGAS -> WATZ des Originals. */
    private static void registerHeatChains() {
        Fluid h2 = ModFluids.HYDROGEN.getSource();
        Fluid watz = ModFluids.WATZ.getSource();
        Fluid wasteFluid = ModFluids.WASTEFLUID.getSource();
        Fluid wasteGas = ModFluids.WASTEGAS.getSource();
        Fluid coolant = ModFluids.COOLANT.getSource();

        // COOLANT erhaelt im Space-Zweig zusaetzlich BOILER (Rest wie HBM)
        FluidType.addTrait(coolant, new FT_Heatable().setEff(HeatingType.BOILER, 1.0D).setEff(HeatingType.HEATEXCHANGER, 1.0D)
                .setEff(HeatingType.PWR, 1.0D).setEff(HeatingType.ICF, 1.0D).addStep(300, 1, ModFluids.COOLANT_HOT.getSource(), 1));

        FluidType.addTrait(h2, new FT_Heatable().setEff(HeatingType.PWR, 1.0D).addStep(300, 1, SUPERHEATED_HYDROGEN.fluid(), 1));
        FluidType.addTrait(SUPERHEATED_HYDROGEN.fluid(), new FT_Coolable(h2, 1, 1, 300));

        FluidType.addTrait(watz, new FT_Heatable().setEff(HeatingType.PWR, 1.0D).addStep(300, 1, GAS_WATZ.fluid(), 1));
        FluidType.addTrait(watz, new FT_PWRModerator(1.40D));
        FluidType.addTrait(GAS_WATZ.fluid(), new FT_Coolable(watz, 1, 1, 300));

        FluidType.addTrait(wasteFluid, new FT_Heatable().setEff(HeatingType.PWR, 1.0D).addStep(300, 1, wasteGas, 1));
        FluidType.addTrait(wasteFluid, new FT_PWRModerator(1.20D));
        FluidType.addTrait(wasteGas, new FT_Coolable(watz, 1, 1, 300));

        FluidType.addTrait(URANIUM_BROMIDE.fluid(), new FT_Heatable().setEff(HeatingType.PWR, 1.0D).addStep(300, 1, GASEOUS_URANIUM_BROMIDE.fluid(), 1));
        FluidType.addTrait(URANIUM_BROMIDE.fluid(), new FT_PWRModerator(1.75D));
        FluidType.addTrait(GASEOUS_URANIUM_BROMIDE.fluid(), new FT_Coolable(URANIUM_BROMIDE.fluid(), 1, 1, 300));

        FluidType.addTrait(THORIUM_BROMIDE.fluid(), new FT_Heatable().setEff(HeatingType.PWR, 1.0D).addStep(300, 1, GASEOUS_THORIUM_BROMIDE.fluid(), 1));
        FluidType.addTrait(THORIUM_BROMIDE.fluid(), new FT_PWRModerator(1.50D));
        FluidType.addTrait(GASEOUS_THORIUM_BROMIDE.fluid(), new FT_Coolable(THORIUM_BROMIDE.fluid(), 1, 1, 300));
    }

    /** {@code new FT_Rocket(isp, thrust)} an HBM-Fluiden (Space-Aenderungen in {@code Fluids.init}). */
    private static void registerRocketFuels() {
        rocket(ModFluids.KEROSENE, 308, 981000);
        rocket(ModFluids.HYDROGEN, 380, 700_000);
        rocket(ModFluids.XENON, 4200, 80_000);
        rocket(ModFluids.WASTEGAS, 900, 700_000);
        rocket(ModFluids.PLASMA_DT, 12000, 700_000);
        rocket(ModFluids.PLASMA_HD, 7500, 700_000);
        rocket(ModFluids.PLASMA_HT, 10000, 700_000);
        rocket(ModFluids.PLASMA_XM, 25000, 700_000);
        rocket(ModFluids.PLASMA_BF, 50000, 700_000);
        rocket(ModFluids.PLASMA_DH3, 20000, 700_000);
        rocket(ModFluids.KEROSENE_REFORM, 321, 1_564_000);
    }

    private static void rocket(FluidEntry e, int isp, long thrust) {
        FluidType.addTrait(e.getSource(), new FT_Rocket(isp, thrust));
    }

    // ═══════════════════════════════ Behaelter ═══════════════════════════════

    /**
     * Original {@code addContainers(new CD_Canister/CD_Gastank)}. HBM fuehrt die Tabelle in privaten Maps von
     * {@link FluidContainerDefs} (kein Erweiterungspunkt, Wunsch W-R4A-1 in SPACE_API_wishes.md) - bis dahin per
     * Reflexion, ohne HBM-Code zu aendern. Muss vor dem ersten {@code FluidContainerRegistry.init()} laufen
     * (lazy, erst im Spiel/JEI). EARTHAIR-Gasflasche ({@code CD_Gastank(0xC3BEE8, 0xFFFFFF)}) haengt an {@code hbm_m:air}.
     */
    private static void registerContainerDefs() {
        Map<String, FluidContainerDefs.CDCanister> canisters = defsMap("CANISTERS");
        Map<String, FluidContainerDefs.CDGastank> gastanks = defsMap("GASTANKS");
        if (canisters == null || gastanks == null) return;
        for (Def d : DEFS) {
            if (d.canister >= 0) canisters.putIfAbsent(d.name, new FluidContainerDefs.CDCanister(d.canister));
            if (d.gasBottle >= 0) gastanks.putIfAbsent(d.name, new FluidContainerDefs.CDGastank(d.gasBottle, d.gasLabel));
        }
        gastanks.putIfAbsent("air", new FluidContainerDefs.CDGastank(0xC3BEE8, 0xFFFFFF)); // EARTHAIR
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static <T> Map<String, T> defsMap(String field) {
        try {
            Field f = FluidContainerDefs.class.getDeclaredField(field);
            f.setAccessible(true);
            return (Map<String, T>) f.get(null);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            SpaceRegistry.LOGGER.warn("[space_m] FluidContainerDefs.{} nicht erreichbar - keine Space-Kanister/Gasflaschen: {}", field, ex.toString());
            return null;
        }
    }

    /**
     * Space-Zusaetze in {@code FluidContainerRegistry.register()}: Weltfluid-Eimer (Original
     * {@code bucket_mercury/bromine/ccl} -> {@code Fluids.MERCURY/BROMINE/CCL}), Getraenke und Trockeneis.
     * Gegenstaende anderer Runden (R4-B) werden per ID gesucht; fehlt einer, wird der Eintrag uebersprungen.
     */
    private static void registerContainers() {
        container("bucket_mercury", "minecraft:bucket", ModFluids.MERCURY.getSource(), 1000);
        container("bucket_bromine", "minecraft:bucket", BROMINE.fluid(), 1000);
        container("bucket_ccl", "minecraft:bucket", CCL.fluid(), 1000);
        container("glass_smilk", "glass_empty", SMILK.fluid(), 100);
        container("minecraft:milk_bucket", "minecraft:bucket", MILK.fluid(), 1000);
        container("coffee", "cmug_empty", COFFEE.fluid(), 100);
        container("teacup", "teacup_empty", TEA.fluid(), 100);
        container("bottle_honey", "minecraft:glass_bottle", HONEY.fluid(), 100);
        container("dry_ice", null, ModFluids.CARBONDIOXIDE.getSource(), 1000); //literal block of carbon dioxide
    }

    private static void container(String full, @Nullable String empty, Fluid fluid, int amount) {
        Item f = item(full);
        Item e = empty == null ? null : item(empty);
        if (f == null || (empty != null && e == null)) {
            SpaceRegistry.LOGGER.info("[space_m] Fluidbehaelter {} -> {} uebersprungen (Gegenstand fehlt noch)", full, FluidType.forFluid(fluid).getName());
            return;
        }
        FluidContainerRegistry.registerContainer(new FluidContainerRegistry.FluidContainer(
                new ItemStack(f), e == null ? null : new ItemStack(e), fluid, amount));
    }

    /** {@code ns:id} oder ohne Namensraum zuerst {@code space_m}, dann {@code hbm_m}. */
    @Nullable
    private static Item item(String id) {
        if (id.contains(":")) return lookup(ResourceLocation.tryParse(id));
        Item it = lookup(SpaceMod.id(id));
        return it != null ? it : lookup(ResourceLocation.fromNamespaceAndPath("hbm_m", id));
    }

    @Nullable
    private static Item lookup(@Nullable ResourceLocation rl) {
        if (rl == null || !BuiltInRegistries.ITEM.containsKey(rl)) return null;
        Item it = BuiltInRegistries.ITEM.get(rl);
        return it == Items.AIR ? null : it;
    }
}
