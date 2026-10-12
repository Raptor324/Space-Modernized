package com.space_m.war;

import com.hbm_m.entity.projectile.EntityBombletZeta;
import com.hbm_m.explosion.vanillant.ExplosionVNT;
import com.hbm_m.explosion.vanillant.standard.EntityProcessorCrossSmooth;
import com.hbm_m.explosion.vanillant.standard.ExplosionEffectWeapon;
import com.hbm_m.explosion.vanillant.standard.PlayerProcessorStandard;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Bomblet des Carpet-Jets: NTM Space {@code EntityBombletZeta} mit {@code type == 9} - beim Aufschlag eine
 * VNT-Explosion (Staerke 4, an +0,5/+1,5/+0,5) mit {@code EntityProcessorCrossSmooth(1, 100)},
 * {@code PlayerProcessorStandard} und {@code ExplosionEffectWeapon(15, 3.5, 1.25)}. Flugbahn wie der HBM-Port
 * (0,99 Luftwiderstand, -0,05 Schwerkraft). Darstellung = HBM-Bomblet-Renderer.
 */
public class EntityCarpetBomblet extends EntityBombletZeta {

    public EntityCarpetBomblet(EntityType<? extends EntityCarpetBomblet> type, Level level) {
        super(type, level);
    }

    @Override
    public void tick() {
        this.xOld = this.xo = this.getX();
        this.yOld = this.yo = this.getY();
        this.zOld = this.zo = this.getZ();

        Vec3 motion = this.getDeltaMovement();
        this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);

        this.setDeltaMovement(motion.x * 0.99D, motion.y - 0.05D, motion.z * 0.99D);
        updateRotation();

        BlockPos pos = BlockPos.containing(this.getX(), this.getY(), this.getZ());
        if (!this.level().getBlockState(pos).isAir() || this.getY() < this.level().getMinBuildHeight()) {
            if (!this.level().isClientSide) {
                if (getBombType() == 9) {
                    ExplosionVNT vnt = new ExplosionVNT(level(), this.getX() + 0.5F, this.getY() + 1.5F, this.getZ() + 0.5F, 4F);
                    vnt.setEntityProcessor(new EntityProcessorCrossSmooth(1, 100));
                    vnt.setPlayerProcessor(new PlayerProcessorStandard());
                    vnt.setSFX(new ExplosionEffectWeapon(15, 3.5F, 1.25F));
                    vnt.explode();
                }
                this.discard();
            }
        }
    }
}
