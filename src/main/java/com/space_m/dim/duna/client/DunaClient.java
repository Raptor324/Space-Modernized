package com.space_m.dim.duna.client;

import com.space_m.client.sky.CelestialSkyProvider;
import com.space_m.dim.duna.DunaDustStorm;
import com.space_m.network.SpacePacketHandler;
import com.space_m.particle.SpaceParticles;

import dev.architectury.registry.client.particle.ParticleProviderRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Clientseite des Duna-Staubsturms (Original {@code WorldProviderDuna.updateWeather}, Zweig {@code worldObj.isRemote},
 * und {@code deserialize}): Staerke vom Server uebernehmen, Staubpartikel um die Kamera.
 */
public final class DunaClient {

    private DunaClient() {}

    /** Aus {@code SpaceClient.init()}. */
    public static void init() {
        SpacePacketHandler.registerS2C(SpacePacketHandler.DUNA_DUST_STORM, DunaDustStorm.Packet::decode, (pkt, ctx) -> ctx.queue(() -> {
            DunaDustStorm.clientIntensity = pkt.intensity;
            CelestialSkyProvider.Duna.dustStorm = pkt.intensity;
        }));
        ParticleProviderRegistry.register(SpaceParticles.DUST_STORM, DunaDustParticle.Provider::new);
    }

    /**
     * Je Client-Tick in der Duna nach der Glaettung ({@code CelestialSkyProvider.Duna.clientTick}): 1:1
     * <pre>
     * if(dustStormSmoothed >= 0.05F &amp;&amp; worldObj.rand.nextFloat() &lt; dustStormSmoothed) {
     *     Vec3 vec = Vec3.createVectorHelper(20, 0, 50);
     *     vec.rotateAroundZ((float)(worldObj.rand.nextDouble() * Math.PI * 10));
     *     vec.rotateAroundY((float)(worldObj.rand.nextDouble() * Math.PI * 2 * 5));
     *     ParticleUtil.spawnDustFlame(worldObj, viewEntity.posX + vec.xCoord, viewEntity.posY, viewEntity.posZ + vec.zCoord, -4, 0, 0);
     * }
     * </pre>
     */
    public static void tickDust(ClientLevel level, float dustStormSmoothed) {
        if (dustStormSmoothed >= 0.05F && level.random.nextFloat() < dustStormSmoothed) {
            Entity viewEntity = Minecraft.getInstance().getCameraEntity();
            if (viewEntity == null) return;
            Vec3 vec = new Vec3(20, 0, 50);
            vec = vec.zRot((float) (level.random.nextDouble() * Math.PI * 10));
            vec = vec.yRot((float) (level.random.nextDouble() * Math.PI * 2 * 5));
            level.addParticle(SpaceParticles.DUST_STORM.get(), viewEntity.getX() + vec.x, viewEntity.getY(), viewEntity.getZ() + vec.z, -4, 0, 0);
        }
    }
}
