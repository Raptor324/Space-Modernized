package com.space_m.dim.legacy;

/** 1:1 {@code net.minecraft.world.gen.layer.GenLayerFuzzyZoom} (1.7.10): Zoom mit reiner Zufallswahl. */
public class GenLayerFuzzyZoom extends GenLayerZoom {

    public GenLayerFuzzyZoom(long seed, GenLayer parent) {
        super(seed, parent);
    }

    @Override
    protected int selectModeOrRandom(int a, int b, int c, int d) {
        return this.selectRandom(a, b, c, d);
    }
}
