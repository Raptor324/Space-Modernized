package com.space_m.dim.feature;

import java.util.Optional;
import java.util.Random;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.space_m.dim.legacy.LegacyWorld;
import com.space_m.dim.legacy.NoiseGeneratorPerlin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * 1:1 die Space-Fassung von {@code com.hbm.world.feature.OreLayer3D} als Datapack-Feature
 * ({@code space_m:ore_layer_3d}): Erzlagen aus drei Perlin-Feldern zwischen y 6 und 64, ersetzt nur
 * Planetengestein ({@code WorldProviderCelestial.getStone()} = {@code target}).
 *
 * <p>Original: {@code DecorateBiomeEvent.Pre}-Handler, einmal je Chunk und Dimension (Liste
 * {@code alreadyDecorated}); hier ein Feature des Bioms, das genau einmal je Chunk laeuft. Der Rausch-Seed haengt
 * am Laufindex {@code id} der Instanz ({@code OreLayer3D.counter} in Konstruktionsreihenfolge der
 * {@code MainRegistry}: Haematit 0, Bauxit 1, Malachit 2, Mun-Konglomerat 3, Ike-Konglomerat 4, Minmus-Regolith 5,
 * Minmus-Glatt 6, Minmus-Konglomerat 7 - bei abgeschalteten globalen Lagen verschieben sich die Nummern wie im
 * Original, siehe {@code id_after}). Der Original-Fehler (nz liest ebenfalls cacheX) bleibt erhalten.
 *
 * <p>JSON: {@code {"block": {...}, "target": {...}, "id": 5, "scale_h": 0.06, "scale_v": 0.25, "threshold": 220,
 * "enabled": "hbm_m:enableHematite", "id_after": ["hbm_m:enableHematite", ...]}}.
 */
public class OreLayer3DFeature extends Feature<OreLayer3DFeature.Config> {

    public OreLayer3DFeature() {
        super(Config.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> ctx) {
        Config c = ctx.config();
        if (c.enabled.isPresent() && !ConfigFlag.get(c.enabled.get())) return false;

        WorldGenLevel world = ctx.level();
        long seed = world.getSeed();
        int id = c.id;
        for (String flag : c.idAfter) if (!ConfigFlag.get(flag)) id--;

        NoiseGeneratorPerlin noiseX = new NoiseGeneratorPerlin(new Random(seed + 101 + id), 4);
        NoiseGeneratorPerlin noiseY = new NoiseGeneratorPerlin(new Random(seed + 102 + id), 4);
        // noiseZ (seed + 103 + id) wird im Original erzeugt, aber nie gelesen

        int cx = ctx.origin().getX() & ~15;
        int cz = ctx.origin().getZ() & ~15;

        double[][] cacheX = new double[16][65];
        for (int o = 0; o < 16; o++) {
            for (int y = 64; y > 5; y--) {
                cacheX[o][y] = noiseX.func_151601_a(y * c.scaleV, (cz + 8 + o) * c.scaleH);
            }
        }

        boolean placed = false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int ox = 0; ox < 16; ox++) {
            int x = cx + 8 + ox;

            for (int oz = 0; oz < 16; oz++) {
                int z = cz + 8 + oz;

                double ny = noiseY.func_151601_a(x * c.scaleH, z * c.scaleH);

                for (int y = 64; y > 5; y--) {
                    double nx = cacheX[oz][y];
                    double nz = cacheX[ox][y];

                    if (nx * ny * nz > c.threshold) {
                        pos.set(x, y, z);
                        if (!LegacyWorld.inBounds(world, pos)) continue;
                        BlockState target = world.getBlockState(pos);

                        if (target.isSolidRender(world, pos) && target.getBlock() == c.target.getBlock()) {
                            world.setBlock(pos, c.block, 2);
                            placed = true;
                        }
                    }
                }
            }
        }
        return placed;
    }

    public static final class Config implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockState.CODEC.fieldOf("block").forGetter(c -> c.block),
                BlockState.CODEC.fieldOf("target").forGetter(c -> c.target),
                Codec.INT.fieldOf("id").forGetter(c -> c.id),
                Codec.DOUBLE.fieldOf("scale_h").forGetter(c -> c.scaleH),
                Codec.DOUBLE.fieldOf("scale_v").forGetter(c -> c.scaleV),
                Codec.DOUBLE.fieldOf("threshold").forGetter(c -> c.threshold),
                Codec.STRING.optionalFieldOf("enabled").forGetter(c -> c.enabled),
                Codec.STRING.listOf().optionalFieldOf("id_after", java.util.List.of()).forGetter(c -> c.idAfter)
        ).apply(i, Config::new));

        public final BlockState block;
        /** Planetengestein ({@code WorldProviderCelestial.getStone()}). */
        public final BlockState target;
        /** {@code OreLayer3D.id} mit allen globalen Lagen eingeschaltet. */
        public final int id;
        public final double scaleH;
        public final double scaleV;
        public final double threshold;
        /** Optionaler Schalter ({@code WorldConfig.enableHematite} ...). */
        public final Optional<String> enabled;
        /** Schalter der vorher konstruierten, abschaltbaren Lagen ({@code id} sinkt je abgeschaltetem). */
        public final java.util.List<String> idAfter;

        public Config(BlockState block, BlockState target, int id, double scaleH, double scaleV, double threshold, Optional<String> enabled, java.util.List<String> idAfter) {
            this.block = block;
            this.target = target;
            this.id = id;
            this.scaleH = scaleH;
            this.scaleV = scaleV;
            this.threshold = threshold;
            this.enabled = enabled;
            this.idAfter = idAfter;
        }
    }
}
