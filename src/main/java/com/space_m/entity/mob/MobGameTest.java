package com.space_m.entity.mob;

import java.util.List;

import com.space_m.SpaceMod;
import com.space_m.dim.SpaceDimensions;
import com.space_m.lib.SpaceTags;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
//? if forge {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?} elif neoforge {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?}

/**
 * R8-A: Space-Mobs - Spawnlisten je Planet/Biom (1:1 Original-Gewichte), Fleisch-Creeper nur auf {@code tumor} in
 * Laythe, Attribute/Groessen, Erstickungs-Immunitaet, Fleisch-Explosion verwandelt Bloecke in {@code tumor}.
 */
@GameTestHolder(SpaceMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MobGameTest {

    private MobGameTest() {}

    private static void check(boolean cond, String msg) {
        if (!cond) throw new GameTestAssertException(msg);
    }

    private static void spawn(GameTestHelper helper, String biome, MobCategory cat, EntityType<?> type, int weight, int min, int max) {
        Biome b = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME).get(ResourceKey.create(Registries.BIOME, SpaceMod.id(biome)));
        check(b != null, "biome " + biome);
        List<MobSpawnSettings.SpawnerData> list = b.getMobSettings().getMobs(cat).unwrap();
        for (MobSpawnSettings.SpawnerData d : list) {
            if (d.type == type) {
                check(d.getWeight().asInt() == weight && d.minCount == min && d.maxCount == max,
                        biome + ": " + EntityType.getKey(type) + " " + d.getWeight().asInt() + "/" + d.minCount + "/" + d.maxCount);
                return;
            }
        }
        throw new GameTestAssertException(biome + ": kein Spawn " + EntityType.getKey(type));
    }

    @GameTest(template = "empty3x3x3", batch = "space_mobs", timeoutTicks = 100)
    public static void spawnsPerPlanet(GameTestHelper helper) {
        spawn(helper, "mun", MobCategory.CREATURE, SpaceMobs.MOON_COW.get(), 10, 4, 4);
        spawn(helper, "thatmo", MobCategory.CREATURE, SpaceMobs.TANKBOT.get(), 6, 1, 1);
        spawn(helper, "thatmo", MobCategory.CREATURE, SpaceMobs.MOON_COW.get(), 10, 1, 1);
        for (String b : new String[] {"laythe_islands", "laythe_ocean", "laythe_polar", "laythe_coast"}) {
            spawn(helper, b, MobCategory.WATER_CREATURE, SpaceMobs.SCUTTERFISH.get(), 10, 4, 4);
            spawn(helper, b, MobCategory.WATER_CREATURE, SpaceMobs.SIFTER_EEL.get(), 10, 1, 3);
            check(helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME).get(ResourceKey.create(Registries.BIOME, SpaceMod.id(b)))
                    .getGenerationSettings().features().stream().anyMatch(set -> set.stream()
                            .anyMatch(h -> h.unwrapKey().map(k -> k.location().equals(SpaceMod.id("laythe/cave_tumor"))).orElse(false))),
                    b + ": Fleischhoehle (laythe/cave_tumor) fehlt");
        }
        spawn(helper, "laythe_ocean", MobCategory.WATER_CREATURE, SpaceMobs.SCRAP_FISH.get(), 2, 1, 4);
        spawn(helper, "laythe_ocean", MobCategory.WATER_CREATURE, SpaceMobs.DEPTH_SQUID.get(), 1, 1, 4);
        spawn(helper, "laythe_coast", MobCategory.WATER_CREATURE, SpaceMobs.SCUTTLECRAB.get(), 4, 1, 3);

        // Platzierungsregeln: Chunk-Erzeugung ohne getCanSpawnHere, Tiefseekalmar nur Y 46-62
        ServerLevel level = helper.getLevel();
        BlockPos p = helper.absolutePos(new BlockPos(1, 1, 1));
        check(SpawnPlacements.checkSpawnRules(SpaceMobs.MOON_COW.get(), level, MobSpawnType.CHUNK_GENERATION, p, level.random), "Mun-Kuh bei Chunk-Erzeugung");
        check(SpawnPlacements.checkSpawnRules(SpaceMobs.TANKBOT.get(), level, MobSpawnType.CHUNK_GENERATION, p, level.random), "Tankbot bei Chunk-Erzeugung");
        check(!SpawnPlacements.checkSpawnRules(SpaceMobs.DEPTH_SQUID.get(), level, MobSpawnType.NATURAL, p.atY(100), level.random), "Tiefseekalmar ueber Y 63");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "space_mobs", timeoutTicks = 100)
    public static void fleshOnTumorOnlyOnLaythe(GameTestHelper helper) {
        ServerLevel over = helper.getLevel();
        BlockPos p = helper.absolutePos(new BlockPos(1, 2, 1));
        helper.setBlock(new BlockPos(1, 1, 1), SpaceMobs.TUMOR.get());
        check(!MobSpawns.isFleshSpot(over, MobCategory.MONSTER, p), "Fleisch-Spawn ausserhalb Laythe");

        ServerLevel laythe = over.getServer().getLevel(SpaceDimensions.LAYTHE);
        if (laythe != null) {
            BlockPos lp = new BlockPos(0, 200, 0);
            laythe.setBlock(lp.below(), SpaceMobs.TUMOR.get().defaultBlockState(), 3);
            check(MobSpawns.isFleshSpot(laythe, MobCategory.MONSTER, lp), "Laythe: Monster auf tumor -> Fleisch-Creeper");
            check(!MobSpawns.isFleshSpot(laythe, MobCategory.CREATURE, lp), "Laythe: nur Monster-Kategorie");
            laythe.setBlock(lp.below(), Blocks.STONE.defaultBlockState(), 3);
            check(!MobSpawns.isFleshSpot(laythe, MobCategory.MONSTER, lp), "Laythe: auf Stein kein Fleisch-Creeper");
            laythe.setBlock(lp.below(), Blocks.AIR.defaultBlockState(), 3);
        }
        check(MobSpawns.fleshSpawn().type == SpaceMobs.CREEPER_FLESH.get(), "Fleisch-Creeper 10/4/4");
        helper.succeed();
    }

    private static void attr(LivingEntity e, String name, double hp) {
        check(Math.abs(e.getAttributeBaseValue(Attributes.MAX_HEALTH) - hp) < 1e-6, name + " Leben " + e.getAttributeBaseValue(Attributes.MAX_HEALTH));
    }

    @GameTest(template = "empty3x3x3", batch = "space_mobs", timeoutTicks = 100)
    public static void attributes(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        EntityTankbot tank = SpaceMobs.TANKBOT.get().create(level);
        attr(tank, "Tankbot", 180);
        check(tank.getAttributeBaseValue(Attributes.FOLLOW_RANGE) == 60 && tank.getAttributeBaseValue(Attributes.ATTACK_DAMAGE) == 15
                && tank.getAttributeBaseValue(Attributes.KNOCKBACK_RESISTANCE) == 1, "Tankbot Folgeweite/Schaden/Rueckstoss");
        check(tank.fireImmune() && tank.deathTime == -50, "Tankbot feuerfest, Todeszeit -50");
        check(Math.abs(tank.getBbWidth() - 2.45F) < 1e-4 && Math.abs(tank.getBbHeight() - 3.25F) < 1e-4, "Tankbot 2.45x3.25");
        EntityBFAngel angel = SpaceMobs.BF_ANGEL.get().create(level);
        attr(angel, "BFAngel", 10000);
        check(angel.fireImmune() && angel.deathTime == -30, "BFAngel feuerfest, Todeszeit -30");
        attr(SpaceMobs.MOON_COW.get().create(level), "Mun-Kuh", 10);
        attr(SpaceMobs.SCUTTERFISH.get().create(level), "Scutterfish", 15);
        attr(SpaceMobs.SCRAP_FISH.get().create(level), "Scrapfish", 15);
        attr(SpaceMobs.SIFTER_EEL.get().create(level), "Sifter-Aal", 15);
        check(SpaceMobs.SIFTER_EEL.get().create(level).getAttributeBaseValue(Attributes.FOLLOW_RANGE) == 10, "Fisch Folgeweite 10");
        attr(SpaceMobs.SCUTTLECRAB.get().create(level), "Scuttlecrab", 20);
        attr(SpaceMobs.DEPTH_SQUID.get().create(level), "Tiefseekalmar", 10);
        attr(SpaceMobs.CREEPER_FLESH.get().create(level), "Fleisch-Creeper", 20);

        for (String id : SpaceMobs.SUFFOCATION_IMMUNE) {
            EntityType<?> t = EntityType.byString(id).orElse(null);
            if (t == null) continue; // Siege-UFO/Behemoth registriert R8-B
            check(t.is(SpaceTags.SUFFOCATION_IMMUNE), id + " im Tag suffocation_immune");
        }
        check(SpaceMobs.MICRO_UFO.get().create(level) instanceof com.hbm_m.entity.mob.siege.EntitySiegeCraft, "Macro-UFO = SiegeCraft");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "space_mobs", timeoutTicks = 100)
    public static void fleshCreeperMakesTumor(GameTestHelper helper) {
        for (int x = -2; x <= 4; x++) for (int y = 0; y <= 3; y++) for (int z = -2; z <= 4; z++)
            helper.setBlock(new BlockPos(x, y, z), Blocks.DIRT);
        helper.setBlock(new BlockPos(1, 2, 1), Blocks.AIR);
        helper.setBlock(new BlockPos(1, 3, 1), Blocks.AIR);
        EntityCreeperFlesh creeper = helper.spawn(SpaceMobs.CREEPER_FLESH.get(), new BlockPos(1, 2, 1));
        creeper.fleshExplode();
        int tumor = 0;
        for (int x = -2; x <= 4; x++) for (int y = 0; y <= 3; y++) for (int z = -2; z <= 4; z++)
            if (helper.getBlockState(new BlockPos(x, y, z)).is(SpaceMobs.TUMOR.get())) tumor++;
        check(tumor > 0, "Fleisch-Explosion: keine tumor-Bloecke");
        check(creeper.isRemoved(), "Creeper nach der Explosion entfernt");
        helper.succeed();
    }
}
