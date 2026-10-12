package com.space_m.machine.dyson.client;

import com.hbm_m.particle.nt.ParticleCoolingTowerNT;
import com.hbm_m.particle.nt.ParticleEngineNT;
import com.hbm_m.particle.nt.ParticleRocketFlameNT;
import com.hbm_m.sound.ClientSoundBootstrap;
import com.space_m.machine.atmo.AtmoFluids;
import com.space_m.machine.dyson.TileEntityDysonConverterAnatmogenesis;
import com.space_m.machine.dyson.TileEntityDysonReceiver;
import com.space_m.sound.SpaceSounds;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Client-Teile des Dyson-Systems (nur ueber {@code EnvExecutor} erreicht):
 * <ul>
 *   <li>{@code TileEntityDysonReceiver}: Schleife {@code block.dysonBeam} an {@code dir*8} ({@code getLoopedSound(..., 0.75F,
 *       20F, 1.0F, 20)}, {@code updatePitch(0.85F)}) und in 1/10 der Ticks ein Leuchtfunke ({@code spawnFlare}) ueber der
 *       Schuessel (y+11, +-5).</li>
 *   <li>{@code TileEntityDysonConverterAnatmogenesis}: Gasfahne {@code effectNT "tower"} (lift 0.5, base 0.8, max 4, Leben
 *       100-149, Farbe des Gases, zufaellige Bewegung) auf y+3.25.</li>
 *   <li>Partikeltyp {@code "spinlaunch"} ({@code ClientProxy.effectNT}): {@code count} Raketenflammen mit Groesse
 *       {@code scale}, Bewegung {@code mo* * (1 - rand*0.1) + Versatz}, Lebensdauer {@code maxAge + rand(5)}.</li>
 * </ul>
 */
public final class DysonClientEffects {

    private DysonClientEffects() {}

    public static void receiverTick(TileEntityDysonReceiver be, Direction dir) {
        ClientSoundBootstrap.updateSound(be, be.isReceiving, () -> beamLoop(be, dir));
        if (!be.isReceiving) return;

        Level level = be.getLevel();
        if (level == null) return;
        BlockPos pos = be.getBlockPos();
        if (level.random.nextInt(10) == 0) {
            spawnFlare(pos.getX() - 5 + level.random.nextDouble() * 10, pos.getY() + 11, pos.getZ() - 5 + level.random.nextDouble() * 10,
                    0, 0.1 + level.random.nextFloat() * 0.1, 0, 4F + level.random.nextFloat() * 2);
        }
    }

    public static void stop(TileEntityDysonReceiver be) {
        ClientSoundBootstrap.updateSound(be, false, null);
    }

    /** Original {@code ParticleUtil.spawnFlare} (Client-Zweig: {@code effectNT} mit Typ "flare"). */
    public static void spawnFlare(double x, double y, double z, double mX, double mY, double mZ, float scale) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        ParticleEngineNT.INSTANCE.add(new ParticleGlowNT(level, x, y, z, mX, mY, mZ, scale));
    }

    public static void anatmogenesisTick(TileEntityDysonConverterAnatmogenesis be) {
        Level level = be.getLevel();
        if (!(level instanceof ClientLevel cl)) return;
        BlockPos pos = be.getBlockPos();

        // effectNT "tower" mit Bewegung mX/mY/mZ (HBMs Erzeuger uebernimmt sie nicht, daher direkt)
        ParticleCoolingTowerNT fx = new ParticleCoolingTowerNT(cl, pos.getX() + 0.5, pos.getY() + 3.25, pos.getZ() + 0.5);
        fx.setLift(0.5F);
        fx.setBaseScale(0.8F);
        fx.setMaxScale(4F);
        fx.setLife(100 + level.random.nextInt(50));
        fx.setColor(AtmoFluids.color(be.fluid));
        fx.xd = (level.random.nextDouble() - 0.5);
        fx.yd = (level.random.nextDouble()) * 2;
        fx.zd = (level.random.nextDouble() - 0.5);
        ParticleEngineNT.INSTANCE.add(fx);
    }

    /** {@code ClientProxy.effectNT} Zweig "spinlaunch". */
    public static void spinlaunch(ClientLevel world, Player player, RandomSource rand, double x, double y, double z, CompoundTag data) {
        int count = data.getInt("count");

        float scale = data.contains("scale") ? data.getFloat("scale") : 1F;
        double mX = data.getDouble("moX");
        double mY = data.getDouble("moY");
        double mZ = data.getDouble("moZ");

        int maxAge = data.getInt("maxAge");

        for (int i = 0; i < count; i++) {
            double ox = rand.nextDouble() * 2 - 1;
            double oy = rand.nextDouble() * 2 - 1;
            double oz = rand.nextDouble() * 2 - 1;
            double mult = 1.0 - rand.nextDouble() * 0.1;

            ParticleRocketFlameNT fx = new ParticleRocketFlameNT(world, x + ox, y + oy, z + oz).setScale(scale);
            fx.xd = mX * mult + ox;
            fx.yd = mY * mult + oy;
            fx.zd = mZ * mult + oz;
            if (maxAge > 0) fx.setMaxAge(maxAge + rand.nextInt(5));
            ParticleEngineNT.INSTANCE.add(fx);
        }
    }

    private static Object beamLoop(TileEntityDysonReceiver receiver, Direction dir) {
        BlockPos pos = receiver.getBlockPos();
        final float baseVolume = 0.75F;
        final double range = 20D;
        return new AbstractTickableSoundInstance(SpaceSounds.BLOCK_DYSONBEAM.get(), SoundSource.BLOCKS, RandomSource.create()) {
            {
                this.x = pos.getX() + dir.getStepX() * 8;
                this.y = pos.getY();
                this.z = pos.getZ() + dir.getStepZ() * 8;
                this.looping = true;
                this.delay = 0;
                this.volume = baseVolume;
                this.pitch = 0.85F; // updatePitch(0.85F)
                this.attenuation = Attenuation.NONE;
            }

            @Override
            public void tick() {
                Level level = Minecraft.getInstance().level;
                if (level == null) { this.stop(); return; }
                if (!(level.getBlockEntity(pos) instanceof TileEntityDysonReceiver live) || !live.isReceiving) { this.stop(); return; }
                LocalPlayer player = Minecraft.getInstance().player;
                if (player == null) { this.stop(); return; }
                double d = Math.sqrt(player.distanceToSqr(this.x, this.y, this.z));
                this.volume = (float) Math.max(0D, baseVolume * (1D - d / range));
            }
        };
    }
}
