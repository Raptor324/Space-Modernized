package com.space_m.machine.atmo.client;

import com.hbm_m.sound.ClientSoundBootstrap;
import com.hbm_m.sound.ModSounds;
import com.space_m.machine.atmo.AtmoFluids;
import com.space_m.machine.atmo.AtmosphericEmitterBlockEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Client-Teil von {@code TileEntityAtmosphericEmitter.updateEntity} (nur ueber {@code EnvExecutor} erreicht):
 * Motor-Loop ({@code getLoopedSound("hbm:block.motor", x, y, z, 0.25F, 10F, 1.0F, 20)}) und Rauchsaeule.
 */
public final class AtmoClientEffects {

    private static final double RANGE = 10.0D;
    private static final float VOLUME = 0.25F;

    private AtmoClientEffects() {}

    public static void emitterTick(AtmosphericEmitterBlockEntity be, boolean running) {
        ClientSoundBootstrap.updateSound(be, running, () -> motorLoop(be));
        if (!running || be.tank.getFill() <= 0) return;

        LocalPlayer player = Minecraft.getInstance().player;
        Level level = be.getLevel();
        if (player == null || level == null) return;

        BlockPos pos = be.getBlockPos();
        if (Math.sqrt(player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ())) >= 100) return;

        // dir = Original-Metadatum (dem Spieler zugewandt), rot = dir.getRotation(UP) = im Uhrzeigersinn
        Direction dir = be.getBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? be.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING) : Direction.NORTH;
        Direction rot = dir.getClockWise();

        CompoundTag data = new CompoundTag();
        data.putFloat("lift", 0.5F);
        data.putFloat("base", 0.375F);
        data.putFloat("max", 3F);
        data.putInt("life", 100 + level.random.nextInt(50));
        data.putString("type", "tower");
        data.putInt("color", AtmoFluids.color(be.tank.getTankType()));
        data.putDouble("posX", pos.getX() + 0.5 - dir.getStepX() * 0.5 + rot.getStepX() * 0.5);
        data.putDouble("posZ", pos.getZ() + 0.5 - dir.getStepZ() * 0.5 + rot.getStepZ() * 0.5);
        data.putDouble("posY", pos.getY() + 2.5);
        data.putDouble("mY", 0.2);

        com.hbm_m.particle.helper.ParticleEffectClient.effectNT(data);
    }

    public static void emitterStop(AtmosphericEmitterBlockEntity be) {
        ClientSoundBootstrap.updateSound(be, false, null);
    }

    private static Object motorLoop(AtmosphericEmitterBlockEntity emitter) {
        BlockPos pos = emitter.getBlockPos();
        return new AbstractTickableSoundInstance(ModSounds.MOTOR.get(), SoundSource.BLOCKS, RandomSource.create()) {
            {
                this.x = pos.getX() + 0.5;
                this.y = pos.getY() + 0.5;
                this.z = pos.getZ() + 0.5;
                this.looping = true;
                this.delay = 0;
                this.volume = VOLUME;
                this.pitch = 1F;
                this.attenuation = Attenuation.LINEAR;
            }

            @Override
            public void tick() {
                Level level = Minecraft.getInstance().level;
                if (level == null) { this.stop(); return; }
                if (!(level.getBlockEntity(pos) instanceof AtmosphericEmitterBlockEntity live) || !live.isRunning()) { this.stop(); return; }
                LocalPlayer player = Minecraft.getInstance().player;
                if (player == null) { this.stop(); return; }
                double d = Math.sqrt(player.distanceToSqr(this.x, this.y, this.z));
                this.volume = (float) Math.max(0D, VOLUME * (1D - d / RANGE));
            }
        };
    }
}
