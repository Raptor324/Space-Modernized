package com.space_m.entity.mob;

import com.hbm_m.entity.mob.siege.EntitySiegeCraft;
import com.space_m.atmosphere.ISuffocationImmune;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * NTM-Space-Fassung von {@code EntitySiegeCraft} ("Macro UFO", {@code entity_micro_ufo}, Invasions-Grosskampfschiff):
 * Verhalten von HBMs {@link EntitySiegeCraft} (Laserfaecher, Zielstrahl, Stufen) plus die Space-Ergaenzungen:
 * <ul>
 *   <li>Lebenszeit 4 Minuten, danach Rueckzug: kein Ziel, Wegpunkt senkrecht nach Y 255, verschwindet ueber Y 250 bzw.
 *       15 s nach Ablauf;</li>
 *   <li>Fluggeschwindigkeit ohne Ziel 0.5, mit Ziel {@code 1 + Tempo * 3}, beim Rueckzug doppelt.</li>
 * </ul>
 * HBMs Klasse bleibt unveraendert (Erde = Master-Verhalten); das Addon registriert diese Unterklasse unter der
 * Space-ID mit Spawn-Ei. Die Space-Beutemengen von {@code SiegeTier} sind HBM-Drift (Bericht, R9).
 */
public class EntitySiegeCraftSpace extends EntitySiegeCraft implements ISuffocationImmune {

    private int lifetime = 0;
    private boolean isRetreating = false;
    private static final int MAX_LIFETIME = 60 * 4 * 20;

    public EntitySiegeCraftSpace(EntityType<? extends EntitySiegeCraftSpace> entityType, Level world) {
        super(entityType, world);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return EntitySiegeCraft.createAttributes();
    }

    /** Beim Rueckzug kein Ziel (Original: {@code target = null} nach der Suche im selben Tick). */
    @Override
    protected void scanForTarget() {
        super.scanForTarget();
        if (isRetreating) this.target = null;
    }

    @Override
    protected void customServerAiStep() {
        lifetime++;
        if (lifetime > MAX_LIFETIME) {
            isRetreating = true;
        }
        if (isRetreating) this.target = null;

        super.customServerAiStep();
        if (this.isRemoved()) return;

        if (isRetreating) {
            this.target = null;
            this.setWaypoint((int) getX(), 255, (int) getZ());
            approachPosition(20);
            if (this.getY() > 250 || lifetime > MAX_LIFETIME + 300) {
                this.discard();
                return;
            }
        }

        // Space: neue Annaeherungsgeschwindigkeit (ueberschreibt die der HBM-Klasse im selben Tick)
        if (this.courseChangeCooldown > 0) {
            double speed = this.target == null ? 0.5D : 1.0D + (this.getAttributeValue(Attributes.MOVEMENT_SPEED) * 3.0D);

            if (isRetreating) speed *= 2.0D;

            approachPosition(speed);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putInt("lifetime", this.lifetime);
        nbt.putBoolean("isRetreating", this.isRetreating);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.lifetime = nbt.getInt("lifetime");
        this.isRetreating = nbt.getBoolean("isRetreating");
    }
}
