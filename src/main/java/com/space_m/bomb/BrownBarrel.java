package com.space_m.bomb;

import com.hbm_m.block.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.TallGrassBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 1:1 NTM Space {@code BrownBarrel} als {@code oil_barrel} (Textur {@code barrel_oily}, Fass-Modell {@code RenderBarrel}):
 * <ul>
 *   <li>Form/Kollision 2..14 x 0..16 x 2..14 Pixel.</li>
 *   <li>Beim Setzen Tick in 20 Ticks; jeder Tick: {@code OilSpot.generateOilSpot(world, x, z, 4, 10, false)} und neu planen -
 *       das Fass verseucht die Umgebung dauerhaft.</li>
 *   <li>{@code onBlockDestroyedByExplosion}: zuendet nur {@code if(this == ModBlocks.yellow_barrel)} - beim Oelfass nie
 *       (1:1); {@code explode()} selbst wuerde fuer das Oelfass {@code newExplosion(2.5F, Feuer, Zerstoerung)} ausloesen.</li>
 *   <li>{@code canDropFromExplosion = false}.</li>
 * </ul>
 * Original-Klasse: BrownBarrel
 */
public class BrownBarrel extends Block {

    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 16, 14);

    public BrownBarrel(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public boolean dropFromExplosion(Explosion explosion) {
        return false;
    }

    /** Original {@code explode}: nur das Oelfass ({@code this == ModBlocks.oil_barrel}). */
    public void explode(Level world, int x, int y, int z) {
        world.explode(null, x + 0.5F, y + 0.5F, z + 0.5F, 2.5F, true, Level.ExplosionInteraction.BLOCK);
    }

    /** Original {@code tickRate}. */
    public int tickRate() {
        return 20;
    }

    @Override
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState old, boolean moved) {
        super.onPlace(state, world, pos, old, moved);
        if (!world.isClientSide) world.scheduleTick(pos, this, tickRate());
    }

    @Override
    public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource rand) {
        generateOilSpot(world, pos.getX(), pos.getZ(), 4, 10);
        world.scheduleTick(pos, this, tickRate());
    }

    /**
     * Original {@code OilSpot.generateOilSpot(world, x, z, width, count, addWillows = false)}: an {@code count} gauss-verteilten
     * Stellen die oberen 4 Bloecke absuchen - Pflanzen sterben ab, Gras/Erde -> {@code dirt_dead} (1/10 {@code dirt_oily}),
     * Sand -> {@code sand_dirty(_red)}, Stein -> {@code stone_cracked}, natuerliche Blaetter verschwinden.
     */
    public static void generateOilSpot(ServerLevel world, int x, int z, int width, int count) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < count; i++) {
            int rX = x + (int) (world.random.nextGaussian() * width);
            int rZ = z + (int) (world.random.nextGaussian() * width);
            int rY = world.getHeight(Heightmap.Types.MOTION_BLOCKING, rX, rZ);

            for (int y = rY; y > rY - 4; y--) {
                BlockState below = world.getBlockState(pos.set(rX, y - 1, rZ));
                BlockState ground = world.getBlockState(pos.set(rX, y, rZ));

                /* don't affect mustard willows */
                if (ground.is(ModBlocks.PLANT_FLOWER_CD0.get()) || ground.is(ModBlocks.PLANT_FLOWER_CD1.get())) continue;
                if (ground.is(ModBlocks.PLANT_TALL_CD2.get())) continue;

                boolean dead = ground.is(ModBlocks.PLANT_DEAD.get()) || ground.is(ModBlocks.PLANT_DEAD_GRASS.get())
                        || ground.is(ModBlocks.PLANT_DEAD_FLOWER.get()) || ground.is(ModBlocks.PLANT_DEAD_BIGFLOWER.get())
                        || ground.is(ModBlocks.PLANT_DEAD_FERN.get());

                if (below.isRedstoneConductor(world, pos.set(rX, y - 1, rZ)) && !dead) {
                    pos.set(rX, y, rZ);
                    if (ground.getBlock() instanceof TallGrassBlock) {
                        if (world.random.nextInt(10) == 0) {
                            // Original: Meta 2 (Farn) -> FERN, sonst GRASS
                            world.setBlock(pos, (ground.is(Blocks.FERN) ? ModBlocks.PLANT_DEAD_FERN : ModBlocks.PLANT_DEAD_GRASS).get().defaultBlockState(), 3);
                        } else {
                            world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                        }
                    } else if (ground.getBlock() instanceof FlowerBlock) {
                        world.setBlock(pos, ModBlocks.PLANT_DEAD_FLOWER.get().defaultBlockState(), 3);
                    } else if (ground.getBlock() instanceof DoublePlantBlock) {
                        world.setBlock(pos, ModBlocks.PLANT_DEAD_BIGFLOWER.get().defaultBlockState(), 3);
                    } else if (ground.getBlock() instanceof BushBlock) {
                        world.setBlock(pos, ModBlocks.PLANT_DEAD.get().defaultBlockState(), 3);
                    }
                }

                pos.set(rX, y, rZ);
                if (ground.is(Blocks.GRASS_BLOCK) || ground.is(Blocks.DIRT)) {
                    world.setBlock(pos, (world.random.nextInt(10) == 0 ? ModBlocks.DIRT_OILY : ModBlocks.DIRT_DEAD).get().defaultBlockState(), 3);
                    break;

                } else if (ground.is(Blocks.SAND) || ground.is(Blocks.RED_SAND) || ground.is(ModBlocks.ORE_OIL_SAND.get())) {
                    // Original: Sand-Meta 1 = roter Sand
                    world.setBlock(pos, (ground.is(Blocks.RED_SAND) ? ModBlocks.SAND_DIRTY_RED : ModBlocks.SAND_DIRTY).get().defaultBlockState(), 3);
                    break;

                } else if (ground.is(Blocks.STONE)) {
                    world.setBlock(pos, ModBlocks.STONE_CRACKED.get().defaultBlockState(), 3);
                    break;

                } else if (ground.is(BlockTags.LEAVES) && ground.hasProperty(BlockStateProperties.PERSISTENT)
                        && !ground.getValue(BlockStateProperties.PERSISTENT)) {
                    // Original: Material.leaves, Zerfall-Pruefung gesetzt (meta & 8), nicht vom Spieler gesetzt (meta & 4)
                    world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    break;
                }
            }
        }
    }
}
