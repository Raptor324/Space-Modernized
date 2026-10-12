package com.space_m.entity.mob;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code EntityDepthSquid} (Sagan-See): Vanilla-Tintenfisch in sieben Farbvarianten mit eigenem Modell; die
 * Beinschwung-Animation wird nach der Vanilla-Bewegung neu berechnet ("mojang squid implementation just blasts
 * through everything"). Drops wie {@code EntitySquid.dropFewItems}: {@code rand(3 + looting) + 1} Tintenbeutel.
 */
public class EntityDepthSquid extends Squid implements IEntityEnumMulti {

    public enum DepthSquid {
        AQUA,
        BLACK,
        ORANGE,
        OURPLE,
        RED,
        SILVER,
        VICIOUS,
    }

    public DepthSquid type;

    public EntityDepthSquid(EntityType<? extends EntityDepthSquid> entityType, Level world) {
        super(entityType, world);
        type = DepthSquid.values()[world.random.nextInt(DepthSquid.values().length)];
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Squid.createAttributes();
    }

    @Override
    public Enum<?> getEnum() {
        return type;
    }

    @Override
    public void travel(Vec3 input) {
        super.travel(input);
        // limbSwing: f6 = min(sqrt(dx*dx + dz*dz) * 4, 1), limbSwingAmount += (f6 - amount) * 0.4
        this.calculateEntityAnimation(false);
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
        int j = random.nextInt(3 + MobDrops.looting(this, source)) + 1;
        for (int k = 0; k < j; ++k) {
            this.spawnAtLocation(new ItemStack(Items.INK_SAC), 0.0F);
        }
    }

    /** {@code EntitySquid.getCanSpawnHere}: {@code posY > 45 && posY < 63} (und keine Kollision). */
    public static boolean checkDepthSquidSpawnRules(EntityType<? extends net.minecraft.world.entity.Mob> type, net.minecraft.world.level.ServerLevelAccessor level,
                                                    net.minecraft.world.entity.MobSpawnType reason, net.minecraft.core.BlockPos pos,
                                                    net.minecraft.util.RandomSource random) {
        return pos.getY() > 45 && pos.getY() < 63 && EntityFish.checkFishSpawnRules(type, level, reason, pos, random);
    }
}
