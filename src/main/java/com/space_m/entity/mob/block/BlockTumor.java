package com.space_m.entity.mob.block;

import net.minecraft.world.level.block.Block;

/**
 * 1:1 {@code ModBlocks.tumor} ({@code BlockTint(Material.clay, "tumor_base2", "tumor_base3", "fleshv2")}): Fleischmasse
 * der Laythe-Fleischhoehlen, Spawnboden des Fleisch-Creepers ({@code ChunkProviderLaythe.getPossibleCreatures}).
 *
 * <p>{@code BlockTint.getIcon} waehlt je Position eine der vier Texturen ({@code tumor_base}, {@code tumor_base2},
 * {@code tumor_base3}, {@code fleshv2}) per Koordinaten-Hash; hier ueber gleichgewichtete Zufallsmodelle im Blockstate
 * (Vanilla-Positions-Hash statt des Original-Hashes - gleiche Verteilung, andere Anordnung). {@code colorMultiplier}
 * ist bei Metadatum 0 weiss, also keine Faerbung.
 */
public class BlockTumor extends Block {

    public BlockTumor(Properties props) {
        super(props);
    }
}
