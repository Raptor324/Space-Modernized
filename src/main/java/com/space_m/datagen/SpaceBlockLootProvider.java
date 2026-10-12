// neo-pendant: Datagen laeuft nur auf Forge; NeoForge nutzt dieselben src/generated-Daten (processResources)
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
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

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
        // R7-A: Raumofen (Original BlockFurnace.getItemDropped = Vanilla-Ofen)
        add(com.space_m.machine.misc.MiscBlocks.FURNACE.get(), createSingleItemTable(net.minecraft.world.item.Items.FURNACE));
        add(com.space_m.machine.misc.MiscBlocks.LIT_FURNACE.get(), createSingleItemTable(net.minecraft.world.item.Items.FURNACE));
        // R7-B: Original IPersistentNBT (Tanks wandern mit in den Drop)
        for (RegistrySupplier<Block> b : SpaceBlocks.dropPersistent()) {
            Block block = b.get();
            add(block, LootTable.lootTable().withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
                    .add(applyExplosionDecay(block, LootItem.lootTableItem(block).apply(com.hbm_m.loot.PersistentNbtFunction.persistent())))));
        }
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        List<Block> known = new ArrayList<>();
        for (RegistrySupplier<Block> b : SpaceBlocks.dropSelf()) known.add(b.get());
        for (RegistrySupplier<Block> b : SpaceBlocks.dropPersistent()) known.add(b.get());
        known.add(com.space_m.machine.misc.MiscBlocks.FURNACE.get()); // R7-A
        known.add(com.space_m.machine.misc.MiscBlocks.LIT_FURNACE.get());
        return known;
    }
}
//?}
