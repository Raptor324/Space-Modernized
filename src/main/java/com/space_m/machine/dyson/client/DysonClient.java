package com.space_m.machine.dyson.client;

import com.hbm_m.particle.helper.ParticleCreators;

/**
 * Client-Anmeldung des Dyson-Systems ({@code effectNT}-Typen "spinlaunch" und "flare").
 * BERs and item models migrated to Nucleus declarative specs in SpaceMachineRenderers.
 */
public final class DysonClient {

    private DysonClient() {}

    /** Aus {@code SpaceClient.setup} (Client-Setup). */
    public static void setup() {
        // BERs and item models migrated to Nucleus declarative specs in SpaceMachineRenderers

        // ClientProxy.effectNT: "spinlaunch" (Dyson-Startanlage), "flare" (ParticleUtil.spawnFlare vom Server)
        ParticleCreators.particleCreators().putIfAbsent("spinlaunch", DysonClientEffects::spinlaunch);
        ParticleCreators.particleCreators().putIfAbsent("flare", (level, player, rand, x, y, z, data) ->
                DysonClientEffects.spawnFlare(x, y, z, data.getDouble("mX"), data.getDouble("mY"), data.getDouble("mZ"), data.getFloat("scale")));
    }
}
