package com.space_m.test;

// R9-C: Nur NeoForge 1.21.1. Der GameTestServer baut seine Welt aus dem Flachland-Preset mit leerer LEVEL_STEM-Liste
// (WorldDimensions.bake) und verwirft so die Datapack-Dimensionen des Addons. Mit -Dspace_m.gametestDimensions=true
// (nur runs.gameTestServer in build.neoforge.gradle.kts) meldet das Addon den eingebauten Pack "gametest_dimensions"
// an; dessen minecraft:flat-Preset enthaelt alle space_m-Dimensionen (erzeugt in processResources). Normale Welten und
// Server sind nicht betroffen (Schalter fehlt). Forge 1.20.1 laedt die Datapack-Dimensionen im GameTestServer bereits.
//? if neoforge {
/*import com.space_m.SpaceMod;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;

@net.neoforged.fml.common.EventBusSubscriber(modid = SpaceMod.MOD_ID, bus = net.neoforged.fml.common.EventBusSubscriber.Bus.MOD)
public final class GameTestDimensionsPack {

    public static final String PROPERTY = "space_m.gametestDimensions";

    private GameTestDimensionsPack() {}

    @net.neoforged.bus.api.SubscribeEvent
    public static void onAddPackFinders(net.neoforged.neoforge.event.AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA || !Boolean.getBoolean(PROPERTY)) return;
        event.addPackFinders(ResourceLocation.fromNamespaceAndPath(SpaceMod.MOD_ID, "gametest_dimensions"), PackType.SERVER_DATA,
                Component.literal("space_m GameTest dimensions"), PackSource.BUILT_IN, true, Pack.Position.TOP);
    }
}
*///?}
