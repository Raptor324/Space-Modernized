package com.space_m.entity.mob.ai;

import java.util.List;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code EntityAISwimAway} (NTM Space, im Original von keinem Mob benutzt): flieht vor Spielern im Umkreis
 * 5/3/5 zu einem Zufallsziel, solange ein Spieler naeher als 10 Bloecke ist.
 */
public class EntityAISwimAway extends Goal {

    private final PathfinderMob fish;
    private final double speed;
    private Player closestPlayer;
    private double randPosY;
    private double randPosX;
    private double randPosZ;

    public EntityAISwimAway(PathfinderMob fish, double speed) {
        this.fish = fish;
        this.speed = speed;
    }

    @Override
    public boolean canUse() {
        List<Player> players = fish.level().getEntitiesOfClass(Player.class, fish.getBoundingBox().inflate(5.0D, 3.0D, 5.0D));
        if (!players.isEmpty()) {
            closestPlayer = players.get(0);
            Vec3 vec3 = DefaultRandomPos.getPos(this.fish, 5, 4);
            if (vec3 == null) return false;
            this.randPosX = vec3.x;
            this.randPosY = vec3.y;
            this.randPosZ = vec3.z;
            return true;
        }

        return false;
    }

    @Override
    public void start() {
        this.fish.getNavigation().moveTo(this.randPosX, this.randPosY, this.randPosZ, this.speed);
    }

    @Override
    public boolean canContinueToUse() {
        // Ensure that the fish continues to flee as long as the player is close
        return fish.distanceTo(closestPlayer) < 10.0F;
    }
}
