package com.space_m.dim.laythe;

import java.util.LinkedHashMap;
import java.util.Map;

import com.space_m.dim.PlanetOres;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.world.level.block.Block;

/**
 * Koerper-Varianten der Erze aus {@code WorldGeneratorCelestial.generateNTMOres} (+ optional
 * {@code generateVanillaOres}) fuer Laythe, Tekto und Thatmo - dieselben Werte wie {@code MunBlocks}
 * (Haerte/Widerstand 1:1 {@code ModBlocks}). Gemeinsamer Helfer der R3-4-Koerper.
 */
public final class CelestialOreSet {

    public final String body;
    public final Map<String, RegistrySupplier<Block>> ores = new LinkedHashMap<>();

    private CelestialOreSet(String body) {
        this.body = body;
    }

    public RegistrySupplier<Block> get(String ore) {
        RegistrySupplier<Block> b = ores.get(ore);
        if (b == null) throw new IllegalArgumentException(ore + "_" + body);
        return b;
    }

    private void add(String ore, float h, float r) {
        ores.put(ore, PlanetOres.ore(ore, body, h, r));
    }

    /**
     * @param vanilla {@code generateVanillaOres} (nur wenn das Planetengestein nicht {@code stone} ist)
     * @param ntm     {@code generateNTMOres}
     */
    public static CelestialOreSet create(String body, boolean vanilla, boolean ntm) {
        CelestialOreSet s = new CelestialOreSet(body);
        if (vanilla) {
            s.add("ore_iron", 3.0F, 5.0F);
            s.add("ore_gold", 3.0F, 5.0F);
            s.add("ore_redstone", 3.0F, 5.0F);
            s.add("ore_diamond", 3.0F, 5.0F);
        }
        if (ntm) {
            s.ores.put("ore_uranium", PlanetOres.ore("ore_uranium", body,
                    p -> new com.hbm_m.block.gas.OutgasBlock(p, () -> com.hbm_m.block.ModBlocks.GAS_RADON.get(), true, true), 5.0F, 10.0F));
            for (String o : new String[] {"ore_thorium", "ore_titanium", "ore_sulfur", "ore_aluminium", "ore_copper", "ore_nickel", "ore_zinc",
                    "ore_fluorite", "ore_niter", "ore_tungsten", "ore_lead"}) s.add(o, 5.0F, 10.0F);
            s.add("ore_beryllium", 5.0F, 15.0F);
            s.add("ore_rare", 5.0F, 10.0F);
            s.add("ore_cinnebar", 5.0F, 10.0F);
            s.add("ore_cobalt", 5.0F, 10.0F);
            for (String o : new String[] {"cluster_iron", "cluster_titanium", "cluster_aluminium", "cluster_copper"}) s.add(o, 5.0F, 15.0F);
        }
        return s;
    }

    /** Einzelne Variante (z.B. {@code ore_asbestos} auf Laythe). */
    public static RegistrySupplier<Block> single(String ore, String body, float h, float r) {
        return PlanetOres.ore(ore, body, h, r);
    }
}
