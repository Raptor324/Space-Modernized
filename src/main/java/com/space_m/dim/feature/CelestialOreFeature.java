package com.space_m.dim.feature;

import java.util.Random;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.space_m.dim.legacy.LegacyWorld;
import com.space_m.dim.legacy.WorldGenMinable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * Erzadern der Himmelskoerper als Datapack-Feature ({@code space_m:celestial_ore}), 1:1 die beiden Verteilungen
 * des Originals:
 * <ul>
 *   <li>{@code "distribution": "toolbox"} = {@code DungeonToolbox.generateOre(world, rand, x, z, count, size,
 *       min_y, range, ore, meta, target)}: {@code count} Adern bei {@code x + rand(16)}, {@code min_y + rand(range)}.</li>
 *   <li>{@code "distribution": "vanilla"} = {@code WorldGeneratorCelestial.genVanillaOre(..., min_y, max_y=range,
 *       count, size, ore, target, meta)}: {@code y = rand(range - min_y) + min_y}.</li>
 * </ul>
 * {@code chance > 0}: nur mit Wahrscheinlichkeit 1/{@code chance} je Chunk (Alexandrit {@code alexandriteSpawn}).
 * {@code count}/{@code chance} koennen auf die Original-Konfiguration zeigen ({@link ConfigInt}). Platzierung ohne
 * Modifikatoren: Ursprung = Chunk-Ecke wie {@code IWorldGenerator.generate(chunkX * 16, chunkZ * 16)}.
 */
// Original-Klasse: WorldGeneratorCelestial (generateNTMOres/generateVanillaOres/genVanillaOre; Tiefenerze und
// Schluesselloch: DepthDepositFeature/KeyholeFeature; Bedrock-Erze -> R8, meteor_dungeon -> Wunsch W-B1)
public class CelestialOreFeature extends Feature<CelestialOreFeature.Config> {

    public CelestialOreFeature() {
        super(Config.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> ctx) {
        Config c = ctx.config();
        WorldGenLevel world = ctx.level();
        Random rand = LegacyWorld.random(ctx.random());
        BlockPos origin = ctx.origin();
        int x = origin.getX() & ~15;
        int z = origin.getZ() & ~15;

        if (c.chance != null) {
            int chance = c.chance.get();
            if (chance <= 0 || rand.nextInt(chance) != 0) return false;
        }

        int count = c.count.get();
        WorldGenMinable minable = new WorldGenMinable(c.ore, c.size, c.target);

        if (c.vanilla) {
            // genVanillaOre: y = rand(yMax - yMin) + yMin
            for (int l = 0; l < count; ++l) {
                int genX = x + rand.nextInt(16);
                int genY = rand.nextInt(c.range - c.minY) + c.minY;
                int genZ = z + rand.nextInt(16);
                minable.generate(world, rand, genX, genY, genZ);
            }
        } else {
            // DungeonToolbox.generateOre
            for (int i = 0; i < count; i++) {
                int genX = x + rand.nextInt(16);
                int genY = c.minY + (c.range > 0 ? rand.nextInt(c.range) : 0);
                int genZ = z + rand.nextInt(16);
                minable.generate(world, rand, genX, genY, genZ);
            }
        }
        return true;
    }

    public static final class Config implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockState.CODEC.fieldOf("ore").forGetter(c -> c.ore),
                BlockState.CODEC.fieldOf("target").forGetter(c -> c.target),
                ConfigInt.CODEC.fieldOf("count").forGetter(c -> c.count),
                Codec.INT.fieldOf("size").forGetter(c -> c.size),
                Codec.INT.fieldOf("min_y").forGetter(c -> c.minY),
                Codec.INT.fieldOf("range").forGetter(c -> c.range),
                Codec.STRING.optionalFieldOf("distribution", "toolbox").forGetter(c -> c.vanilla ? "vanilla" : "toolbox"),
                ConfigInt.CODEC.optionalFieldOf("chance").forGetter(c -> java.util.Optional.ofNullable(c.chance))
        ).apply(i, (ore, target, count, size, minY, range, dist, chance) -> new Config(ore, target, count, size, minY, range, "vanilla".equals(dist), chance.orElse(null))));

        public final BlockState ore;
        public final BlockState target;
        public final ConfigInt count;
        public final int size;
        public final int minY;
        /** toolbox: Streuung ({@code variance}); vanilla: {@code yMax}. */
        public final int range;
        public final boolean vanilla;
        public final ConfigInt chance;

        public Config(BlockState ore, BlockState target, ConfigInt count, int size, int minY, int range, boolean vanilla, ConfigInt chance) {
            this.ore = ore;
            this.target = target;
            this.count = count;
            this.size = size;
            this.minY = minY;
            this.range = range;
            this.vanilla = vanilla;
            this.chance = chance;
        }
    }
}
