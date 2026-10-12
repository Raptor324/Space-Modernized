package com.space_m.entity.mob;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code EntityFish} (NTM Space): Basis der Laythe-Fische (Scutterfish, Scrapfish, Sifter-Aal). Schwimmt einem
 * Zufallsziel (alle ~200 Ticks neu, ausserhalb des Wassers jeden Tick) mit 0.01 Beschleunigung plus Zitterbewegung
 * nach, dreht sich mit hoechstens 10 Grad je Tick in die Bewegungsrichtung und zappelt an Land. Im Wasser nur
 * {@code motionY *= 0.8} (keine waagerechte Reibung, wie im Original). KI: Spieler ansehen, umsehen, umherwandern.
 * Werte: 15 Leben, Folgeweite 10, Groesse 2x2 (Registrierung).
 */
public abstract class EntityFish extends WaterAnimal {

    private Vec3 currentSwimTarget;
    private final double swimSpeed;
    private final float watchDistance;

    protected EntityFish(EntityType<? extends EntityFish> type, Level world, double swimSpeed, float watchDistance) {
        super(type, world);
        this.swimSpeed = swimSpeed;
        this.watchDistance = watchDistance;
        // getNavigator().setCanSwim(true)
        if (this.getNavigation() instanceof GroundPathNavigation nav) nav.setCanFloat(true);
        this.currentSwimTarget = new Vec3(getX(), getY(), getZ());
        this.registerFishGoals();
    }

    @Override
    protected net.minecraft.world.entity.ai.navigation.PathNavigation createNavigation(Level level) {
        return new GroundPathNavigation(this, level);
    }

    @Override
    protected void registerGoals() {
        // Felder werden erst nach super() gesetzt -> Ziele in registerFishGoals (Ende des Konstruktors)
    }

    /** {@code tasks.addTask} 0/1/2 des Original-Konstruktors. */
    protected void registerFishGoals() {
        this.goalSelector.addGoal(0, new LookAtPlayerGoal(this, Player.class, watchDistance));
        this.goalSelector.addGoal(1, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, swimSpeed));
    }

    public static AttributeSupplier.Builder createFishAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 15.0D).add(Attributes.FOLLOW_RANGE, 10.0D);
    }

    /** {@code updateAITasks}: an Land nach oben kippen, springen und zufaellig zappeln. */
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();

        if (!this.isInWater()) {
            this.setXRot(this.getXRot() + 0.2F);
            this.setYRot(0.0F);
            this.setJumping(true);
            if (this.onGround()) {
                this.push(0.4 * random.nextDouble() - 0.4 * random.nextDouble(), 0.0, 0.4 * random.nextDouble() - 0.4 * random.nextDouble());
            }
        }
    }

    /** {@code onLivingUpdate}: nach dem Vanilla-Teil Ziel waehlen, beschleunigen, ausrichten. */
    @Override
    public void aiStep() {
        super.aiStep();
        this.updateSwimTarget();
        this.moveTowardsTarget();
        this.updateRotation();
    }

    private void updateSwimTarget() {
        if (random.nextInt(200) == 0 || !this.isInWater()) {
            double targetX = this.getX() + (random.nextDouble() - 0.5) * 20.0;
            // 1.7.10: clamp(0, worldObj.getHeight() - 1)
            double targetY = Mth.clamp(this.getY() + (random.nextDouble() - 0.5) * 8.0, level().getMinBuildHeight(), level().getMaxBuildHeight() - 1);
            double targetZ = this.getZ() + (random.nextDouble() - 0.5) * 20.0;
            this.currentSwimTarget = new Vec3(targetX, targetY, targetZ);
        }
    }

    private void moveTowardsTarget() {
        double deltaX = this.currentSwimTarget.x - this.getX();
        double deltaY = this.currentSwimTarget.y - this.getY();
        double deltaZ = this.currentSwimTarget.z - this.getZ();
        double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);

        double dirX = deltaX / distance;
        double dirY = deltaY / distance;
        double dirZ = deltaZ / distance;

        double speed = 0.01;
        Vec3 m = this.getDeltaMovement();
        this.setDeltaMovement(
                m.x + dirX * speed + (random.nextDouble() - 0.5) * 0.02,
                m.y + dirY * speed + (random.nextDouble() - 0.5) * 0.02,
                m.z + dirZ * speed + (random.nextDouble() - 0.5) * 0.02);
        // Original: updateRotation((float) dirX, (float) dirY, (float) dirZ) -> Vanilla-updateRotation(cur, target, max),
        // dessen Ergebnis verworfen wird (keine Wirkung)
    }

    /** {@code moveEntityWithHeading}: im Wasser nur vertikal gedaempft, sonst Vanilla. */
    @Override
    public void travel(Vec3 input) {
        if (this.isInWater()) {
            Vec3 m = this.getDeltaMovement();
            this.setDeltaMovement(m.x, m.y * 0.8F, m.z);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.calculateEntityAnimation(false);
        } else {
            super.travel(input);
        }
    }

    private void updateRotation() {
        Vec3 m = this.getDeltaMovement();
        double deltaX = m.x;
        double deltaZ = m.z;
        double deltaY = m.y;
        float targetYaw = (float) (Math.atan2(deltaZ, deltaX) * (180D / Math.PI)) - 90F;
        float targetPitch = (float) -(Math.atan2(deltaY, Math.sqrt(deltaX * deltaX + deltaZ * deltaZ)) * (180D / Math.PI));

        this.setYRot(this.updateRotation(this.getYRot(), targetYaw, 10.0F));
        this.setXRot(this.updateRotation(this.getXRot(), targetPitch, 10.0F));
    }

    private float updateRotation(float currentRotation, float targetRotation, float maxIncrement) {
        float deltaRotation = Mth.wrapDegrees(targetRotation - currentRotation);
        if (deltaRotation > maxIncrement) {
            deltaRotation = maxIncrement;
        }
        if (deltaRotation < -maxIncrement) {
            deltaRotation = -maxIncrement;
        }
        return currentRotation + deltaRotation;
    }

    /** {@code EntityWaterMob.getCanSpawnHere}: nur ohne Kollision mit anderen Entities (Platzierung IN_WATER). */
    public static boolean checkFishSpawnRules(EntityType<? extends Mob> type, net.minecraft.world.level.ServerLevelAccessor level,
                                              net.minecraft.world.entity.MobSpawnType reason, net.minecraft.core.BlockPos pos,
                                              net.minecraft.util.RandomSource random) {
        return level.getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER);
    }
}
