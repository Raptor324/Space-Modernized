package com.space_m.dim;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import com.space_m.block.SpaceBlocks;
import com.space_m.main.SpaceCreativeTabs;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;

/**
 * Koerper-Varianten der Erze (Original: {@code BlockOre} mit Metadatum = {@code SolarSystem.Body.ordinal()},
 * gerendert in zwei Durchgaengen: Planetengestein {@code body.getStoneTexture()} + Erz-Overlay).
 *
 * <p>1.20 hat keine Metadaten; jede Kombination wird ein eigener Block {@code space_m:<erz>_<koerper>}
 * (z.B. {@code ore_lithium_mun}). Modell {@code space_m:block/planet_ore}: zwei Wuerfel, unten das Gestein, oben
 * das Original-Overlay (cutout) - dieselbe Zwei-Schichten-Darstellung wie {@code RenderBlockMultipass}.
 * Name/Lang wie im Original ({@code tile.<erz>.name}), Beute wie das Erz der Oberwelt (Kerbin-Variante in
 * HBM-Modernized) mit Behutsamkeit = die Koerper-Variante selbst.
 *
 * <p>Die Kerbin-Variante (Metadatum 1) ist das Erz von HBM-Modernized bzw. Vanilla und wird hier nicht angelegt.
 * Neue Koerper (R3) rufen {@link #ore} in ihrer {@code <Koerper>Blocks}-Klasse auf.
 */
public final class PlanetOres {

    private PlanetOres() {}

    /** Eine Koerper-Variante: Original-Erz-ID ({@code ore_lithium}), Koerper ({@code mun}). */
    public record Variant(String ore, String body, RegistrySupplier<Block> block) {
        public String id() {
            return PlanetOres.id(ore, body);
        }
    }

    private static final Map<String, Variant> VARIANTS = new LinkedHashMap<>();
    private static final List<RegistrySupplier<? extends Block>> PICKAXE = new ArrayList<>();

    public static String id(String ore, String body) {
        return ore + "_" + body;
    }

    /**
     * Standard-Steinwerte des Originals: {@code setHardness(h).setResistance(r)} (1.7.10-Widerstand; in 1.20 wie
     * HBM-Modernized {@code r * 0.6}, weil 1.7.10 intern {@code r * 3} speichert und Explosionen durch 5 teilen).
     */
    public static BlockBehaviour.Properties rock(float hardness, float resistance1710) {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops()
                .strength(hardness, resistance1710 * 0.6F)
                .sound(SoundType.STONE);
    }

    /** Variante mit einfachem Block. */
    public static RegistrySupplier<Block> ore(String ore, String body, float hardness, float resistance1710) {
        return ore(ore, body, p -> new Block(p), hardness, resistance1710);
    }

    /** Variante mit eigener Blockklasse (z.B. Uran = {@code OutgasBlock}). */
    public static RegistrySupplier<Block> ore(String ore, String body, Function<BlockBehaviour.Properties, Block> factory, float hardness, float resistance1710) {
        String id = id(ore, body);
        RegistrySupplier<Block> reg = SpaceBlocks.registerWithItem(id, () -> factory.apply(rock(hardness, resistance1710)), SpaceCreativeTabs.Tab.BLOCKS);
        VARIANTS.put(id, new Variant(ore, body, reg));
        PICKAXE.add(reg);
        return reg;
    }

    /** Markiert einen Block fuer {@code minecraft:mineable/pickaxe} (Datagen {@code SpaceTagProviders}). */
    public static <T extends Block> RegistrySupplier<T> pickaxe(RegistrySupplier<T> block) {
        PICKAXE.add(block);
        return block;
    }

    @Nullable
    public static Variant get(String ore, String body) {
        return VARIANTS.get(id(ore, body));
    }

    public static List<Variant> variants() {
        return List.copyOf(VARIANTS.values());
    }

    /** Alle Bloecke des Dimensions-Frameworks, die mit der Spitzhacke abgebaut werden. */
    public static List<RegistrySupplier<? extends Block>> pickaxeBlocks() {
        return Collections.unmodifiableList(PICKAXE);
    }
}
