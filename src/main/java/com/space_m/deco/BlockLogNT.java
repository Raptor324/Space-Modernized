package com.space_m.deco;

import net.minecraft.world.level.block.RotatedPillarBlock;

/**
 * 1:1 {@code com.hbm.blocks.generic.BlockLogNT}: Vanilla-{@code BlockLog} mit eigener Seiten-/Stirntextur
 * ({@code bf_log}, {@code eu_log}, {@code primed_log} = {@code <id>_side/_top}, {@code lattice_log} = {@code rad_log_side/_top}).
 * Achse wie Vanilla-Staemme ({@code AXIS} = Metadaten 0/4/8), Modelle {@code minecraft:block/cube_column(_horizontal)}.
 * Im Tag {@code minecraft:logs} (Original {@code BlockLog.canSustainLeaves/isWood}), aber nicht brennbar
 * (keine {@code Blocks.fire.setFireInfo}-Anmeldung im Original); Lava entzuendet ({@code Material.wood}).
 * Auch {@code pvc_log}/{@code vinyl_log} (Tekto) sind {@code BlockLogNT} - dort als {@code RotatedPillarBlock} registriert.
 */
// Original-Klasse: BlockLogNT
public class BlockLogNT extends RotatedPillarBlock {

    public BlockLogNT(Properties properties) {
        super(properties);
    }
}
