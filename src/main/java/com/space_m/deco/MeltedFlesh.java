package com.space_m.deco;

import com.space_m.celestial.CelestialBody;
import com.space_m.celestial.trait.CBT_Temperature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 1:1 {@code com.hbm.blocks.fluid.MeltedFlesh} ({@code flesh_block}, {@code charred_flesh_block},
 * {@code carbonized_flesh_block}; Material.snow, Kies-Klang, Haerte 0.1, Lichtdurchlaessigkeit 0).
 *
 * <ul>
 *   <li>Flache Schicht 1 x 0.125 x 1 (Auswahl und Kollision), ersetzbar wie Schnee.</li>
 *   <li>Haelt nur auf undurchsichtigen, bewegungsblockierenden Vollbloecken oder Laub, nie auf (Pack-)Eis; sonst
 *       verschwindet sie ohne Beute ({@code setBlockToAir}).</li>
 *   <li>Zufallstick: ist der Himmelskoerper heisser als 100 Grad C ({@link CBT_Temperature}), wird Fleisch zu
 *       verkohltem Fleisch, verkohltes zu karbonisiertem.</li>
 *   <li>Anzeige (nur ueber 100 Grad C): Fleisch Rauch + Wolke, verkohltes Fleisch grosser Rauch.</li>
 *   <li>Beute (Loot-Tabellen): {@code space_m:flesh}, {@code space_m:grilled_flesh}, {@code hbm_m:coal_powder_tiny}.
 *       Material.snow verlangt ein Werkzeug, das 1.7.10 fuer diesen Block nicht kennt (nur Schnee zaehlt fuer die
 *       Schaufel) - daher {@code requiresCorrectToolForDrops} ohne Abbau-Tag: Spieler-Abbau ohne Beute,
 *       Explosionen droppen wie im Original.</li>
 * </ul>
 */
// Original-Klasse: MeltedFlesh
public class MeltedFlesh extends Block {

    private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D);

    public MeltedFlesh(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    /** Original {@code canPlaceBlockAt}. */
    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState b = level.getBlockState(below);
        if (b.is(Blocks.ICE) || b.is(Blocks.PACKED_ICE)) return false;
        if (b.is(BlockTags.LEAVES)) return true;
        return b.isSolidRender(level, below) && b.blocksMotion();
    }

    /** Original {@code onNeighborBlockChange -> func_150155_m}: ohne Halt zu Luft (keine Beute). */
    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState() : super.updateShape(state, dir, neighbor, level, pos, neighborPos);
    }

    private static boolean hot(Level level) {
        CBT_Temperature temperature = CelestialBody.getTrait(level, CBT_Temperature.class);
        return temperature != null && temperature.degrees > 100;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!hot(level)) return;
        if (this == SpaceDeco.FLESH_BLOCK.get()) {
            level.setBlock(pos, SpaceDeco.CHARRED_FLESH_BLOCK.get().defaultBlockState(), 3);
        } else if (this == SpaceDeco.CHARRED_FLESH_BLOCK.get()) {
            level.setBlock(pos, SpaceDeco.CARBONIZED_FLESH_BLOCK.get().defaultBlockState(), 3);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        if (!hot(level)) return;
        double x = pos.getX() + rand.nextFloat();
        double y = pos.getY() + rand.nextFloat() * 0.5F + 0.5F;
        double z = pos.getZ() + rand.nextFloat();
        if (this == SpaceDeco.FLESH_BLOCK.get()) {
            level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0D, 0.0D, 0.0D);
            level.addParticle(ParticleTypes.CLOUD, x, y, z, 0.0D, 0.0D, 0.0D);
        } else if (this == SpaceDeco.CHARRED_FLESH_BLOCK.get()) {
            level.addParticle(ParticleTypes.LARGE_SMOKE, x, y, z, 0.0D, 0.0D, 0.0D);
        }
    }

    //? if >= 1.21.1 {
    /*public static final com.mojang.serialization.MapCodec<MeltedFlesh> CODEC = simpleCodec(MeltedFlesh::new);
    @Override protected com.mojang.serialization.MapCodec<? extends Block> codec() { return CODEC; }
    *///?}
}
