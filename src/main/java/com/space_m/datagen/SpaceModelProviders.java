// neo-pendant: Datagen laeuft nur auf Forge; NeoForge nutzt dieselben src/generated-Daten (processResources)
//? if forge {
package com.space_m.datagen;

import com.space_m.SpaceMod;
import com.space_m.block.SpaceBlocks;
import com.space_m.item.SpaceItems;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

/**
 * Blockstates/Block-Modelle und Item-Modelle fuer die als "einfach" markierten Eintraege
 * ({@link SpaceBlocks#cube}, {@link SpaceItems#basic}). Alles andere (OBJ-Modelle, Mehrblock-Maschinen,
 * Varianten) bekommt handgeschriebene JSONs unter {@code src/main/resources} oder eigene Methoden hier.
 */
public final class SpaceModelProviders {
    private SpaceModelProviders() {}

    public static final class BlockStates extends BlockStateProvider {
        public BlockStates(PackOutput output, ExistingFileHelper efh) {
            super(output, SpaceMod.MOD_ID, efh);
        }

        @Override
        protected void registerStatesAndModels() {
            for (RegistrySupplier<Block> b : SpaceBlocks.simpleCubes()) {
                simpleBlockWithItem(b.get(), cubeAll(b.get()));
            }
        }
    }

    public static final class ItemModels extends ItemModelProvider {
        public ItemModels(PackOutput output, ExistingFileHelper efh) {
            super(output, SpaceMod.MOD_ID, efh);
        }

        @Override
        protected void registerModels() {
            for (RegistrySupplier<Item> i : SpaceItems.generatedModels()) {
                basicItem(i.get());
            }
        }
    }
}
//?}
