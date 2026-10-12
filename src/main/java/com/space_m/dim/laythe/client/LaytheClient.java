package com.space_m.dim.laythe.client;

import com.space_m.dim.laythe.LaytheBlocks;
import com.space_m.dim.laythe.block.BlockFallingTint;

import dev.architectury.registry.client.rendering.ColorHandlerRegistry;

/** R9-B: Blockfarbe des Laythe-Schlicks ({@code BlockFallingTint.colorMultiplier}, Schlickhoehlen-Schichten). */
public final class LaytheClient {

    private LaytheClient() {}

    public static void init() {
        ColorHandlerRegistry.registerBlockColors((state, level, pos, tintIndex) -> BlockFallingTint.color(state),
                LaytheBlocks.LAYTHE_SILT);
    }
}
