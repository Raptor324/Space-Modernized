package com.space_m.resources;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.space_m.celestial.SolarSystem;
import com.space_m.dim.PlanetOres;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * 1:1 NTM Space {@code BlockOre.spawnsOn} + {@code addValidBody/addAllBodies/addAllExcept} (Aufrufe aus
 * {@code WorldGeneratorCelestial} und den {@code WorldGenerator<Koerper>}-Konstruktoren) und
 * {@code BlockOre.vanillaMap}: auf welchen Koerpern ein Erz vorkommt (Tooltip "Can be found on").
 *
 * <p>Original: ein Erzblock mit Metadatum je Koerper. Im Port gibt es je Erz mehrere Bloecke: die Kerbin-Variante
 * von HBM-Modernized ({@code hbm_m:<x>_ore}, {@code _deepslate}, teils {@code ore_<x>}) bzw. Vanilla und die
 * Koerper-Varianten des Addons ({@code space_m:<erz>_<koerper>}, {@link PlanetOres}). Alle gehoeren zum selben
 * Original-Erz und zeigen denselben Tooltip. Deepslate-Erze gibt es erst ab 1.17 - sie gelten als dasselbe Erz.
 */
public final class OreLocations {

    private OreLocations() {}

    /** Original-Erzname ({@code ore_iron}) -> Koerper. */
    private static final Map<String, Set<SolarSystem.Body>> spawnsOn = new LinkedHashMap<>();

    /** Original {@code vanillaMap} + HBM-Port-Namen: Gegenstands-ID -> Original-Erzname. */
    private static final Map<ResourceLocation, String> itemToOre = new HashMap<>();

    /** Koerper-Varianten {@code <erz>_<koerper>} -> Original-Erzname (nach der Registrierung aufgebaut). */
    @Nullable private static volatile Map<String, String> variantToOre;

    /** Vanilla-Kohle (Original Sonderfall in {@code ModEventHandlerClient}: nur Kerbin). */
    public static final String COAL = "ore_coal";

    static {
        // WorldGeneratorCelestial: "Specify which ores spawn where"
        addAllBodies("ore_iron");
        addAllBodies("ore_gold");
        addAllBodies("ore_diamond");
        addAllBodies("ore_redstone");

        addValidBody("ore_emerald", SolarSystem.Body.KERBIN);
        addValidBody("ore_lapis", SolarSystem.Body.KERBIN);

        addAllBodies("ore_uranium");
        addAllBodies("ore_thorium");
        addAllBodies("ore_titanium");
        addAllBodies("ore_sulfur");
        addAllBodies("ore_aluminium");
        addAllBodies("ore_copper");
        addAllBodies("ore_zinc");
        addAllBodies("ore_fluorite");
        addAllBodies("ore_niter");
        addAllBodies("ore_tungsten");
        addAllBodies("ore_lead");
        addAllBodies("ore_beryllium");
        addAllBodies("ore_rare");
        addAllBodies("ore_cinnebar");
        addAllBodies("ore_cobalt");
        addAllBodies("cluster_iron");
        addAllBodies("cluster_titanium");
        addAllBodies("cluster_aluminium");
        addAllBodies("cluster_copper");

        addValidBody("ore_asbestos", SolarSystem.Body.KERBIN);
        addValidBody("ore_lignite", SolarSystem.Body.KERBIN);
        addValidBody("ore_oil", SolarSystem.Body.KERBIN);
        addValidBody("ore_bedrock_oil", SolarSystem.Body.KERBIN);
        addValidBody("ore_coltan", SolarSystem.Body.KERBIN);

        addAllExcept("ore_nickel", SolarSystem.Body.KERBIN);

        // WorldGeneratorDres
        addValidBody("ore_shale", SolarSystem.Body.DRES);
        addValidBody("ore_lanthanium", SolarSystem.Body.DRES);
        addValidBody("ore_niobium", SolarSystem.Body.DRES);
        addValidBody("ore_coltan", SolarSystem.Body.DRES);
        addValidBody("ore_lanthanium", SolarSystem.Body.DRES);
        // WorldGeneratorDuna
        addValidBody("ore_oil", SolarSystem.Body.DUNA);
        // WorldGeneratorEve
        addValidBody("ore_niobium", SolarSystem.Body.EVE);
        addValidBody("ore_iodine", SolarSystem.Body.EVE);
        addValidBody("ore_schrabidium", SolarSystem.Body.EVE);
        addValidBody("ore_gas", SolarSystem.Body.EVE);
        // WorldGeneratorIke
        addValidBody("ore_mineral", SolarSystem.Body.IKE);
        addValidBody("ore_lithium", SolarSystem.Body.IKE);
        addValidBody("ore_coltan", SolarSystem.Body.IKE);
        addValidBody("ore_asbestos", SolarSystem.Body.IKE);
        addValidBody("ore_brine", SolarSystem.Body.IKE);
        // WorldGeneratorLaythe
        addValidBody("ore_emerald", SolarSystem.Body.LAYTHE);
        addValidBody("ore_lapis", SolarSystem.Body.LAYTHE);
        addValidBody("ore_asbestos", SolarSystem.Body.LAYTHE);
        addValidBody("ore_oil", SolarSystem.Body.LAYTHE);
        // WorldGeneratorMinmus
        addValidBody("ore_brine", SolarSystem.Body.MINMUS);
        // WorldGeneratorMoho
        addValidBody("ore_mineral", SolarSystem.Body.MOHO);
        addValidBody("ore_shale", SolarSystem.Body.MOHO);
        addValidBody("ore_glowstone", SolarSystem.Body.MOHO);
        addValidBody("ore_fire", SolarSystem.Body.MOHO);
        addValidBody("ore_australium", SolarSystem.Body.MOHO);
        // WorldGeneratorMoon
        addValidBody("ore_lithium", SolarSystem.Body.MUN);
        addValidBody("ore_quartz", SolarSystem.Body.MUN);
        addValidBody("ore_shale", SolarSystem.Body.MUN);
        addValidBody("ore_brine", SolarSystem.Body.MUN);
        // WorldGeneratorTekto
        addValidBody("ore_tekto", SolarSystem.Body.TEKTO);

        // Original vanillaMap (new BlockOre(Material.rock, Blocks.x_ore)) + Deepslate
        vanilla("ore_iron", "iron_ore", "deepslate_iron_ore");
        vanilla("ore_gold", "gold_ore", "deepslate_gold_ore");
        vanilla("ore_redstone", "redstone_ore", "deepslate_redstone_ore");
        vanilla("ore_lapis", "lapis_ore", "deepslate_lapis_ore");
        vanilla("ore_emerald", "emerald_ore", "deepslate_emerald_ore");
        vanilla("ore_quartz", "nether_quartz_ore");
        vanilla("ore_diamond", "diamond_ore", "deepslate_diamond_ore");
        vanilla(COAL, "coal_ore", "deepslate_coal_ore");

        // Kerbin-Varianten in HBM-Modernized (Port-Namen)
        hbm("ore_uranium", "uranium_ore", "uranium_ore_deepslate");
        hbm("ore_thorium", "thorium_ore", "thorium_ore_deepslate");
        hbm("ore_titanium", "titanium_ore", "titanium_ore_deepslate");
        hbm("ore_sulfur", "sulfur_ore", "sulfur_ore_deepslate");
        hbm("ore_aluminium", "ore_aluminium", "aluminum_ore_deepslate");
        hbm("ore_copper", "ore_copper");
        hbm("ore_fluorite", "fluorite_ore", "fluorite_ore_deepslate");
        hbm("ore_niter", "niter_ore", "niter_ore_deepslate");
        hbm("ore_tungsten", "tungsten_ore", "tungsten_ore_deepslate");
        hbm("ore_lead", "lead_ore", "lead_ore_deepslate");
        hbm("ore_beryllium", "beryllium_ore", "beryllium_ore_deepslate");
        hbm("ore_rare", "ore_rare", "rareground_ore", "rareground_ore_deepslate");
        hbm("ore_cinnebar", "cinnabar_ore", "cinnabar_ore_deepslate");
        hbm("ore_cobalt", "cobalt_ore", "cobalt_ore_deepslate");
        hbm("cluster_iron", "cluster_iron");
        hbm("cluster_titanium", "cluster_titanium");
        hbm("cluster_aluminium", "cluster_aluminium");
        hbm("cluster_copper", "cluster_copper");
        hbm("ore_asbestos", "asbestos_ore", "asbestos_ore_deepslate");
        hbm("ore_lignite", "lignite_ore", "lignite_ore_deepslate");
        hbm("ore_oil", "ore_oil");
        hbm("ore_bedrock_oil", "ore_bedrock_oil");
        hbm("ore_coltan", "coltan_ore", "coltan_ore_deepslate");
        hbm("ore_schrabidium", "schrabidium_ore");
        hbm("ore_lithium", "lithium_ore", "lithium_ore_deepslate");
        hbm("ore_australium", "ore_australium", "australium_ore");
    }

    /** Original {@code addValidBody}. */
    public static void addValidBody(String ore, SolarSystem.Body body) {
        spawnsOn.computeIfAbsent(ore, k -> EnumSet.noneOf(SolarSystem.Body.class)).add(body);
    }

    /** Original {@code addAllBodies}. */
    public static void addAllBodies(String ore) {
        for (SolarSystem.Body celestial : SolarSystem.Body.values()) {
            addValidBody(ore, celestial);
        }
    }

    /** Original {@code addAllExcept}. */
    public static void addAllExcept(String ore, SolarSystem.Body body) {
        for (SolarSystem.Body celestial : SolarSystem.Body.values()) {
            if (celestial == body) continue;
            addValidBody(ore, celestial);
        }
    }

    private static void vanilla(String ore, String... ids) {
        for (String id : ids) itemToOre.put(ResourceLocation.fromNamespaceAndPath("minecraft", id), ore);
    }

    private static void hbm(String ore, String... ids) {
        for (String id : ids) itemToOre.put(ResourceLocation.fromNamespaceAndPath("hbm_m", id), ore);
    }

    /** Koerper eines Original-Erzes (leer = kein Tooltip). */
    public static Set<SolarSystem.Body> spawnsOn(String ore) {
        Set<SolarSystem.Body> s = spawnsOn.get(ore);
        return s != null ? Collections.unmodifiableSet(s) : Collections.emptySet();
    }

    /** Original-Erzname eines Gegenstands (HBM/Vanilla/Koerper-Variante), sonst null. */
    @Nullable
    public static String oreOf(Item item) {
        if (item == Items.AIR) return null;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        String ore = itemToOre.get(id);
        if (ore != null) return ore;
        if (!com.space_m.SpaceMod.MOD_ID.equals(id.getNamespace())) return null;
        Map<String, String> variants = variantToOre;
        if (variants == null) {
            variants = new HashMap<>();
            for (PlanetOres.Variant v : PlanetOres.variants()) variants.put(v.id(), v.ore());
            variantToOre = variants;
        }
        return variants.get(id.getPath());
    }

    /** Alle Original-Erze mit Fundort (Tests). */
    public static List<String> ores() {
        return List.copyOf(spawnsOn.keySet());
    }

    /**
     * Tooltip-Zeilen 1:1 {@code BlockOre.addInformation} bzw. {@code ModEventHandlerClient /// ORES ///} (Kohle nur
     * Kerbin). Kein Erz -> nichts.
     */
    public static void addTooltip(Item item, List<Component> list) {
        String ore = oreOf(item);
        if (ore == null) return;

        if (COAL.equals(ore)) {
            // we don't have any celestial coal, special case
            list.add(Component.literal("Can be found on:").withStyle(ChatFormatting.GOLD));
            list.add(Component.literal(" - ").append(Component.translatable("body.kerbin")).withStyle(ChatFormatting.AQUA));
            return;
        }

        addLines(spawnsOn(ore), list);
    }

    /** Original {@code BlockOre.addInformation}. */
    public static void addLines(Set<SolarSystem.Body> spawnsOn, List<Component> list) {
        if (spawnsOn.isEmpty()) return;

        if (spawnsOn.size() == SolarSystem.Body.values().length) {
            list.add(Component.literal("Can be found anywhere").withStyle(ChatFormatting.GOLD));
            return;
        } else if (spawnsOn.size() == SolarSystem.Body.values().length - 1) {
            list.add(Component.literal("Can be found anywhere except:").withStyle(ChatFormatting.GOLD));
            for (SolarSystem.Body body : SolarSystem.Body.values()) {
                if (spawnsOn.contains(body)) continue;
                list.add(Component.literal(" - ").append(Component.translatable("body." + body.name)).withStyle(ChatFormatting.RED));
            }
            return;
        }

        list.add(Component.literal("Can be found on:").withStyle(ChatFormatting.GOLD));
        for (SolarSystem.Body body : spawnsOn) {
            list.add(Component.literal(" - ").append(Component.translatable("body." + body.name)).withStyle(ChatFormatting.AQUA));
        }
    }
}
