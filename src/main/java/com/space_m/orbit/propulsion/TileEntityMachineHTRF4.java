package com.space_m.orbit.propulsion;

import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.space_m.celestial.CelestialBody;
import com.space_m.sound.SpaceSounds;

import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 NTM Space {@code TileEntityMachineHTRF4} (HTR-F4, Legacy): Fusionstriebwerk mit DT-Plasma (64 000 mB, Befuellung
 * nur von aussen ueber den Plasmaerhitzer), Speicher 1 GHE, Brennen kostet {@code fuelCost * 250 000} HE, Schub
 * 1 600 000 000, 180 Ticks. Strom ueber die zwei Anschluesse am Heck ({@code -rot*10 +-dir}); Geraeusche
 * {@code misc.lpwstart}/{@code lpwstop}/{@code lpwloop}; Abgas {@code missileContrailf} bzw. mit BF-Plasma
 * {@code missileContrailbf}.
 * Original-Klasse: TileEntityMachineHTRF4
 */
public class TileEntityMachineHTRF4 extends StationThrusterBlockEntity {

    public static final long maxPower = 1_000_000_000;

    private static final int POWER_COST_MULTIPLIER = 250_000;

    public TileEntityMachineHTRF4(BlockPos pos, BlockState state) {
        super(PropulsionBlockEntities.HTRF4.get(), pos, state, maxPower);
        tanks = new FluidTank[1];
        tanks[0] = new FluidTank(ModFluids.PLASMA_DT.getSource(), 64000);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.htrf4");
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileEntityMachineHTRF4 be) {
        if (!CelestialBody.inOrbit(level)) return;

        if (!level.isClientSide) {
            be.tickRegistration();

            // Only fill power, plasma heater filling handled separately
            for (PropulsionUtil.DirPos con : be.getConPos()) {
                be.trySubscribe((ServerLevel) level, con.pos().getX(), con.pos().getY(), con.pos().getZ(), con.dir());
            }

            be.tickSound(SpaceSounds.MISC_LPWSTART.get(), 1.5F, SpaceSounds.MISC_LPWSTOP.get(), 2.0F, 20);

            be.setChanged();
            be.sendUpdateToClient(); // networkPackNT(250)
        } else {
            be.tickAnimation();
            EnvExecutor.runInEnv(Env.CLIENT, () -> () -> com.space_m.orbit.propulsion.client.PropulsionClientEffects.htrf4Tick(be));
        }

        be.advanceTime();
    }

    /** Original {@code getConPos}: {@code -rot*10 +dir} und {@code -rot*10 -dir}, Richtung {@code rot}. */
    private PropulsionUtil.DirPos[] getConPos() {
        Direction dir = dir();
        Direction rot = PropulsionUtil.rot(dir);
        return new PropulsionUtil.DirPos[] {
                PropulsionUtil.at(worldPosition, dir, 1, -10, 0, rot),
                PropulsionUtil.at(worldPosition, dir, -1, -10, 0, rot),
        };
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && level.isClientSide) {
            EnvExecutor.runInEnv(Env.CLIENT, () -> () -> com.space_m.orbit.propulsion.client.PropulsionClientEffects.stop(this));
        }
    }

    @Override
    public boolean isFacingPrograde() {
        return dir() == Direction.SOUTH;
    }

    @Override
    protected long powerCostMultiplier() {
        return POWER_COST_MULTIPLIER;
    }

    @Override
    public float getThrust() {
        return 1_600_000_000.0F; // F1 thrust
    }

    @Override
    protected int burnTime() {
        return 180; // Cooldown
    }

    /** Original {@code getRenderBoundingBox}: x-11..x+12, y-2..y+3, z-11..z+12. */
    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.getX() - 11, worldPosition.getY() - 2, worldPosition.getZ() - 11,
                worldPosition.getX() + 12, worldPosition.getY() + 3, worldPosition.getZ() + 12);
    }
}
