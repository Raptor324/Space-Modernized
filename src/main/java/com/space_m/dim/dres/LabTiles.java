package com.space_m.dim.dres;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.world.gen.nbt.BlockSelector;

import net.minecraft.util.RandomSource;

/**
 * 1:1 {@code com.hbm.world.gen.component.Component.LabTiles} von NTM Space (mit Rissanteil; HBM-Modernized hat nur
 * die 1.7.10-Fassung ohne Parameter, paketintern): {@code tile_lab}, {@code tile_lab_cracked} bis 0.9,
 * {@code tile_lab_broken} darueber.
 */
public class LabTiles extends BlockSelector {

    private final float crackFrequency;

    public LabTiles() {
        this(0.5F);
    }

    public LabTiles(float crackFrequency) {
        this.crackFrequency = 1 - crackFrequency;
    }

    @Override
    public void selectBlocks(RandomSource rand, int posX, int posY, int posZ, boolean notInterior) {
        float chance = rand.nextFloat();

        if (chance < crackFrequency) {
            this.selected = ModBlocks.TILE_LAB.get().defaultBlockState();
        } else if (chance < 0.9F) {
            this.selected = ModBlocks.TILE_LAB_CRACKED.get().defaultBlockState();
        } else {
            this.selected = ModBlocks.TILE_LAB_BROKEN.get().defaultBlockState();
        }
    }
}
