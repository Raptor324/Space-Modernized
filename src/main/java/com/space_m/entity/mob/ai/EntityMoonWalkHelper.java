package com.space_m.entity.mob.ai;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;

/**
 * 1:1 {@code EntityMoonWalkHelper}: Bewegungssteuerung der Mun-Kuh - schaut vom Ziel weg (+90 statt -90 Grad) und
 * laeuft rueckwaerts darauf zu (Vorwaertseingabe {@code -speed}), also Moonwalk.
 */
public class EntityMoonWalkHelper extends MoveControl {

    public EntityMoonWalkHelper(Mob entity) {
        super(entity);
    }

    @Override
    public void tick() {
        this.mob.setZza(0.0F);

        if (this.operation == Operation.MOVE_TO) {
            this.operation = Operation.WAIT;
            int i = Mth.floor(this.mob.getBoundingBox().minY + 0.5D);
            double d0 = this.wantedX - this.mob.getX();
            double d1 = this.wantedZ - this.mob.getZ();
            double d2 = this.wantedY - (double) i;
            double d3 = d0 * d0 + d2 * d2 + d1 * d1;

            if (d3 >= 2.500000277905201E-7D) {
                float f = (float) (Math.atan2(d1, d0) * 180.0D / Math.PI) + 90.0F;
                this.mob.setYRot(this.rotlerp(this.mob.getYRot(), f, 30.0F));
                float speed = (float) (this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED));
                this.mob.setSpeed(speed);
                this.mob.setZza(-speed);

                if (d2 > 0.0D && d0 * d0 + d1 * d1 < 1.0D) {
                    this.mob.getJumpControl().jump();
                }
            }
        }
    }
}
