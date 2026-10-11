//? if forge {
package com.space_m.datagen;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.space_m.block.SpaceBlocks;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;

/**
 * Block-Loot des Addons. Standard: {@link SpaceBlocks#dropSelf()}; Sonderfaelle (Erze mit Fortune,
 * Original-{@code getItemDropped}/{@code quantityDropped}) als eigene Methoden in {@link #generate()} und
 * Block in {@link #getKnownBlocks()} aufnehmen. Nur hier gelistete Bloecke brauchen eine Loot-Tabelle.
 */
public final class SpaceBlockLootProvider extends BlockLootSubProvider {

    public SpaceBlockLootProvider() {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags());
    }

    @Override
    protected void generate() {
        for (RegistrySupplier<Block> b : SpaceBlocks.dropSelf()) {
            dropSelf(b.get());
        }
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        List<Block> known = new ArrayList<>();
        for (RegistrySupplier<Block> b : SpaceBlocks.dropSelf()) known.add(b.get());
        return known;
    }
}
//?}
