package com.space_m.entity.mob.ai;

import java.util.EnumSet;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code EntityAISwimmingVTwo} (NTM Space, im Original von keinem Mob benutzt): 1/120 je Pruefung ein
 * Zufallsziel im Umkreis 10/7 anschwimmen (Sperrbit 1 = Bewegung).
 */
public class EntityAISwimmingVTwo extends Goal {

    private final PathfinderMob fish;
    private double xPosition;
    private double yPosition;
    private double zPosition;
    private final double speed;

    public EntityAISwimmingVTwo(PathfinderMob fish, double speed) {
        this.fish = fish;
        this.speed = speed;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (this.fish.getRandom().nextInt(120) != 0) {
            return false;
        }

        Vec3 randomTarget = DefaultRandomPos.getPos(this.fish, 10, 7);
        if (randomTarget == null) {
            return false;
        } else {
            this.xPosition = randomTarget.x;
            this.yPosition = randomTarget.y;
            this.zPosition = randomTarget.z;
            return true;
        }
    }

    @Override
    public boolean canContinueToUse() {
        return !this.fish.getNavigation().isDone();
    }

    @Override
    public void start() {
        this.fish.getNavigation().moveTo(this.xPosition, this.yPosition, this.zPosition, this.speed);
    }
}
