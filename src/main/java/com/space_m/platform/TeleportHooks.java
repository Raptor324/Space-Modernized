package com.space_m.platform;

import org.jetbrains.annotations.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Dimensionswechsel ohne Portal (Original {@code CelestialTeleporter}: Spieler/Rakete/Drop-Pod an eine feste
 * Position im Zielkoerper setzen, keine Portalsuche, kein Portal-Bau).
 *
 * <p>Spieler: {@code ServerPlayer.teleportTo(ServerLevel, x, y, z, yaw, pitch)} (beide Versionen gleich).
 * Andere Entities: 1.20.1-Forge {@code changeDimension(level, ITeleporter)}, 1.21.1 {@code changeDimension(DimensionTransition)}.
 */
public final class TeleportHooks {
    private TeleportHooks() {}

    /**
     * Setzt die Entity in {@code target} an die Position. Gleiche Dimension = normales Teleportieren.
     *
     * @return die Entity in der Zieldimension (bei Nicht-Spielern eine neue Instanz) oder null bei Fehlschlag
     */
    @Nullable
    public static Entity teleport(Entity entity, ServerLevel target, double x, double y, double z, float yaw, float pitch) {
        if (entity instanceof ServerPlayer player) {
            player.teleportTo(target, x, y, z, yaw, pitch);
            return player;
        }
        if (entity.level() == target) {
            entity.moveTo(x, y, z, yaw, pitch);
            return entity;
        }
        //? if forge {
        return entity.changeDimension(target, new net.minecraftforge.common.util.ITeleporter() {
            @Override
            public Entity placeEntity(Entity e, ServerLevel current, ServerLevel dest, float y0,
                                      java.util.function.Function<Boolean, Entity> repositionEntity) {
                // false = keine Portalsuche/kein Portal-Bau (CelestialTeleporter.placeInPortal ist leer)
                return repositionEntity.apply(false);
            }

            @Override
            public net.minecraft.world.level.portal.PortalInfo getPortalInfo(Entity e, ServerLevel dest,
                    java.util.function.Function<ServerLevel, net.minecraft.world.level.portal.PortalInfo> defaultPortalInfo) {
                return new net.minecraft.world.level.portal.PortalInfo(new Vec3(x, y, z), Vec3.ZERO, yaw, pitch);
            }

            @Override
            public boolean playTeleportSound(ServerPlayer player, ServerLevel src, ServerLevel dest) {
                return false;
            }
        });
        //?} else {
        /*return entity.changeDimension(new net.minecraft.world.level.portal.DimensionTransition(
                target, new Vec3(x, y, z), Vec3.ZERO, yaw, pitch,
                net.minecraft.world.level.portal.DimensionTransition.DO_NOTHING));
        *///?}
    }

    @Nullable
    public static Entity teleport(Entity entity, ServerLevel target, Vec3 pos) {
        return teleport(entity, target, pos.x, pos.y, pos.z, entity.getYRot(), entity.getXRot());
    }
}
