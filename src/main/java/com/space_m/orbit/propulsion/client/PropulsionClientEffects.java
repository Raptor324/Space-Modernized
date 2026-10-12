package com.space_m.orbit.propulsion.client;

import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.particle.nt.ParticleEngineNT;
import com.hbm_m.sound.ClientSoundBootstrap;
import com.space_m.celestial.CelestialBody;
import com.space_m.celestial.trait.CBT_Atmosphere;
import com.space_m.orbit.propulsion.PropulsionUtil;
import com.space_m.orbit.propulsion.TileEntityMachineHTR3;
import com.space_m.orbit.propulsion.TileEntityMachineHTRF4;
import com.space_m.orbit.propulsion.TileEntityMachineHTRNeo;
import com.space_m.sound.SpaceSounds;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Client-Teil der Triebwerks-{@code updateEntity} (nur ueber {@code EnvExecutor} erreicht): Schleifengeraeusch
 * ({@code getLoopedSound(sound, x, y, z, 0.25F, 27.5F, 1.0F, 20)} mit {@code updateVolume(getVolume(1F))} = Lautstaerke 1,
 * lineare Reichweite 27,5) und Abgas-Partikel ({@code MainRegistry.proxy.effectNT}, mit dem Atmosphaerendruck des
 * Originals: im Orbit 0).
 */
public final class PropulsionClientEffects {

    private static final float RANGE = 27.5F;

    private PropulsionClientEffects() {}

    /** Original {@code TileEntityMachineHTR3.updateEntity} (Client): ab {@code soundtime > 18} Schleife und Abgas bei {@code rot*7}. */
    public static void htr3Tick(TileEntityMachineHTR3 be) {
        boolean running = be.isOn && be.soundtime > 18;
        ClientSoundBootstrap.updateSound(be, running, () -> loop(be, SpaceSounds.MISC_HTRLOOP.get()));
        if (!running) return;

        Direction dir = PropulsionUtil.rot(be.dir());
        BlockPos pos = be.getBlockPos();
        contrail(TileEntityMachineHTR3.getContrailType(be.tanks[0].getTankType()),
                pos.getX() + dir.getStepX() * 7, pos.getY() + 1, pos.getZ() + dir.getStepZ() * 7,
                dir.getStepX() * 10, 0, dir.getStepZ() * 10, 3F, 40 + be.getLevel().random.nextInt(40));
    }

    /** Original {@code TileEntityMachineHTRF4.updateEntity} (Client): Abgas bei {@code rot*12}, BF-Plasma eigene Flamme. */
    public static void htrf4Tick(TileEntityMachineHTRF4 be) {
        boolean running = be.isOn && be.soundtime > 18;
        ClientSoundBootstrap.updateSound(be, running, () -> loop(be, SpaceSounds.MISC_LPWLOOP.get()));
        if (!running) return;

        Direction dir = PropulsionUtil.rot(be.dir());
        BlockPos pos = be.getBlockPos();
        contrail(be.tanks[0].getTankType() == ModFluids.PLASMA_BF.getSource() ? "missileContrailbf" : "missileContrailf",
                pos.getX() + dir.getStepX() * 12, pos.getY() + 1, pos.getZ() + dir.getStepZ() * 12,
                dir.getStepX() * 10, 0, dir.getStepZ() * 10, 3F, 40 + be.getLevel().random.nextInt(40));
    }

    /** Original {@code TileEntityMachineHTRNeo.updateEntity} (Client): Schleife ab {@code soundtime > 28}, kein Partikel. */
    public static void htrNeoTick(TileEntityMachineHTRNeo be) {
        ClientSoundBootstrap.updateSound(be, be.isOn && be.soundtime > 28, () -> loop(be, SpaceSounds.MISC_HTRLOOP.get()));
    }

    /**
     * R9-B4: Original {@code TileEntityMachineLPW2.updateEntity} (Client): ab {@code soundtime > 18} Schleife
     * {@code misc.lpwloop} und Abgas {@code missileContrail} bei {@code rot*8.5}, y+4.5 (Massstab 3, Alter 20-39).
     */
    public static void lpw2Tick(com.hbm_m.blockentity.machines.MachineLpw2BlockEntity be, boolean running) {
        ClientSoundBootstrap.updateSound(be, running, () -> loop(be, SpaceSounds.MISC_LPWLOOP.get()));
        if (!running) return;

        Direction dir = PropulsionUtil.rot(PropulsionUtil.dir(be.getBlockState()));
        BlockPos pos = be.getBlockPos();
        contrail("missileContrail",
                pos.getX() + dir.getStepX() * 8.5, pos.getY() + 4.5, pos.getZ() + dir.getStepZ() * 8.5,
                dir.getStepX() * 10, 0, dir.getStepZ() * 10, 3F, 20 + be.getLevel().random.nextInt(20));
    }

    public static void stop(BlockEntity be) {
        ClientSoundBootstrap.updateSound(be, false, null);
    }

    /** {@code NBTTagCompound data ... MainRegistry.proxy.effectNT(data)} mit Atmosphaerendruck. */
    private static void contrail(String type, double x, double y, double z, double moX, double moY, double moZ, float scale, int maxAge) {
        ClientLevel world = Minecraft.getInstance().level;
        LocalPlayer player = Minecraft.getInstance().player;
        if (world == null || player == null) return;

        if (new Vec3(player.getX() - x, player.getY() - y, player.getZ() - z).length() > 350) return;

        boolean inOrbit = CelestialBody.inOrbit(world);
        CBT_Atmosphere atmosphere = !inOrbit ? CelestialBody.getTrait(world, CBT_Atmosphere.class) : null;
        double pressure = atmosphere != null ? atmosphere.getPressure() : 0;

        ParticleRocketSpace fx = new ParticleRocketSpace(world, x, y, z, ParticleRocketSpace.Variant.of(type));
        fx.setScale(scale);
        fx.xd = moX;
        fx.yd = moY;
        fx.zd = moZ;
        fx.setMaxAge(maxAge);
        fx.setAtmosphericPressure(pressure);
        ParticleEngineNT.INSTANCE.add(fx);
    }

    private static Object loop(BlockEntity be, SoundEvent sound) {
        BlockPos pos = be.getBlockPos();
        return new AbstractTickableSoundInstance(sound, SoundSource.BLOCKS, RandomSource.create()) {
            {
                this.x = pos.getX() + 0.5;
                this.y = pos.getY() + 0.5;
                this.z = pos.getZ() + 0.5;
                this.looping = true;
                this.delay = 0;
                this.volume = 1F;
                this.pitch = 1F;
                this.attenuation = Attenuation.NONE;
            }

            @Override
            public void tick() {
                ClientLevel level = Minecraft.getInstance().level;
                if (level == null || level.getBlockEntity(pos) != be || be.isRemoved()) { this.stop(); return; }
                LocalPlayer player = Minecraft.getInstance().player;
                if (player == null) { this.stop(); return; }
                double d = Math.sqrt(player.distanceToSqr(this.x, this.y, this.z));
                this.volume = (float) Math.max(0D, 1D - d / RANGE);
            }
        };
    }
}
