package com.space_m.orbit.propulsion;

import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.space_m.celestial.CelestialBody;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 NTM Space {@code TileEntityXenonThruster}: Xenon-Ionentriebwerk, 4 000 mB Xenon, 20 MHE Speicher, Schub
 * 1 400 000; Brennen kostet {@code fuelCost} mB und {@code fuelCost * 5 000} HE, 200 Ticks Vorwaermen/Abkuehlen.
 * Nur im Orbit aktiv und nur prograd (Ausrichtung WEST) bei der Station angemeldet. Client: Schubanzeige
 * {@code thrustAmount} +-0,01/Tick (Abgasstrahl im Renderer).
 * Original-Klasse: TileEntityXenonThruster
 */
public class TileEntityXenonThruster extends StationThrusterBlockEntity {

    public static final long maxPower = 20_000_000;

    private static final int POWER_COST_MULTIPLIER = 5_000;

    public float thrustAmount;

    public TileEntityXenonThruster(BlockPos pos, BlockState state) {
        super(PropulsionBlockEntities.XENON_THRUSTER.get(), pos, state, maxPower);
        tanks = new FluidTank[1];
        tanks[0] = new FluidTank(ModFluids.XENON.getSource(), 4_000);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.xenonThruster");
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileEntityXenonThruster be) {
        if (!CelestialBody.inOrbit(level)) return;

        if (!level.isClientSide) {
            be.tickRegistration();

            for (PropulsionUtil.DirPos con : be.getConPos()) {
                for (FluidTank tank : be.tanks) {
                    be.trySubscribe((ServerLevel) level, con.pos().getX(), con.pos().getY(), con.pos().getZ(), con.dir());
                    be.trySubscribe(tank.getTankType(), level, con.pos(), con.dir());
                }
            }

            be.setChanged();
            be.sendUpdateToClient(); // networkPackNT(250)
        } else {
            if (be.isOn) {
                be.thrustAmount += 0.01D;
                if (be.thrustAmount > 1) be.thrustAmount = 1;
            } else {
                be.thrustAmount -= 0.01D;
                if (be.thrustAmount < 0) be.thrustAmount = 0;
            }
        }
    }

    /** Original {@code getConPos}: drei Felder hinter dem Kern ({@code -dir}, {@code -dir +-rot}). */
    private PropulsionUtil.DirPos[] getConPos() {
        Direction dir = dir();
        return new PropulsionUtil.DirPos[] {
                PropulsionUtil.at(worldPosition, dir, -1, -1, 0, dir),
                PropulsionUtil.at(worldPosition, dir, -1, 0, 0, dir),
                PropulsionUtil.at(worldPosition, dir, -1, 1, 0, dir),
        };
    }

    @Override
    public boolean isFacingPrograde() {
        return dir() == Direction.WEST;
    }

    @Override
    protected long powerCostMultiplier() {
        return POWER_COST_MULTIPLIER;
    }

    @Override
    public float getThrust() {
        // but le realisme :((((
        // do not speak to me of realisme, miss I can carry 2,880,000kg of dirt in my fucking pocket
        return 1_400_000;
    }

    @Override
    protected int burnTime() {
        return 200; // ten second prewarm / ten second recharge time
    }

    /** Original {@code getRenderBoundingBox}: x-2..x+3, y-1..y+2, z-2..z+3. */
    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.getX() - 2, worldPosition.getY() - 1, worldPosition.getZ() - 2,
                worldPosition.getX() + 3, worldPosition.getY() + 2, worldPosition.getZ() + 3);
    }
}
