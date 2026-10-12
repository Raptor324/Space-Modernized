package com.space_m.entity.mob;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code EntityScuttlecrab} (Laythe-Riff): Wassertier ohne KI, das waehrend seiner Lebensupdates nicht "im
 * Wasser" ist und deshalb mit Landphysik ueber den Meeresgrund laeuft. Krebsgang: Vor/Seit-Eingabe vertauscht, Richtung
 * wechselt alle 20-99 Ticks. Varianten TROPICAL, 1/8 CLAYINFUSED (Textur). Droppt Ton ({@code getDropItem}, Vanilla-
 * {@code dropFewItems}). Werte wie {@code EntityWaterMob}: 20 Leben, Groesse 0.6x1.8 (keine eigene).
 */
public class EntityScuttlecrab extends WaterAnimal implements IEntityEnumMulti {

    public enum Scuttlecrab {
        TROPICAL,
        CLAYINFUSED,
    }

    public Scuttlecrab type;

    // Walk on the seafloor, to do so, we override the isInWater check when attempting movement
    private boolean seafloorWalking = false;

    // Crab walk!
    float crabDirection = 1;
    int switchTimer = 0;

    public EntityScuttlecrab(EntityType<? extends EntityScuttlecrab> entityType, Level world) {
        super(entityType, world);

        type = Scuttlecrab.TROPICAL;
        if (world.random.nextInt(8) == 0) type = Scuttlecrab.CLAYINFUSED;
    }

    /** {@code EntityWaterMob} setzt keine Attribute: 1.7.10-Vorgabe (20 Leben, Folgeweite 16). */
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes();
    }

    @Override
    public Enum<?> getEnum() {
        return type;
    }

    @Override
    public boolean isInWater() {
        if (seafloorWalking) return false;
        return super.isInWater();
    }

    @Override
    public void aiStep() {
        seafloorWalking = true;
        super.aiStep();
        seafloorWalking = false;
    }

    @Override
    public void travel(Vec3 input) {
        switchTimer--;
        if (switchTimer <= 0) {
            crabDirection *= -1;
            switchTimer = 20 + level().random.nextInt(80);
        }

        // super.moveEntityWithHeading(forwards * crabDirection, strafe): (strafe, forward) vertauscht
        super.travel(new Vec3(input.z * crabDirection, input.y, input.x));
    }

    //? if < 1.21.1 {
    @Override
    protected void dropCustomDeathLoot(DamageSource source, int lootingLevel, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, lootingLevel, recentlyHit);
    //?} else {
    /*@Override
    protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel serverLevel, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(serverLevel, source, recentlyHit);
    *///?}
        com.hbm_m.entity.mob.CreeperDrops.dropDefault(this, Items.CLAY_BALL, MobDrops.looting(this, source));
    }
}
