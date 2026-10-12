package com.space_m.bomb;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 NTM {@code TileEntityFF} ({@code tileentity_forcefield}): Kugelschild mit Radius 10 um die Blockmitte. Jede
 * Nicht-Spieler-Entity innerhalb von {@code rad + 25} wird verfolgt; ueberquert sie die Kugel (nach innen oder aussen),
 * wird sie auf {@code rad +- 1} zurueckgesetzt, ihre Bewegung gespiegelt und {@code weapon.sparkShoot} (2.5/1.0) gespielt.
 * Laeuft wie im Original auf beiden Seiten ({@code if(!worldObj.isRemote)} auskommentiert), Geraeusch nur auf dem Server.
 * Original-Klasse: TileEntityFF
 */
public class TileEntityFF extends BlockEntity {

    final List<Entity> outside = new ArrayList<>();
    final List<Entity> inside = new ArrayList<>();

    public TileEntityFF(BlockPos pos, BlockState state) {
        super(SpaceBombs.FORCEFIELD_BE.get(), pos, state);
    }

    public void tick() {
        prototype(10F);
    }

    public void prototype(float rad) {
        if (level == null) return;

        List<Entity> oLegacy = new ArrayList<>(outside);
        List<Entity> iLegacy = new ArrayList<>(inside);

        outside.clear();
        inside.clear();

        double cx = worldPosition.getX() + 0.5, cy = worldPosition.getY() + 0.5, cz = worldPosition.getZ() + 0.5;
        List<Entity> list = level.getEntities((Entity) null, new AABB(cx - (rad + 25), cy - (rad + 25), cz - (rad + 25),
                cx + (rad + 25), cy + (rad + 25), cz + (rad + 25)));

        for (Entity entity : list) {
            if (entity instanceof Player) continue;

            double dist = Math.sqrt(Math.pow(cx - entity.getX(), 2) + Math.pow(cy - entity.getY(), 2) + Math.pow(cz - entity.getZ(), 2));
            boolean out = dist > rad;

            // if the entity has not been registered yet
            if (!oLegacy.contains(entity) && !iLegacy.contains(entity)) {
                if (out) outside.add(entity);
                else inside.add(entity);

            // if the entity has crossed inwards
            } else if (oLegacy.contains(entity) && !out) {
                bounce(entity, cx, cy, cz, rad + 1, -1);
                outside.add(entity);

            // if the entity has crossed outwards
            } else if (iLegacy.contains(entity) && out) {
                bounce(entity, cx, cy, cz, rad - 1, 1);
                inside.add(entity);

            } else {
                if (out) outside.add(entity);
                else inside.add(entity);
            }
        }
    }

    /** Gemeinsamer Teil beider Zweige: auf die Schale setzen, Bewegung entlang der Normalen ({@code sign}) umlenken. */
    private void bounce(Entity entity, double cx, double cy, double cz, double shell, int sign) {
        Vec3 vec = new Vec3(cx - entity.getX(), cy - entity.getY(), cz - entity.getZ()).normalize();

        double mx = -vec.x * shell;
        double my = -vec.y * shell;
        double mz = -vec.z * shell;

        entity.moveTo(cx + mx, cy + my, cz + mz, 0, 0);

        Vec3 motion = entity.getDeltaMovement();
        double mo = motion.length();
        Vec3 newMotion = new Vec3(vec.x * sign * mo, vec.y * sign * mo, vec.z * sign * mo);
        entity.setDeltaMovement(newMotion);

        // Original: posX -= motionX (ein Schritt zurueck)
        entity.setPos(entity.getX() - newMotion.x, entity.getY() - newMotion.y, entity.getZ() - newMotion.z);

        if (!level.isClientSide)
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), HbmSoundsNT.get("hbm:weapon.sparkShoot"), SoundSource.BLOCKS, 2.5F, 1.0F);
    }

    /** Original {@code getRenderBoundingBox}: {@code INFINITE_EXTENT_AABB}. */
    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        return com.hbm_m.platform.RenderBounds.INFINITE;
    }
}
