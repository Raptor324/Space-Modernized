package com.space_m.travel;

import java.util.ArrayDeque;
import java.util.Queue;

import org.jetbrains.annotations.Nullable;

import com.space_m.platform.TeleportHooks;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * Original-Klasse: CelestialTeleporter ({@code com.hbm.dim.CelestialTeleporter}).
 *
 * <p>Portalloser Dimensionswechsel zu einem Himmelskoerper (Raketen, Drop-Pods, Stationen, {@code /ntmstations tp}).
 * Wie im Original wird jeder Wechsel nur <b>eingereiht</b> und am Ende eines Level-Ticks ausgefuehrt (je Level-Tick
 * ein Eintrag, Original: {@code ModEventHandler.worldTick} Phase END -> {@code runQueuedTeleport}), damit Raketen und
 * Spieler nicht mitten in ihrem eigenen Tick die Welt wechseln.
 *
 * <ul>
 *   <li>{@code grounded = true}: sichere Landeposition - von der Bauhoehe abwaerts der erste nicht-Luft-Block in
 *       der Zielspalte, Ziel-Y = dieser Block + 5 (1:1 {@code placeInPortal}).</li>
 *   <li>{@code grounded = false}: Ziel-Y unveraendert, die Zielspalte wird nur vorgeladen.</li>
 *   <li>Sitzt ein Spieler auf der Entity (Rakete), wird statt ihrer der Spieler teleportiert; das Reittier/Fahrzeug
 *       des Spielers wird mitgenommen und der Spieler wieder aufgesetzt ({@link CelestialMount} fuer die
 *       "Raketen-Haftung" des Originals, {@code EntityRideableRocket.setThrower}).</li>
 * </ul>
 *
 * Versionsunterschiede liegen in {@link TeleportHooks}.
 */
public class CelestialTeleporter {

    private final ResourceKey<Level> target;

    private final double x;
    private double y;
    private final double z;

    /** Should we be placed directly on the first ground block below? */
    private final boolean grounded;

    private Entity entity;

    public CelestialTeleporter(Entity entity, ResourceKey<Level> target, double x, double y, double z, boolean grounded) {
        this.entity = entity;
        this.target = target;
        this.x = x;
        this.y = y;
        this.z = z;
        this.grounded = grounded;
    }

    /** 1:1 {@code placeInPortal}: Ziel-Y bestimmen (und die Zielspalte laden/generieren). */
    public double placeInLevel(ServerLevel level) {
        int ix = (int) x;
        int iy = (int) y;
        int iz = (int) z;

        if (grounded) {
            // Original: for(i = getHeight(); i > 0; i--) if(getBlock(ix, i, iz) != air) { y = i + 5; break; }
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(ix, 0, iz);
            for (int i = level.getMaxBuildHeight(); i > level.getMinBuildHeight(); i--) {
                if (!level.getBlockState(pos.setY(i)).isAir()) {
                    y = i + 5;
                    break;
                }
            }
        } else {
            // dummy load to maybe gen chunk
            level.getBlockState(new BlockPos(ix, Mth.clamp(iy, level.getMinBuildHeight() + 1, level.getMaxBuildHeight() - 1), iz));
        }

        return y;
    }

    /** Fuehrt den Wechsel aus. @return die Entity in der Zieldimension (Spieler: der Spieler) oder null. */
    @Nullable
    Entity runTeleport(MinecraftServer server) {
        ServerLevel targetLevel = server.getLevel(target);
        if (targetLevel == null || entity.isRemoved()) return null;

        // If this entity got teleported with a player rider, switch to the rider!
        for (Entity passenger : entity.getPassengers()) {
            if (passenger instanceof ServerPlayer) {
                entity = passenger;
                break;
            }
        }

        double ty = placeInLevel(targetLevel);

        if (entity instanceof ServerPlayer player) {
            Entity ridingEntity = player.getVehicle();

            // stopRiding passiert im Spieler-Teleport (beide Versionen)
            TeleportHooks.teleport(player, targetLevel, x, ty, z, player.getYRot(), player.getXRot());

            if (ridingEntity != null && ridingEntity.isAlive()) {
                Entity newEntity = TeleportHooks.teleport(ridingEntity, targetLevel, x, ty, z, ridingEntity.getYRot(), ridingEntity.getXRot());
                if (newEntity != null) {
                    player.startRiding(newEntity, true);

                    // Ensure rocket stickiness
                    if (newEntity instanceof CelestialMount mount) {
                        mount.onCelestialTeleport(player);
                    }
                }
                // Original: playerMP.setPositionAndUpdate(x, 900, z) - nur ein 1.7.10-Client-Resync nach dem
                // Wiederaufsitzen; ab 1.20 synchronisiert ServerEntity die Mitfahrer selbst (entfaellt).
            }
            return player;
        }

        return TeleportHooks.teleport(entity, targetLevel, x, ty, z, entity.getYRot(), entity.getXRot());
    }

    // ── Warteschlange ───────────────────────────────────────────────────────────────────────────

    private static final Queue<CelestialTeleporter> queue = new ArrayDeque<>();

    /** Original {@code runQueuedTeleport}: ein Eintrag je Level-Tick (Ende). */
    public static void runQueuedTeleport(MinecraftServer server) {
        CelestialTeleporter teleporter = queue.poll();
        if (teleporter != null) teleporter.runTeleport(server);
    }

    /**
     * Reiht einen Wechsel ein (nur Serverseite). Gleiche Dimension wird wie im Original ignoriert.
     */
    public static void teleport(Entity entity, ResourceKey<Level> dim, double x, double y, double z, boolean grounded) {
        if (entity.level().dimension() == dim) return; // ignore if we're teleporting to the same place
        if (entity.level().isClientSide()) return;

        queue.add(new CelestialTeleporter(entity, dim, x, y, z, grounded));
    }

    public static int queued() {
        return queue.size();
    }

    /** Beim Serverstopp leeren (Original: statische Queue lebte bis zum JVM-Ende). */
    public static void clearQueue() {
        queue.clear();
    }
}
