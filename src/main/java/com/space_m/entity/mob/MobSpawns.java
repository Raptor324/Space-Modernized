package com.space_m.entity.mob;

import java.util.List;

import com.space_m.SpaceMod;
import com.space_m.config.SpaceConfig;
import com.space_m.dim.SpaceDimensions;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;

/**
 * Spawn-Regeln der Space-Mobs (Original: {@code BiomeGenBase.SpawnListEntry} in den Planeten-Biomen,
 * {@code ChunkProviderLaythe.getPossibleCreatures}, {@code getCanSpawnHere} der Klassen).
 *
 * <p>Die Spawnlisten selbst stehen 1:1 in den Biom-JSONs ({@code spawners}): Mun Mun-Kuh 10/4/4 (creature);
 * Thatmo Tankbot 6/1/1, Mun-Kuh 10/1/1 (creature; War Behemoth 2/1/1 meldet R8-B per BiomeModifications an); alle Laythe-Biome Scutterfish 10/4/4 und
 * Sifter-Aal 10/1/3 (water_creature), Sagan-See zusaetzlich Scrapfish 2/1/4, Sifter-Aal 1/1/4, Tiefseekalmar 1/1/4,
 * Riff Scuttlecrab 4/1/3. Hier:
 * <ul>
 *   <li>Platzierungsregeln: 1.7.10 prueft bei der Chunk-Erzeugung ({@code performWorldGenSpawning}) nur den Platz,
 *       nicht {@code getCanSpawnHere} -> {@code CHUNK_GENERATION} immer erlaubt; danach die Klassenregeln (Kuh: Gras +
 *       Licht > 8, Monster: Dunkelheit, Wassertiere: im Wasser, Tiefseekalmar zusaetzlich Y 46-62).</li>
 *   <li>{@code MobConfig.enableMunCows = false}: keine Kuehe im Mun-Biom (Original: Eintrag fehlt dann).</li>
 *   <li>Laythe: Monster nur auf {@code tumor}, dort ausschliesslich Fleisch-Creeper 10/4/4 (Loader-Ereignis
 *       {@code LevelEvent.PotentialSpawns}, beide Loader).</li>
 * </ul>
 */
public final class MobSpawns {

    private MobSpawns() {}

    public static final ResourceKey<Biome> MUN_BIOME = ResourceKey.create(Registries.BIOME, SpaceMod.id("mun"));

    public static void init() {
        // Ereignisse ueber @EventBusSubscriber (unten); nur Klasse laden.
    }

    /** Kuh: Chunk-Erzeugung immer, sonst {@code EntityAnimal.getCanSpawnHere}; Mun nur mit {@code enableMunCows}. */
    public static boolean checkMoonCow(EntityType<EntityMoonCow> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
        if (!SpaceConfig.get().enableMunCows && level.getBiome(pos).is(MUN_BIOME)) return false;
        if (reason == MobSpawnType.CHUNK_GENERATION) return true;
        return Animal.checkAnimalSpawnRules(type, level, reason, pos, random);
    }

    /** Thatmo-Kreaturen (Monster in der creature-Liste): Chunk-Erzeugung immer, sonst {@code EntityMob.getCanSpawnHere}. */
    public static <T extends Monster> boolean checkThatmoMonster(EntityType<T> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
        if (reason == MobSpawnType.CHUNK_GENERATION) return true;
        return Monster.checkMonsterSpawnRules(type, level, reason, pos, random);
    }

    /** {@code ChunkProviderLaythe.getPossibleCreatures}: Monster auf {@code tumor} in Laythe -> nur Fleisch-Creeper 10/4/4. */
    public static boolean isFleshSpot(LevelAccessor level, MobCategory category, BlockPos pos) {
        if (category != MobCategory.MONSTER) return false;
        if (!(level instanceof ServerLevel sl) || sl.dimension() != SpaceDimensions.LAYTHE) return false;
        return level.getBlockState(pos.below()).is(SpaceMobs.TUMOR.get());
    }

    public static MobSpawnSettings.SpawnerData fleshSpawn() {
        return new MobSpawnSettings.SpawnerData(SpaceMobs.CREEPER_FLESH.get(), 10, 4, 4);
    }

    //? if forge {
    @net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = SpaceMod.MOD_ID, bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModBus {
        @net.minecraftforge.eventbus.api.SubscribeEvent
        public static void onSpawnPlacements(net.minecraftforge.event.entity.SpawnPlacementRegisterEvent event) {
            var op = net.minecraftforge.event.entity.SpawnPlacementRegisterEvent.Operation.REPLACE;
            var ground = net.minecraft.world.entity.SpawnPlacements.Type.ON_GROUND;
            var water = net.minecraft.world.entity.SpawnPlacements.Type.IN_WATER;
            var hm = net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES;
            event.register(SpaceMobs.MOON_COW.get(), ground, hm, MobSpawns::checkMoonCow, op);
            event.register(SpaceMobs.TANKBOT.get(), ground, hm, MobSpawns::checkThatmoMonster, op);
            event.register(SpaceMobs.CREEPER_FLESH.get(), ground, hm, Monster::checkMonsterSpawnRules, op);
            event.register(SpaceMobs.SCUTTERFISH.get(), water, hm, EntityFish::checkFishSpawnRules, op);
            event.register(SpaceMobs.SIFTER_EEL.get(), water, hm, EntityFish::checkFishSpawnRules, op);
            event.register(SpaceMobs.SCRAP_FISH.get(), water, hm, EntityFish::checkFishSpawnRules, op);
            event.register(SpaceMobs.SCUTTLECRAB.get(), water, hm, EntityFish::checkFishSpawnRules, op);
            event.register(SpaceMobs.DEPTH_SQUID.get(), water, hm, EntityDepthSquid::checkDepthSquidSpawnRules, op);
        }
    }

    @net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = SpaceMod.MOD_ID, bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.FORGE)
    public static final class GameBus {
        @net.minecraftforge.eventbus.api.SubscribeEvent
        public static void onPotentialSpawns(net.minecraftforge.event.level.LevelEvent.PotentialSpawns event) {
            if (!isFleshSpot(event.getLevel(), event.getMobCategory(), event.getPos())) return;
            for (MobSpawnSettings.SpawnerData d : List.copyOf(event.getSpawnerDataList())) event.removeSpawnerData(d);
            event.addSpawnerData(fleshSpawn());
        }
    }
    //?} elif neoforge {
    /*@net.neoforged.fml.common.EventBusSubscriber(modid = SpaceMod.MOD_ID, bus = net.neoforged.fml.common.EventBusSubscriber.Bus.MOD)
    public static final class ModBus {
        @net.neoforged.bus.api.SubscribeEvent
        public static void onSpawnPlacements(net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent event) {
            var op = net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE;
            var ground = net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND;
            var water = net.minecraft.world.entity.SpawnPlacementTypes.IN_WATER;
            var hm = net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES;
            event.register(SpaceMobs.MOON_COW.get(), ground, hm, MobSpawns::checkMoonCow, op);
            event.register(SpaceMobs.TANKBOT.get(), ground, hm, MobSpawns::checkThatmoMonster, op);
            event.register(SpaceMobs.CREEPER_FLESH.get(), ground, hm, Monster::checkMonsterSpawnRules, op);
            event.register(SpaceMobs.SCUTTERFISH.get(), water, hm, EntityFish::checkFishSpawnRules, op);
            event.register(SpaceMobs.SIFTER_EEL.get(), water, hm, EntityFish::checkFishSpawnRules, op);
            event.register(SpaceMobs.SCRAP_FISH.get(), water, hm, EntityFish::checkFishSpawnRules, op);
            event.register(SpaceMobs.SCUTTLECRAB.get(), water, hm, EntityFish::checkFishSpawnRules, op);
            event.register(SpaceMobs.DEPTH_SQUID.get(), water, hm, EntityDepthSquid::checkDepthSquidSpawnRules, op);
        }
    }

    @net.neoforged.fml.common.EventBusSubscriber(modid = SpaceMod.MOD_ID)
    public static final class GameBus {
        @net.neoforged.bus.api.SubscribeEvent
        public static void onPotentialSpawns(net.neoforged.neoforge.event.level.LevelEvent.PotentialSpawns event) {
            if (!isFleshSpot(event.getLevel(), event.getMobCategory(), event.getPos())) return;
            for (MobSpawnSettings.SpawnerData d : List.copyOf(event.getSpawnerDataList())) event.removeSpawnerData(d);
            event.addSpawnerData(fleshSpawn());
        }
    }
    *///?}
}
