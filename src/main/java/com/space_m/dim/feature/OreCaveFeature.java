package com.space_m.dim.feature;

import java.util.Optional;
import java.util.Random;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.space_m.dim.legacy.LegacyWorld;
import com.space_m.dim.legacy.NoiseGeneratorPerlin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * 1:1 die Space-Fassung von {@code com.hbm.world.feature.OreCave} als Datapack-Feature ({@code space_m:ore_cave}):
 * Erzhoehlen-Schicht nach 2D-Rauschen um {@code y_level}; das Erz waechst nur an Hohlraeume (optional mit
 * Fluessigkeitsbecken), in den Hohlraeumen haengen Tropfsteine des Erzes.
 *
 * <p>Ersetzt wird {@code override} bzw. das Planetengestein ({@code target} = {@code WorldProviderCelestial.getStone()}).
 * Rausch-Seed 1:1: {@code worldSeed + MetaBlock.hashCode() * 31 + yLevel} mit
 * {@code MetaBlock.hashCode() = 31 * (31 + "hbm:<block>".hashCode()) + meta} ({@code ore_name}/{@code ore_meta}
 * = Original-Registriername und Metadatum). Original: {@code DecorateBiomeEvent.Pre}; hier ein Biom-Feature.
 *
 * <p>{@code BlockStalagmite.getMetaFromResource}: die passende Tropfstein-Variante steht direkt im JSON
 * ({@code stalactite}/{@code stalagmite}); ohne beide Felder keine Tropfsteine ({@code setStalagmites(false)}).
 */
public class OreCaveFeature extends Feature<OreCaveFeature.Config> {

    public OreCaveFeature() {
        super(Config.CODEC);
    }

    private static boolean isStalagmite(BlockState state) {
        String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        return path.startsWith("stalactite") || path.startsWith("stalagmite");
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> ctx) {
        Config c = ctx.config();
        if (c.enabled.isPresent() && !ConfigFlag.get(c.enabled.get())) return false;

        WorldGenLevel world = ctx.level();
        Random rand = LegacyWorld.random(ctx.random());
        Block replace = c.target.getBlock();

        int hash = 31 * (31 + c.oreName.hashCode()) + c.oreMeta;
        NoiseGeneratorPerlin noise = new NoiseGeneratorPerlin(new Random(world.getSeed() + (hash * 31) + c.yLevel), 2);

        int cX = ctx.origin().getX() & ~15;
        int cZ = ctx.origin().getZ() & ~15;
        double scale = 0.01D;
        boolean placed = false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos npos = new BlockPos.MutableBlockPos();

        for (int x = cX + 8; x < cX + 24; x++) {
            for (int z = cZ + 8; z < cZ + 24; z++) {

                double n = noise.func_151601_a(x * scale, z * scale);

                if (n > c.threshold) {
                    int range = (int) ((n - c.threshold) * c.rangeMult);

                    if (range > c.maxRange)
                        range = (c.maxRange * 2) - range;

                    if (range < 0)
                        continue;

                    for (int y = c.yLevel - range; y <= c.yLevel + range; y++) {
                        pos.set(x, y, z);
                        if (!LegacyWorld.inBounds(world, pos)) continue;
                        BlockState genTarget = world.getBlockState(pos);

                        if (genTarget.isSolidRender(world, pos) && genTarget.getBlock() == replace) {

                            boolean shouldGen = false;
                            boolean canGenFluid = rand.nextBoolean();

                            for (Direction dir : LegacyDirections.VALID) {
                                npos.setWithOffset(pos, dir);
                                BlockState neighbor = world.getBlockState(npos);
                                if (neighbor.isAir() || (c.ignoreWater && neighbor.getFluidState().is(FluidTags.WATER)) || isStalagmite(neighbor)) {
                                    shouldGen = true;
                                }

                                if (shouldGen && (c.fluid.isEmpty() || !canGenFluid))
                                    break;

                                if (c.fluid.isPresent()) {
                                    switch (dir) {
                                        case UP:
                                            if (!neighbor.isAir() && !isStalagmite(neighbor)) canGenFluid = false;
                                            break;
                                        case DOWN:
                                            if (!neighbor.isSolidRender(world, npos)) canGenFluid = false;
                                            break;
                                        default:
                                            if (!neighbor.isSolidRender(world, npos) && neighbor.getBlock() != c.fluid.get().getBlock()) canGenFluid = false;
                                            break;
                                    }
                                }
                            }

                            if (c.fluid.isPresent() && canGenFluid) {
                                world.setBlock(pos, c.fluid.get(), 2);
                                world.setBlock(pos.below(), c.ore, 2);

                                for (Direction dir : LegacyDirections.HORIZONTAL) {
                                    npos.setWithOffset(pos, dir);
                                    BlockState neighbor = world.getBlockState(npos);

                                    if (neighbor.isSolidRender(world, npos))
                                        world.setBlock(npos, c.ore, 2);
                                }
                                placed = true;

                            } else if (shouldGen) {
                                world.setBlock(pos, c.ore, 2);
                                placed = true;
                            }

                        } else if (c.stalactite.isPresent() && c.stalagmite.isPresent()) {

                            if ((genTarget.isAir() || !genTarget.isSolidRender(world, pos)) && rand.nextInt(5) == 0 && genTarget.getFluidState().isEmpty()) {
                                if (canPlace(world, pos, genTarget, c.stalactite.get())) {
                                    world.setBlock(pos, c.stalactite.get(), 2);
                                } else if (canPlace(world, pos, genTarget, c.stalagmite.get())) {
                                    world.setBlock(pos, c.stalagmite.get(), 2);
                                }
                            }
                        }
                    }
                }
            }
        }
        return placed;
    }

    /** 1.7.10 {@code canPlaceBlockAt}: Platz ersetzbar und der Tropfstein haelt dort. */
    private static boolean canPlace(WorldGenLevel world, BlockPos pos, BlockState current, BlockState state) {
        return current.canBeReplaced() && state.canSurvive(world, pos);
    }

    /** {@code ForgeDirection.VALID_DIRECTIONS} (DOWN, UP, NORTH, SOUTH, WEST, EAST) und {@code getOrientation(2..5)}. */
    private static final class LegacyDirections {
        static final Direction[] VALID = {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
        static final Direction[] HORIZONTAL = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
    }

    public static final class Config implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockState.CODEC.fieldOf("ore").forGetter(c -> c.ore),
                Codec.STRING.fieldOf("ore_name").forGetter(c -> c.oreName),
                Codec.INT.optionalFieldOf("ore_meta", 0).forGetter(c -> c.oreMeta),
                BlockState.CODEC.fieldOf("target").forGetter(c -> c.target),
                Codec.DOUBLE.optionalFieldOf("threshold", 2.0D).forGetter(c -> c.threshold),
                Codec.INT.optionalFieldOf("range_mult", 3).forGetter(c -> c.rangeMult),
                Codec.INT.optionalFieldOf("max_range", 4).forGetter(c -> c.maxRange),
                Codec.INT.optionalFieldOf("y_level", 30).forGetter(c -> c.yLevel),
                BlockState.CODEC.optionalFieldOf("fluid").forGetter(c -> c.fluid),
                Codec.BOOL.optionalFieldOf("ignore_water", false).forGetter(c -> c.ignoreWater),
                BlockState.CODEC.optionalFieldOf("stalactite").forGetter(c -> c.stalactite),
                BlockState.CODEC.optionalFieldOf("stalagmite").forGetter(c -> c.stalagmite),
                Codec.STRING.optionalFieldOf("enabled").forGetter(c -> c.enabled)
        ).apply(i, Config::new));

        public final BlockState ore;
        /** Original-Registriername fuer den Rausch-Seed, z.B. {@code hbm:stone_resource}. */
        public final String oreName;
        public final int oreMeta;
        /** Ersetzter Block: {@code setBlockOverride} oder Planetengestein. */
        public final BlockState target;
        public final double threshold;
        public final int rangeMult;
        public final int maxRange;
        public final int yLevel;
        public final Optional<BlockState> fluid;
        public final boolean ignoreWater;
        public final Optional<BlockState> stalactite;
        public final Optional<BlockState> stalagmite;
        public final Optional<String> enabled;

        public Config(BlockState ore, String oreName, int oreMeta, BlockState target, double threshold, int rangeMult, int maxRange, int yLevel,
                      Optional<BlockState> fluid, boolean ignoreWater, Optional<BlockState> stalactite, Optional<BlockState> stalagmite, Optional<String> enabled) {
            this.ore = ore;
            this.oreName = oreName;
            this.oreMeta = oreMeta;
            this.target = target;
            this.threshold = threshold;
            this.rangeMult = rangeMult;
            this.maxRange = maxRange;
            this.yLevel = yLevel;
            this.fluid = fluid;
            this.ignoreWater = ignoreWater;
            this.stalactite = stalactite;
            this.stalagmite = stalagmite;
            this.enabled = enabled;
        }
    }
}
