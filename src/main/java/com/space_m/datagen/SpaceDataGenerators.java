//? if forge {
package com.space_m.datagen;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.space_m.SpaceMod;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Datagen des Addons (nur 1.20.1-Forge, wie HBM-Modernized; NeoForge nimmt {@code src/generated/resources} und
 * stellt Pfade/Formate in {@code processResources} um). Lauf: {@code :1.20.1-forge:runData}.
 *
 * <p>Provider: Lang en_us/ru_ru ({@link SpaceLang} + {@link SpaceLanguageProvider}), Blockstates/Blockmodelle,
 * Item-Modelle, Block-Loot, Block-/Item-/Entity-Tags. Fehlende Texturen von HBM-Modernized werden ueber
 * {@code --existing-mod hbm_m} gefunden (build.forge.gradle.kts).
 */
@Mod.EventBusSubscriber(modid = SpaceMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class SpaceDataGenerators {

    private SpaceDataGenerators() {}

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator gen = event.getGenerator();
        PackOutput out = gen.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookup = event.getLookupProvider();
        ExistingFileHelper efh = event.getExistingFileHelper();

        SpaceTagProviders.Blocks blockTags = new SpaceTagProviders.Blocks(out, lookup, efh);
        gen.addProvider(event.includeServer(), blockTags);
        gen.addProvider(event.includeServer(), new SpaceTagProviders.Items(out, lookup, blockTags.contentsGetter(), efh));
        gen.addProvider(event.includeServer(), new SpaceTagProviders.Entities(out, lookup, efh));
        gen.addProvider(event.includeServer(), new LootTableProvider(out, Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(SpaceBlockLootProvider::new, LootContextParamSets.BLOCK))));

        gen.addProvider(event.includeClient(), new SpaceModelProviders.BlockStates(out, efh));
        gen.addProvider(event.includeClient(), new SpaceModelProviders.ItemModels(out, efh));
        gen.addProvider(event.includeClient(), new SpaceLanguageProvider(out, "en_us", false));
        gen.addProvider(event.includeClient(), new SpaceLanguageProvider(out, "ru_ru", true));
    }
}
//?}
