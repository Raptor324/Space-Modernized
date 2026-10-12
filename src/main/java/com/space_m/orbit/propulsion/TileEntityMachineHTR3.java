package com.space_m.orbit.propulsion;

import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.space_m.celestial.CelestialBody;
import com.space_m.fluid.SpaceFluids;
import com.space_m.sound.SpaceSounds;

import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 NTM Space {@code TileEntityMachineHTR3} ("LPW-3N"): thermisches Kerntriebwerk, gespeist von einem DWR
 * (Druckwasserreaktor) mit ueberhitztem Wasserstoff o.ae. Tank 1 280 000 mB (Typ ueber Fluid-Identifikator: Produkt der
 * ersten Heizstufe mit {@code FT_Rocket}), Schub 800 000 000, 80 Ticks Vorwaermen/Abkuehlen, kein Strom.
 * Server: Anmeldung (prograd = SOUTH), Fluid-Anschluss 6 Felder hinter dem Kern ({@code -rot*6}), Start-/Stopp-Geraeusch
 * {@code misc.htrstart}/{@code misc.htrstop}. Client: Animation ({@code speed/time}), Schleife {@code misc.htrloop} und
 * Abgasstrahl (Partikel je nach Treibstoff: {@code missileContrail}/{@code MUD}/{@code SCH}/{@code UP}).
 * Original-Klasse: TileEntityMachineHTR3
 */
public class TileEntityMachineHTR3 extends StationThrusterBlockEntity {

    public TileEntityMachineHTR3(BlockPos pos, BlockState state) {
        super(PropulsionBlockEntities.HTR3.get(), pos, state, 0);
        tanks = new FluidTank[1];
        tanks[0] = new FluidTank(SpaceFluids.SUPERHEATED_HYDROGEN.fluid(), 1_280_000);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.htr");
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileEntityMachineHTR3 be) {
        if (!CelestialBody.inOrbit(level)) return;

        if (!level.isClientSide) {
            be.tickRegistration();

            for (PropulsionUtil.DirPos con : be.getConPos()) {
                for (FluidTank tank : be.tanks) {
                    be.trySubscribe(tank.getTankType(), level, con.pos(), con.dir());
                }
            }

            be.tickSound(SpaceSounds.MISC_HTRSTART.get(), 1.5F, SpaceSounds.MISC_HTRSTOP.get(), 2.0F, 20);

            be.setChanged();
            be.sendUpdateToClient(); // networkPackNT(250)
        } else {
            be.tickAnimation();
            EnvExecutor.runInEnv(Env.CLIENT, () -> () -> com.space_m.orbit.propulsion.client.PropulsionClientEffects.htr3Tick(be));
        }

        be.advanceTime();
    }

    /** Original {@code getContrailType}. */
    public static String getContrailType(Fluid type) {
        if (type == SpaceFluids.GAS_WATZ.fluid() || type == com.hbm_m.inventory.fluid.ModFluids.WASTEGAS.getSource()
                || type == SpaceFluids.GASEOUS_THORIUM_BROMIDE.fluid()) return "missileContrailMUD";
        if (type == SpaceFluids.GASEOUS_SCHRABIDIUM_BROMIDE.fluid()) return "missileContrailSCH";
        if (type == SpaceFluids.GASEOUS_URANIUM_BROMIDE.fluid() || type == SpaceFluids.GASEOUS_PLUTONIUM_BROMIDE.fluid()) return "missileContrailUP";
        return "missileContrail";
    }

    /** Original {@code getConPos}: {@code dir = getOrientation(meta).getRotation(UP)}, Anschluss bei {@code -dir*6}. */
    private PropulsionUtil.DirPos[] getConPos() {
        Direction dir = PropulsionUtil.rot(dir());
        return new PropulsionUtil.DirPos[] {
                new PropulsionUtil.DirPos(worldPosition.offset(-dir.getStepX() * 6, 0, -dir.getStepZ() * 6), dir)
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
    public float getThrust() {
        return 800_000_000.0F;
    }

    @Override
    protected int burnTime() {
        return 80; // Cooldown
    }

    /** Original {@code getRenderBoundingBox}: x-10..x+11, y-3..y+4, z-10..z+11. */
    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.getX() - 10, worldPosition.getY() - 3, worldPosition.getZ() - 10,
                worldPosition.getX() + 11, worldPosition.getY() + 4, worldPosition.getZ() + 11);
    }
}
