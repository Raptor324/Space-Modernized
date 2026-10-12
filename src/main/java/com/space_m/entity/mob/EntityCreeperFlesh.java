package com.space_m.entity.mob;

import com.hbm_m.entity.mob.CreeperDrops;
import com.hbm_m.explosion.vanillant.ExplosionVNT;
import com.hbm_m.explosion.vanillant.standard.BlockAllocatorBulkie;
import com.hbm_m.explosion.vanillant.standard.BlockMutatorBulkie;
import com.hbm_m.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm_m.explosion.vanillant.standard.EntityProcessorStandard;
import com.hbm_m.explosion.vanillant.standard.ExplosionEffectStandard;
import com.hbm_m.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm_m.particle.helper.IParticleCreator;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code EntityCreeperFlesh} (spawnt auf Laythe nur auf {@code tumor}): statt der Vanilla-Explosion Fleischfetzen
 * ({@code giblets}, cDiv 2) und eine VNT-Explosion der Staerke 4 (geladen 7, Bulkie-Zuteiler 30 / Strahlaufloesung 8 bzw. 16),
 * die die getroffenen Bloecke in Fleisch ({@code tumor}) verwandelt; Wesenschaden mit Reichweitenfaktor 0.25. Der Vanilla-{@code explodeCreeper} ist privat -
 * Abfang ueber {@code com.space_m.mixin.CreeperFleshMixin}. Drops wie {@code EntityCreeper}: Schwarzpulver + Platte.
 */
public class EntityCreeperFlesh extends Creeper {

    public EntityCreeperFlesh(EntityType<? extends EntityCreeperFlesh> entityType, Level world) {
        super(entityType, world);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Creeper.createAttributes();
    }

    /** {@code func_146077_cc} (explode). Aus dem Mixin, nur Server. */
    public void fleshExplode() {
        if (this.level().isClientSide) return;

        CompoundTag vdat = new CompoundTag();
        vdat.putString("type", "giblets");
        vdat.putInt("ent", this.getId());
        vdat.putInt("cDiv", 2);
        IParticleCreator.sendPacket((ServerLevel) level(), getX(), getY() + getBbHeight() * 0.5, getZ(), 150, vdat);

        this.dead = true;
        this.discard();

        ExplosionVNT vnt = new ExplosionVNT(level(), getX(), getY(), getZ(), this.isPowered() ? 7 : 4, this);
        vnt.setBlockAllocator(new BlockAllocatorBulkie(30, this.isPowered() ? 16 : 8));
        vnt.setBlockProcessor(new BlockProcessorStandard().withBlockEffect(new BlockMutatorBulkie(SpaceMobs.TUMOR.get())));
        vnt.setEntityProcessor(new EntityProcessorStandard().withRangeMod(0.25F));
        vnt.setPlayerProcessor(new PlayerProcessorStandard());
        vnt.setSFX(new ExplosionEffectStandard());
        vnt.explode();
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
        CreeperDrops.dropDefault(this, Items.GUNPOWDER, MobDrops.looting(this, source));
        CreeperDrops.dropRecord(this, source);
    }
}
