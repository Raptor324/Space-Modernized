package com.space_m.orbit.propulsion;

import java.util.List;

import com.hbm_m.api.space.Lpw2Hooks;
import com.hbm_m.api.space.SpaceApi;
import com.hbm_m.blockentity.machines.MachineLpw2BlockEntity;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.space_m.celestial.CelestialBody;
import com.space_m.celestial.SolarSystem;
import com.space_m.orbit.IPropulsion;
import com.space_m.sound.SpaceSounds;

import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 1:1 NTM Space {@code TileEntityMachineLPW2} (Stationstriebwerk "LPW-2"), angedockt an HBMs {@code machine_lpw2}
 * ({@code com.hbm_m.blockentity.machines.MachineLpw2BlockEntity}) ueber die Space-API {@link Lpw2Hooks} (W-R6B-1).
 * HBM stellt Tanks, {@code isOn}, {@code time/lastTime/speed}, {@code addonData} und den Ticker bereit, das Addon die
 * Logik:
 * <ul>
 *   <li>Tanks {@code KEROSENE_REFORM} 256 000 mB + {@code OXYGEN} 256 000 mB; Schub 2 000 000 000 (F1), ISP aus
 *       {@code FT_Rocket} des Tanks 0 (sonst 300), Brennen/Abkuehlen je 120 Ticks.</li>
 *   <li>Nur im Orbit: Anmeldung bei der Station (nur prograd = Blickrichtung SOUTH), Fluid-Anschluesse
 *       {@code +-dir*4 - rot} auf y+3, Start-/Stoppgeraeusch {@code misc.lpwstart} (1,5) / {@code misc.lpwstop} (2,0)
 *       ueber den {@code soundtime}-Zaehler (max. 20).</li>
 *   <li>Client: Animation ({@code speed} +-0,05, {@code lastTime = time; time += speed}), ab {@code soundtime > 18}
 *       Schleife {@code misc.lpwloop} und Abgas {@code missileContrail} bei {@code rot*8.5}, y+4,5 (Massstab 3,
 *       Tempo {@code rot*10}, Alter 20-39).</li>
 * </ul>
 * Der Block-Entity ist HBMs Klasse; die {@link IPropulsion}-Seite haelt {@link Engine} (im Feld {@code addonState}).
 * {@code soundtime} und {@code fuelCost} (Original {@code serialize}) laufen ueber {@code addonData} zum Client.
 *
 * <p>Abweichung: HBMs Block-Entity meldet sein Entfernen nicht an das Addon (Original {@code invalidate}/
 * {@code onChunkUnload} -> {@code unregisterPropulsion}). Ein entferntes Triebwerk zaehlt deshalb nicht mehr
 * (Schub 0, keine Bedingungen) - Wunsch W-R9B4-1 ({@code Lpw2Hooks.onRemoved}).
 * Original-Klasse: TileEntityMachineLPW2
 */
public final class Lpw2Thruster implements Lpw2Hooks {

    public static final String SOUNDTIME = "soundtime";
    public static final String FUEL_COST = "fuelCost";
    /** Original {@code tile.machine_lpw2.name} ("LPW-2 Station Thruster"). */
    public static final String NAME_KEY = "block.space_m.machine_lpw2";

    private Lpw2Thruster() {}

    /** Muss im Mod-Konstruktor laufen (HBM legt die Blockstruktur bei der Blockregistrierung fest). */
    public static void init() {
        SpaceApi.setLpw2Hooks(new Lpw2Thruster());
    }

    @Override
    public boolean active() {
        return true;
    }

    @Override
    public FluidTank[] createTanks() {
        return new FluidTank[] {
                new FluidTank(ModFluids.KEROSENE_REFORM.getSource(), 256_000),
                new FluidTank(ModFluids.OXYGEN.getSource(), 256_000)
        };
    }

    // ── Server ───────────────────────────────────────────────────────────────────────────────

    @Override
    public void serverTick(MachineLpw2BlockEntity be) {
        Level level = be.getLevel();
        if (level == null || !CelestialBody.inOrbit(level)) return;

        Engine engine = engine(be);

        if (engine.registration.tick(engine, isFacingPrograde(be))) {
            be.isOn = false;
        }

        for (PropulsionUtil.DirPos pos : getConPos(be)) {
            for (FluidTank tank : be.tanks) {
                be.trySubscribe(tank.getTankType(), level, pos.pos(), pos.dir());
            }
        }

        float soundtime = be.addonData.getFloat(SOUNDTIME);
        if (be.isOn) {
            soundtime++;

            if (soundtime == 1) {
                level.playSound(null, be.getBlockPos(), SpaceSounds.MISC_LPWSTART.get(), SoundSource.BLOCKS, 1.5F, 1F);
            } else if (soundtime > 20) {
                soundtime = 20;
            }
        } else {
            soundtime--;

            if (soundtime == 19) {
                level.playSound(null, be.getBlockPos(), SpaceSounds.MISC_LPWSTOP.get(), SoundSource.BLOCKS, 2.0F, 1F);
            } else if (soundtime <= 0) {
                soundtime = 0;
            }
        }

        // networkPackNT(250): isOn, soundtime, fuelCost, Tanks
        be.addonData.putFloat(SOUNDTIME, soundtime);
        be.addonData.putInt(FUEL_COST, engine.fuelCost);
        be.markForSync();

        be.lastTime = be.time;
        be.time += be.speed;
    }

    // ── Client ───────────────────────────────────────────────────────────────────────────────

    @Override
    public void clientTick(MachineLpw2BlockEntity be) {
        Level level = be.getLevel();
        if (level == null || !CelestialBody.inOrbit(level)) return;

        if (be.isOn) {
            be.speed += 0.05D;
            if (be.speed > 1) be.speed = 1;
        } else {
            be.speed -= 0.05D;
            if (be.speed < 0) be.speed = 0;
        }

        EnvExecutor.runInEnv(Env.CLIENT, () -> () -> com.space_m.orbit.propulsion.client.PropulsionClientEffects.lpw2Tick(be,
                be.isOn && be.addonData.getFloat(SOUNDTIME) > 18));

        be.lastTime = be.time;
        be.time += be.speed;
    }

    // ── Hilfen ───────────────────────────────────────────────────────────────────────────────

    /** Original {@code isFacingPrograde()}: {@code getOrientation(meta - offset) == SOUTH}. */
    public static boolean isFacingPrograde(BlockEntity be) {
        return PropulsionUtil.dir(be.getBlockState()) == Direction.SOUTH;
    }

    /** Original {@code getConPos}: {@code x +- dir*4 - rot}, y+3, Richtung {@code rot} bzw. {@code rot.getOpposite()}. */
    static PropulsionUtil.DirPos[] getConPos(BlockEntity be) {
        Direction dir = PropulsionUtil.dir(be.getBlockState());
        Direction rot = PropulsionUtil.rot(dir);
        BlockPos p = be.getBlockPos();
        return new PropulsionUtil.DirPos[] {
                new PropulsionUtil.DirPos(p.offset(dir.getStepX() * 4 - rot.getStepX(), 3, dir.getStepZ() * 4 - rot.getStepZ()), rot),
                new PropulsionUtil.DirPos(p.offset(-dir.getStepX() * 4 - rot.getStepX(), 3, -dir.getStepZ() * 4 - rot.getStepZ()), rot.getOpposite())
        };
    }

    /** Die {@link IPropulsion}-Seite eines LPW-2 (lebt im Laufzeitfeld {@code addonState} des HBM-Block-Entitys). */
    public static Engine engine(MachineLpw2BlockEntity be) {
        Engine e = be.addonState instanceof Engine en && en.be == be ? en : null;
        if (e == null) {
            e = new Engine(be);
            be.addonState = e;
        }
        return e;
    }

    /** {@code IPropulsion} des Originals ({@code canPerformBurn/addErrors/getThrust/startBurn/endBurn}). */
    public static final class Engine implements IPropulsion {

        final MachineLpw2BlockEntity be;
        final PropulsionUtil.Registration registration = new PropulsionUtil.Registration();
        int fuelCost;

        Engine(MachineLpw2BlockEntity be) {
            this.be = be;
            this.fuelCost = be.addonData.getInt(FUEL_COST);
        }

        private boolean gone() {
            return be.isRemoved();
        }

        @Override
        public BlockEntity getBlockEntity() {
            return be;
        }

        @Override
        public boolean canPerformBurn(int shipMass, double deltaV) {
            if (gone()) return true;
            int isp = PropulsionUtil.isp(be.tanks[0]);

            fuelCost = SolarSystem.getFuelCost(deltaV, shipMass, isp);

            for (FluidTank tank : be.tanks) {
                if (tank.getFill() < fuelCost) return false;
            }

            return true;
        }

        @Override
        public void addErrors(List<Component> errors) {
            if (gone()) return;
            for (FluidTank tank : be.tanks) {
                if (tank.getFill() < fuelCost) {
                    PropulsionUtil.fuelError(errors, fuelCost);
                }
            }
        }

        @Override
        public float getThrust() {
            if (gone()) return 0F;
            return 2_000_000_000.0F; // F1 thrust
        }

        @Override
        public int startBurn() {
            if (gone()) return 0;
            be.isOn = true;
            for (FluidTank tank : be.tanks) {
                tank.setFill(tank.getFill() - fuelCost);
            }
            be.setChanged();
            return 120;
        }

        @Override
        public int endBurn() {
            if (gone()) return 0;
            be.isOn = false;
            be.setChanged();
            return 120; // Cooldown
        }

        public int getFuelCost() {
            return fuelCost;
        }
    }
}
