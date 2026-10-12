package com.space_m.bomb;

import com.hbm_m.entity.logic.EntityNukeExplosionMK3;
import com.hbm_m.radiation.ChunkRadiationManager;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 NTM Space {@code MachineTrollDisturber(10000000F)} (Textur {@code field_disturber}, kein Creative-Tab):
 * <ul>
 *   <li>Beim Setzen: Tick in 10 Ticks planen und die Chunk-Strahlung um {@code absorb} (10 Mio. RAD) senken
 *       ({@code ChunkRadiationManager.proxy.decrementRad}).</li>
 *   <li>Je Tick (geplant alle 10 Ticks und zufaellig, {@code setTickRandomly(true)}): neu planen und sich wie der
 *       Feldstoerer fuer 100 Ticks in {@code EntityNukeExplosionMK3.at} eintragen (F.L.E.I.J.A.-Stoerung).</li>
 * </ul>
 * Original-Klasse: MachineTrollDisturber
 */
public class MachineTrollDisturber extends Block {

    final float absorb;

    public MachineTrollDisturber(Properties properties, float ab) {
        super(properties);
        this.absorb = ab;
    }

    /** Original {@code tickRate}. */
    public int tickRate() {
        return 10;
    }

    @Override
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState old, boolean moved) {
        if (!world.isClientSide) {
            world.scheduleTick(pos, this, tickRate());
            ChunkRadiationManager.getProxy().decrementRad(world, pos.getX(), pos.getY(), pos.getZ(), absorb);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource rand) {
        update(world, pos);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource rand) {
        update(world, pos);
    }

    /** Original {@code updateTick} (geplanter und zufaelliger Tick laufen dort durch dieselbe Methode). */
    private void update(ServerLevel world, BlockPos pos) {
        world.scheduleTick(pos, this, tickRate());
        EntityNukeExplosionMK3.at.put(new EntityNukeExplosionMK3.ATEntry(world.dimension().location().toString(),
                pos.getX(), pos.getY(), pos.getZ()), world.getGameTime() + 100);
    }
}
