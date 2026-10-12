package com.space_m.satellite;

import java.util.Random;

import com.space_m.celestial.CelestialBody;
import com.space_m.celestial.trait.CBT_War;
import com.space_m.celestial.trait.CBT_War.Projectile;
import com.space_m.celestial.trait.CBT_War.ProjectileType;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * 1:1 NTM Space {@code SatelliteRailgun} ({@code sat_war}): Einzelschuss-Railkanone gegen andere Himmelskoerper.
 * Erster Schuss auf ein Ziel ohne Krieg legt {@code CBT_War(100, 0)} an, jeder weitere startet ein Projektil
 * ({@code Projectile(100, 20, 50, 28*r*5, 55, 20, SMALL, Ziel)}). {@link #fire()} treibt die Muendungsfeuer-Animation
 * ({@code interp} 0..100) an. Die Felder {@code lastOp/interp/cooldown} verdecken wie im Original die der Basisklasse
 * (beide werden synchronisiert, gezeichnet wird mit diesem {@code interp}). Darstellung am Himmel: Client
 * ({@code SatelliteRenderer.renderRailgun}).
 * Original-Klasse: SatelliteRailgun
 */
public class SatelliteRailgun extends SatelliteWar {

    //time to clean up this shit and make it PROPER.

    public SatelliteRailgun() {

    }

    private boolean canFire = false;
    private boolean hasTarget = false;

    public long lastOp;
    public float interp;
    public int cooldown;
    private CelestialBody target;

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        nbt.putLong("lastOp", lastOp);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        lastOp = nbt.getLong("lastOp");
    }

    @Override
    public void onClick(Level world, int x, int z) {
        fireAtTarget(target);

        canFire = hasTarget;
    }

    @Override
    public void fire() {
        if (canFire) {
            interp += 0.5f;
            interp = Math.min(100.0f, interp + 0.3f * (100.0f - interp) * 0.15f);

            if (interp >= 100) {
                interp = 0;
                canFire = false;
            }
        }
    }

    @Override
    public void setTarget(CelestialBody body) {
        target = CelestialBody.getBody(body.dimension);
        if (target != null) {
            hasTarget = true;
        }

    }

    @Override
    public void fireAtTarget(CelestialBody body) {
        if (hasTarget) {
            if (!target.hasTrait(CBT_War.class)) {
                target.modifyTraits(new CBT_War(100, 0));
            } else {
                CBT_War war = target.getTrait(CBT_War.class);
                if (war != null) {
                    // don't crash servers please
                    float rand = (new Random()).nextFloat();

                    //TODO: be able to choose projectile types
                    Projectile projectile = new Projectile(100, 20, 50, 28 * rand * 5, 55, 20, ProjectileType.SMALL, dimensionOf(body));
                    projectile.GUIangle = (int) (rand * 360);
                    war.launchProjectile(projectile);
                }
            }
        }
    }

    /** Original {@code body.dimensionId} (Koerper ohne Dimension = 0 = Kerbin/Overworld). */
    private static ResourceLocation dimensionOf(CelestialBody body) {
        return body != null && body.dimension != null ? body.dimension.location() : Level.OVERWORLD.location();
    }

    public boolean hasTarget() {
        return hasTarget;
    }

    public boolean canFire() {
        return canFire;
    }

    public CelestialBody getTarget() {
        return target;
    }

    @Override
    public int magSize() {
        return 0;
    }

    @Override
    public void serialize(FriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeFloat(interp);
    }

    @Override
    public void deserialize(FriendlyByteBuf buf) {
        super.deserialize(buf);
        this.interp = buf.readFloat();
    }
}
